package demo.recorder.media;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.opengles.GL10;

import demo.recorder.util.CameraUtils;
import demo.recorder.TextureMovieEncoder;
import demo.recorder.ui.CameraPreview;

import static demo.recorder.media.MediaRecordService.RECORDING_OFF;
import static demo.recorder.media.MediaRecordService.RECORDING_ON;
import static demo.recorder.media.MediaRecordService.RECORDING_RESUMED;

/** 
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
*/
public class VideoRecordCore implements TexureObserver,SurfaceTexture.OnFrameAvailableListener{

    private Camera mCamera;
    private static final String TAG = "VideoRecordCore";

    private int mCameraPreviewWidth, mCameraPreviewHeight;

    private TextureMovieEncoder mVideoEncoder;
    private CameraPreview mCameraPreview;
    private File mOutputFile;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private int mRecordQualityType;
    private int mRecordWidth;
    private int mRecordHeight;
    private boolean isShowTestBox;
    private boolean mPauseEnabled;
    protected OnRecordStatusChangedListener mOnRecordStatusChangedListener;

    /**
     * Video quality
     */
    public static final int QUALITY_HIGH = 0;
    public static final int QUALITY_NORMAL_HIGH = 1;
    public static final int QUALITY_NORMAL = 2;
    public static final int QUALITY_NORMAL_LOW = 3;
    public static final int QUALITY_LOW = 4;
    private int mFrameCount;
    private boolean mIsNotifiedPause;
    private long mCurTimeCalcRecordTime;
    private long mLastTimeCalcRecordTime;
    /**
     * 通知录制进度间隔时间，单位：毫秒
     */
    protected int mIntervalNotifyRecordProcessing = 1000;

    /**
     * 显示的旋转角度
     */
    private int mDisplayOrientation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    public VideoRecordCore() {
        mVideoEncoder = new TextureMovieEncoder();
    }


