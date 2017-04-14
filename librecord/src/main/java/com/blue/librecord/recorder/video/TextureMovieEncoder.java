/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blue.librecord.recorder.video;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.blue.librecord.recorder.gles.EglCore;
import com.blue.librecord.recorder.gles.GlUtil;
import com.blue.librecord.recorder.gles.WindowSurface;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.filter.FilterWrapper;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

/**
 * Encode a movie from frames rendered from an external texture image.
 * <p>
 * The object wraps an encoder running on a dedicated thread.  The various control messages
 * may be sent from arbitrary threads (typically the app UI thread).  The encoder thread
 * manages both sides of the encoder (feeding and draining); the only external input is
 * the GL texture.
 * <p>
 * The design is complicated slightly by the need to create an EGL context that shares state
 * with a view that gets restarted if (say) the device orientation changes.  When the view
 * in question is a GLSurfaceView, we don't have full control over the EGL context creation
 * on that side, so we have to bend a bit backwards here.
 * <p>
 * To use:
 * <ul>
 * <li>create TextureMovieEncoder object
 * <li>create an EncoderConfig
 * <li>call TextureMovieEncoder#startRecording() with the config
 * <li>call TextureMovieEncoder#setTextureId() with the texture object that receives frames
 * <li>for each frame, after latching it with SurfaceTexture#updateTexImage(),
 * call TextureMovieEncoder#frameAvailable().
 * </ul>
 * <p>
 * TODO: tweak the API (esp. textureId) so it's less awkward for simple use cases.
 */
@SuppressLint("NewApi")
public class TextureMovieEncoder implements Runnable {
    private static final String TAG = TextureMovieEncoder.class.getSimpleName();
    private static final boolean VERBOSE = false;

    private static final int MSG_START_RECORDING = 0;
    private static final int MSG_STOP_RECORDING = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;
    private static final int MSG_SET_FILTER = 6;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    //    private FullFrameRect mFullScreen;
    private long mStartRecordTimeNanos = 0; // 开始录制的时间
    private long mRecordTotalTimeNanos = 0; // 已录制的总时间
    private FilterWrapper mFilterWrapper = createFilter();

    private FilterWrapper createFilter() {

        GPUImageFilter filter = this.filter;
        if (null == filter) {
            filter = new GPUImageFilter();
        }
        FilterWrapper f = new FilterWrapper(filter);
        f.initFilter();

        if (null != mConfig) {
            // f.onOutpuSizeChanged(mConfig.mPreviewWidth,mConfig.mPreviewHeight);
            f.onOutpuSizeChanged(mConfig.mWidth, mConfig.mHeight);
            f.onSurfaceSizeChanged(mConfig.mWidth, mConfig.mHeight);

            switch (mConfig.mCameraOrientation) {
                case 90:
                    f.setRotation(Rotation.ROTATION_90, mConfig.mFlipHorizontal, mConfig.mFlipVertical);
                    break;
                case 180:
                    f.setRotation(Rotation.ROTATION_180, mConfig.mFlipHorizontal, mConfig.mFlipVertical);
                    break;
                case 270:
                    f.setRotation(Rotation.ROTATION_270, mConfig.mFlipHorizontal, mConfig.mFlipVertical);
                    break;
                default:
                    break;
            }
        }

        return f;
    }

    private int mTextureId;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;

    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;
    private long mTimeStamp;
    private List<Long> mPauseTimeStamp = new ArrayList<Long>();
    private List<Long> mResumeTimeStamp = new ArrayList<Long>();
    private boolean mPause;


    /**
     * Encoder configuration.
     * <p>
     * Object is immutable, which means we can safely pass it between threads without
     * explicit synchronization (and don't need to worry about it getting tweaked out from
     * under us).
     * <p>
     * TODO: make frame rate and iframe interval configurable?  Maybe use builder pattern
     * with reasonable defaults for those and bit rate.
     */
    public static class EncoderConfig {
        final File mOutputFile;
        final int mWidth;
        final int mHeight;

        final int mPreviewWidth;
        final int mPreviewHeight;


        final int mCameraOrientation;// 摄像头旋转度数
        final boolean mFlipHorizontal;
        final boolean mFlipVertical;

        final int mBitRate;
        final EGLContext mEglContext;

        public EncoderConfig(File outputFile, int width, int height, int bitRate, int previewWidth, int previewHeight, int cameraOrientation, boolean flipHorizontal, boolean flipVertical,
                             EGLContext sharedEglContext) {
            mOutputFile = outputFile;
            mWidth = width;
            mHeight = height;
            this.mCameraOrientation = cameraOrientation;
            this.mFlipHorizontal = flipHorizontal;
            this.mFlipVertical = flipVertical;

            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;

            mBitRate = bitRate;
            mEglContext = sharedEglContext;
        }

        @Override
        public String toString() {
            return "EncoderConfig: " + mWidth + "x" + mHeight + " @" + mBitRate +
                    " to '" + mOutputFile.toString() + "' ctxt=" + mEglContext;
        }
    }