    public void bindPreview(CameraPreview cameraPreview){
        mCameraPreview = cameraPreview;
        mCameraPreview.setObServer(this);
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    public void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    protected void openCamera(int desiredWidth, int desiredHeight) {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        int id = 0;
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i);
                id = i;
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        /**
         * init camera params
         */
        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        CameraUtils.setCameraDisplayOrientation((Activity)mCameraPreview.getContext(),id,mCamera);
        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        // leave the frame rate set to default
        mCamera.setParameters(parms);
        // setCameraDisplayOrientation(this,id,mCamera);
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;
        mCameraPreview.queueEvent(new Runnable() {
            @Override public void run() {
                mCameraPreview.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
            }
        });

    }



    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
        mCameraPreview.requestRender();
    }


    public void onPause() {
        releaseCamera();
    }

    private Handler mCameraHandler;
    @Override
    public void onSurfaceCreated(SurfaceTexture aSurfaceTexture) {
        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                VideoRecordCore.CameraHandler.MSG_SET_SURFACE_TEXTURE, aSurfaceTexture));
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        }else {
            mRecordingStatus = RECORDING_OFF;
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }


    @Override
    public void onDrawFrame(SurfaceTexture aSurfaceTexture, int aTexureID) {
        handleRecordEvent(aSurfaceTexture,aTexureID);
    }

    public void changeRecordingState(final boolean isRecording) {
        if (null == mOutputFile) {
            throw new IllegalStateException();
        }
        if (mRecordingEnabled == isRecording) return;
        mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "SurfaceRenderer changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
                mRecordingEnabled = isRecording;
            }
        });
    }

    public void changePauseState(final boolean isPause) {
        if (mPauseEnabled == isPause) return;
        mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                mPauseEnabled = isPause;
            }
        });
    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    public  static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<VideoRecordCore> mVideoRecordCore;

        public CameraHandler(VideoRecordCore aVideoRecordCore) {
            mVideoRecordCore = new WeakReference<VideoRecordCore>(aVideoRecordCore);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mVideoRecordCore.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            VideoRecordCore sVideoRecordCore = mVideoRecordCore.get();
            if (sVideoRecordCore == null) {
                Log.w(TAG, "CameraHandler.handleMessage: sVideoRecordCore is null");
                return;
            }

            switch (what) {
                case MSG_SET_SURFACE_TEXTURE:
                    sVideoRecordCore.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    /**
     * description: handle the record event
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    public void handleRecordEvent(SurfaceTexture aSurfaceTexture,int aTextureID) {

        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    startRecording(aTextureID);
                    break;
                case RECORDING_RESUMED:
                    resumeRecording();
                    break;
                case RECORDING_ON:
                    break;
                default:
                    throw new RuntimeException(TAG+"unknow mRecordingStatus: " + mRecordingStatus);
            }
            pauseOnRecorder(aSurfaceTexture);
        }else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    Log.d(TAG, " stop recording");
                    stopRecordTodo();
                    // 刷新录制状态
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException(TAG+"unknow mRecordingStatus: " + mRecordingStatus);
            }
        }
        drawTestBox();
    }

    private void drawTestBox() {
        if (isShowTestBox) {
            // Test
            if (mRecordingEnabled) {
                if (mPauseEnabled) {
                    drawBox();
                } else {
                    if ((++mFrameCount & 0x04) == 0) {
                        drawBox();
                    }
                }
            }
        }
    }

    private void resumeRecording() {
        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
        mRecordingStatus = RECORDING_ON;
        if(null != mOnRecordStatusChangedListener && mRecordingEnabled)
        {
            long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
            mOnRecordStatusChangedListener.onRecordResume( recordTime);
        }
    }

    private void startRecording(int textureID) {
        Log.d(TAG, "start recording");
        // fps
        int tmpFPS = 25;
        // quality
        int qualityParam = getQualityParam();
        // bitrate
        long bitrate = mRecordWidth * mRecordHeight * 3 * 8 * tmpFPS / qualityParam;
        Log.d(TAG, " record param：width:" + mRecordWidth + ",height:" + mRecordHeight + "bitrate:" + bitrate);
        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(mOutputFile, mRecordWidth, mRecordHeight, (int) bitrate, mCameraPreviewWidth,
                mCameraPreviewHeight, EGL14.eglGetCurrentContext()));
        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        mVideoEncoder.setTextureId(textureID);
        mRecordingStatus = RECORDING_ON;
        // notify the Listener
        if (null != mOnRecordStatusChangedListener) {
            mOnRecordStatusChangedListener.onRecordStart();
        }
    }


    /**
     * description:get the video quality
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    private int getQualityParam() {
        int qualityParam = 256;
        switch (mRecordQualityType) {
            case QUALITY_HIGH:
                qualityParam = 128;
                break;
            case QUALITY_NORMAL_HIGH:
                qualityParam = 192;
                break;
            case QUALITY_NORMAL:
                qualityParam = 256;
                break;
            case QUALITY_NORMAL_LOW:
                qualityParam = 384;
                break;
            case QUALITY_LOW:
                qualityParam = 512;
                break;
            default:
                break;
        }
        return qualityParam;
    }

    /**
     * 录制视频中暂停和恢复
     * @param aSurfaceTexture
     */
    public void pauseOnRecorder(SurfaceTexture aSurfaceTexture) {
        if (mPauseEnabled) {
            pauseTodo() ;
            return;
        }
        // 通知过暂停
        if (mIsNotifiedPause) {
            Log.d("TAG", "SurfaceRenderer 恢复录制");
            mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
        }
        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(aSurfaceTexture);

        if (null != mOnRecordStatusChangedListener) {
            // 通知过暂停
            if (mIsNotifiedPause) {
                // 得到当前录制时长，将时间转换为毫秒
                long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
                // 回调resume
                mOnRecordStatusChangedListener.onRecordResume(recordTime);
            }

            // 暂停恢复以后的录制时长发生了变化
            mCurTimeCalcRecordTime = System.currentTimeMillis();
            if (mCurTimeCalcRecordTime - mLastTimeCalcRecordTime >= mIntervalNotifyRecordProcessing) {
                mLastTimeCalcRecordTime = mCurTimeCalcRecordTime;
                // 得到当前录制时长，将时间转换为毫秒
                long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
                // 回调processing
                mOnRecordStatusChangedListener.onRecordProcessing(recordTime);
            }

        }
        mIsNotifiedPause = false;
    }

    public void setCameraHandler(Handler mCameraHandler) {
        this.mCameraHandler = mCameraHandler;
    }


    /**
     * 设置录制监听器
     *
     * @param listener
     */
    public void setOnRecordStatusChangedListener(OnRecordStatusChangedListener listener) {
        this.mOnRecordStatusChangedListener = listener;
    }

    /**
     * 暂停以后要做的事
     */
    private void pauseTodo(){
        // 没有通知过暂停
        if (!mIsNotifiedPause) {
            // 暂停编码
            mVideoEncoder.pause();
            Log.d("TAG", "SurfaceRenderer 暂停录制");

            if (null != mOnRecordStatusChangedListener) {
                long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
                mOnRecordStatusChangedListener.onRecordPaused(recordTime);
                mIsNotifiedPause = true;
            }
        }
    }




    /**
     * 绘制测试矩形
     */
    private void drawBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    /**
     * 停止录制以后需要做的事
     */
    public void stopRecordTodo() {
        // 得到当前录制时长，将时间转换为毫秒
        final long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
        // 停止录制并且回调对应函数
        mVideoEncoder.stopRecording(new TextureMovieEncoder.OnStopOverListener() {

            @Override
            public void onStopOver() {
                if (null != mOnRecordStatusChangedListener) {
                    mOnRecordStatusChangedListener.onRecordStop(mOutputFile, recordTime);
                }
            }
        });
    }

    /**
     * 配置录制质量，质量越高，画面越清晰，文件越大
     *
     * @param type
     */
    public void configRecordQualityType(int type) {
        this.mRecordQualityType = type;
    }

    /**
     * 配置录制尺寸
     *
     * @param recordWidth
     * @param recordHeight
     */
    public void configRecordSize(int recordWidth, int recordHeight) {
        this.mRecordWidth = recordWidth;
        this.mRecordHeight = recordHeight;
    }

    /**
     * 配置输出文件
     *
     * @param outputFile
     */
    public void configOutputFile(File outputFile) {
        this.mOutputFile = outputFile;
    }

    /**
     * 配置通知录制进度间隔时间，单位：毫秒
     *
     * @param interval
     */
    public void configIntervalNotifyRecordProcessing(int interval) {
        this.mIntervalNotifyRecordProcessing = interval;
    }

}