    /**
     * Tells the video recorder to start recording.  (Call from non-encoder thread.)
     * <p>
     * Creates a new thread, which will create an encoder using the provided configuration.
     * <p>
     * Returns after the recorder thread has started and is ready to accept Messages.  The
     * encoder may not yet be fully configured.
     */
    public void startRecording(EncoderConfig config) {
        Log.d(TAG, "Encoder: startRecording()");
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
                return;
            }
            mRunning = true;
            new Thread(this, "TextureMovieEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MSG_START_RECORDING, config));
    }

    /**
     * 编码停下后回调
     *
     * @author sulei
     */
    public interface OnStopOverListener {
        void onStopOver();
    }

    /**
     * Tells the video recorder to stop recording.  (Call from non-encoder thread.)
     * <p>
     * Returns immediately; the encoder/muxer may not yet be finished creating the movie.
     * <p>
     * TODO: have the encoder thread invoke a callback on the UI thread just before it shuts down
     * so we can provide reasonable status UI (and let the caller know that movie encoding
     * has completed).
     */
    public void stopRecording(OnStopOverListener onStopOverListener) {
        mPauseTimeStamp.clear();
        mResumeTimeStamp.clear();
        mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_RECORDING));
        Message m = mHandler.obtainMessage(MSG_QUIT);
        m.obj = onStopOverListener;
        mHandler.sendMessage(m);
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
        mStartRecordTimeNanos = 0; // 重置开始录制时间
        mRecordTotalTimeNanos = 0;
    }

    /**
     * Returns true if recording has been started.
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRunning;
        }
    }

    /**
     * 获取已录制的时间,单位：纳秒
     *
     * @return
     */
    public long getRecordedTimeNanos() {
        return this.mRecordTotalTimeNanos;
    }

    private GPUImageFilter filter;

    /**
     * 设置滤镜
     *
     * @param filter
     */
    public void setFilter(GPUImageFilter filter) {
        if (null != filter) {
            this.filter = filter.clone();
        }

        if (null != mHandler) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_FILTER, this.filter));
        }
    }

    /**
     * Tells the video recorder to refresh its EGL surface.  (Call from non-encoder thread.)
     */
    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void pause() {
        if (!mPause) {
            mPauseTimeStamp.add(mTimeStamp);
            mPause = true;
        }
    }

    private float[] transform;

    /**
     * Tells the video recorder that a new frame is available.  (Call from non-encoder thread.)
     * <p>
     * This function sends a message and returns immediately.  This isn't sufficient -- we
     * don't want the caller to latch a new frame until we're done with this one -- but we
     * can get away with it so long as the input frame rate is reasonable and the encoder
     * thread doesn't stall.
     * <p>
     * TODO: either block here until the texture has been rendered onto the encoder surface,
     * or have a separate "block if still busy" method that the caller can execute immediately
     * before it calls updateTexImage().  The latter is preferred because we don't want to
     * stall the caller while this thread does work.
     */
    public void frameAvailable(SurfaceTexture st) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }

        if (null == transform) {
            transform = new float[16];
        }

        st.getTransformMatrix(transform);
        //TODO Chris timestamp is here!
        long timestamp = st.getTimestamp();
        if (timestamp == 0) {
            return;
        }

        if (mPause && !mPauseTimeStamp.isEmpty()) {
            mPause = false;
            mResumeTimeStamp.add(timestamp);
            //hack: 50 is to be rethink
            timestamp = mPauseTimeStamp.get(mPauseTimeStamp.size() - 1) + 50;
        } else if (!mPause && !mResumeTimeStamp.isEmpty()) {
            timestamp = mPauseTimeStamp.get(mPauseTimeStamp.size() - 1) +
                    timestamp - mResumeTimeStamp.get(mResumeTimeStamp.size() - 1);
        }
        ;
        if (timestamp == 0) {
            // Seeing this after device is toggled off/on with power button.  The
            // first frame back has a zero timestamp.
            //
            // MPEG4Writer thinks this is cause to abort() in native code, so it's very
            // important that we just ignore the frame.
            Log.w(TAG, "HEY: got SurfaceTexture with timestamp of zero");
            return;
        }

        mTimeStamp = timestamp;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE,
                (int) (timestamp >> 32), (int) timestamp, transform));
    }

    /**
     * Tells the video recorder what texture name to use.  This is the external texture that
     * we're receiving camera previews in.  (Call from non-encoder thread.)
     * <p>
     * TODO: do something less clumsy
     */
    public void setTextureId(int id) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_TEXTURE_ID, id, 0, null));
    }

    /**
     * Encoder thread entry point.  Establishes Looper/Handler and waits for messages.
     * <p>
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<TextureMovieEncoder> mWeakEncoder;

        public EncoderHandler(TextureMovieEncoder encoder) {
            mWeakEncoder = new WeakReference<TextureMovieEncoder>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            TextureMovieEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_START_RECORDING:
                    encoder.handleStartRecording((EncoderConfig) obj);
                    break;
                case MSG_STOP_RECORDING:
                    encoder.handleStopRecording();
                    break;
                case MSG_FRAME_AVAILABLE:
                    long timestamp = (((long) inputMessage.arg1) << 32) |
                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((float[]) obj, timestamp);
                    break;
                case MSG_SET_TEXTURE_ID:
                    encoder.handleSetTexture(inputMessage.arg1);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();

                    // 通知停止编码结束
                    if (null != obj) {
                        ((OnStopOverListener) obj).onStopOver();
                    }

                    break;
                case MSG_SET_FILTER:
                    encoder.handleFilter((GPUImageFilter) obj);
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    /**
     * Starts recording.
     */
    private void handleStartRecording(EncoderConfig config) {
        Log.d(TAG, "handleStartRecording " + config);
        mFrameNum = 0;
//        prepareEncoder(config.mEglContext, config.mWidth, config.mHeight, config.mBitRate,config.mPreviewWidth, config.mPreviewHeight,config.cameraOrientation,
//                config.mOutputFile);
        prepareEncoder(config);

    }

    // 调试功能，是否开启绘制测试矩形
    private boolean isShowTestBox = false;

    /**
     * Handles notification of an available frame.
     * <p>
     * The texture is rendered onto the encoder's input surface, along with a moving
     * box (just because we can).
     * <p>
     *
     * @param transform      The texture transform, from SurfaceTexture.
     * @param timestampNanos The frame's timestamp, from SurfaceTexture.
     */
    private void handleFrameAvailable(float[] transform, long timestampNanos) {
        if (VERBOSE) Log.d(TAG, "handleFrameAvailable tr=" + transform);
        mVideoEncoder.drainEncoder(false);
//        mFullScreen.drawFrame(mTextureId, transform);

//        GLES20.glUseProgram(mProgramHandle); // 小米手机上不这么设置一下,录制出来的图像卡爆了
//        GLES20.glUseProgram(0);
        mFilterWrapper.drawFrame(mTextureId, mPreviewWidth, mPreviewHeight);

        if (isShowTestBox) {
            drawBox(mFrameNum++);
        }

        mInputWindowSurface.setPresentationTime(timestampNanos);
        mInputWindowSurface.swapBuffers();

        if (mStartRecordTimeNanos == 0) {
            mStartRecordTimeNanos = timestampNanos;
        }
        mRecordTotalTimeNanos = timestampNanos - mStartRecordTimeNanos;
    }


    /**
     * Handles a request to stop encoding.
     */
    private void handleStopRecording() {
        Log.d(TAG, "handleStopRecording");
        mVideoEncoder.drainEncoder(true);
        releaseEncoder();
    }

    /**
     * Sets the texture name that SurfaceTexture will use when frames are received.
     */
    private void handleSetTexture(int id) {
        mTextureId = id;
    }

    /**
     * Tears down the EGL surface and context we've been using to feed the MediaCodec input
     * surface, and replaces it with a new one that shares with the new context.
     * <p>
     * This is useful if the old context we were sharing with went away (maybe a GLSurfaceView
     * that got torn down) and we need to hook up with the new one.
     */
    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
//        mFullScreen.release(false);
        mFilterWrapper.destory();

        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
//        mFullScreen = new FullFrameRect(
//                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mFilterWrapper = createFilter();

//        Texture2dProgram.ProgramType programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
//        float[] kernel = null;
//        float colorAdj = 0.0f;
//        mFullScreen.changeProgram(new Texture2dProgram(programType));
//
//        // Update the filter kernel (if any).
//        if (kernel != null) {
//            mFullScreen.getProgram().setKernel(kernel, colorAdj);
//        }
    }

    /**
     * 处理滤镜
     *
     * @param filter
     */
    private void handleFilter(GPUImageFilter filter) {
        if (null != mFilterWrapper) {
            mFilterWrapper.setFilter(filter);
            if (null != mConfig) {
                mFilterWrapper.onOutpuSizeChanged(mConfig.mWidth, mConfig.mHeight);
                mFilterWrapper.onSurfaceSizeChanged(mConfig.mWidth, mConfig.mHeight);
            }

        }
    }

    private int mPreviewWidth;
    private int mPreviewHeight;


    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";
    private int mProgramHandle;

    private EncoderConfig mConfig;

    private void prepareEncoder(EncoderConfig config) {
        mConfig = config;
        mPreviewWidth = config.mPreviewWidth;
        mPreviewHeight = config.mPreviewHeight;


        try {
            mVideoEncoder = new VideoEncoderCore(config.mWidth, config.mHeight, config.mBitRate, config.mOutputFile);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mEglCore = new EglCore(config.mEglContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface(), true);
        mInputWindowSurface.makeCurrent();

//        mFullScreen = new FullFrameRect(
//                new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));

        mProgramHandle = GlUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);

        mFilterWrapper = createFilter();
    }


    private void releaseEncoder() {
        mVideoEncoder.release();
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
//        if (mFullScreen != null) {
//            mFullScreen.release(false);
//            mFullScreen = null;
//        }


        if (null != mFilterWrapper) {
            mFilterWrapper.destory();
            mFilterWrapper = null;
        }

        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    /**
     * Draws a box, with position offset.
     */
    private void drawBox(int posn) {
        final int width = mInputWindowSurface.getWidth();
        int xpos = (posn * 4) % (width - 50);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(xpos, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }
}
