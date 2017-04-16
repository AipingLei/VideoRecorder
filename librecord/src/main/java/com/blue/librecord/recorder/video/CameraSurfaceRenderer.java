package com.blue.librecord.recorder.video;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.blue.librecord.recorder.gles.GlUtil;

import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.filter.FilterWrapper;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.MagicCameraInputFilter;
import jp.co.cyberagent.android.gpuimage.util.MagicFilterParam;

/**
 * 摄像头数据处理器，用于绘制从摄像头采集到的数据、添加滤镜、开关录制功能
 * 外部调用此类中的所有函数都需要通过GLSurfaceView#queueEvent()提交调用
 * Created by blue on 2017/2/14.
 */

public class CameraSurfaceRenderer implements GLSurfaceView.Renderer {

    /**
     * 未开启录制
     */
    private static final int RECORDING_OFF = 0;
    /**
     * 开启录制
     */
    private static final int RECORDING_ON = 1;
    /**
     * 恢复录制
     */
    private static final int RECORDING_RESUMED = 2;
    /**
     * 暂停录制
     */
    private static final int RECORDING_PAUSE = 3;

    /**
     * 摄像头图像数据输出到的textureid，渲染器从该id中取出图像数据进行绘制
     */
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;
    private boolean mRecordingEnabled;
    private volatile boolean mPauseEnabled;
    // 是否已通知过暂停
    private boolean mIsNotifiedPause = false;

    /**
     * 当前录制状态
     */
    private int mRecordingStatus;
    private int mFrameCount;

    /**
     * 来源图像的宽度
     */
    private int mIncomingWidth;
    /**
     * 来源图像的高度
     */
    private int mIncomingHeight;

    /**
     * 显示的旋转角度
     */
    private int mDisplayOrientation;
    private boolean mFlipHorizontal;
    private boolean mFlipVertical;

    /**
     * 最近一次计算录制时间的时间，单位毫秒
     */
    private long mLastTimeCalcRecordTime;
    private long mCurTimeCalcRecordTime;

    private CameraCaptureView mCameraCaptureView;
    /**
     * 视频编码器
     */
    public TextureMovieEncoder mVideoEncoder;
    private Context mContext;

    // 预置滤镜
    private GPUFilterType presetFilterType;
    // 当前滤镜
    private GPUFilterType curFilterType;
    // 滤镜工具类
    private FilterFactory filterFactory;

    // 滤镜控制器
    private FilterWrapper filterWrapper;
    // 所有滤镜
    private Map<GPUFilterType, GPUImageFilter> filters;

    /**
     * 用于绘制相机预览数据
     */
    private MagicCameraInputFilter mCameraInputFilter;

    public CameraSurfaceRenderer(CameraCaptureView cameraCaptureView) {
        mCameraCaptureView = cameraCaptureView;
        mContext = mCameraCaptureView.getContext();
        mVideoEncoder = mCameraCaptureView.sVideoEncoder;

        mTextureId = -1;

        mRecordingStatus = -1;
        mRecordingEnabled = false;
        mPauseEnabled = false;
        mFrameCount = -1;

        mIncomingWidth = mIncomingHeight = -1;
    }

    /**
     * 通知暂停状态，在onPause中调用
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d("TAG", "SurfaceRenderer notifyPausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mIncomingWidth = mIncomingHeight = -1;

        if (mRecordingEnabled) {
            pauseTodo();
        }
    }

    /**
     * 通知renderer改变录制状态
     */
    public void changeRecordingState(boolean isRecording) {
        Log.d("TAG", "SurfaceRenderer changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
        mRecordingEnabled = isRecording;
    }

    /**
     * 通知renderer改变暂停状态
     */
    public void changePauseState(boolean isPause) {
        mPauseEnabled = isPause;
    }

    public void setDisplayOrientation(int orientation, boolean flipHorizontal, boolean flipVertical) {
        mDisplayOrientation = orientation;
        mFlipHorizontal = flipHorizontal;
        mFlipVertical = flipVertical;

        if (null != filterWrapper) {
            switch (mDisplayOrientation) {
                case 90:
                    filterWrapper.setRotation(Rotation.ROTATION_90, mFlipHorizontal, mFlipVertical);
                    break;
                case 180:
                    filterWrapper.setRotation(Rotation.ROTATION_180, mFlipHorizontal, mFlipVertical);
                    break;
                case 270:
                    filterWrapper.setRotation(Rotation.ROTATION_270, mFlipHorizontal, mFlipVertical);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 设置摄像头预览尺寸
     *
     * @param width
     * @param height
     */
    public void setCameraPreviewSize(int width, int height) {
        Log.d("TAG", "SurfaceRenderer setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d("TAG", "SurfaceRenderer onSurfaceCreated mDisplayOrientation:" + mDisplayOrientation);

        mRecordingEnabled = mVideoEncoder.isRecording();
        // 当前开启了录制
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        }
        // 当前未开启录制
        else {
            mRecordingStatus = RECORDING_OFF;
        }

        // 初始化gpu参数
        MagicFilterParam.initMagicFilterParam(unused);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtil.checkGlError("glGenTextures");

        int texId = textures[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GlUtil.checkGlError("glTexParameter");
        mTextureId = texId;

        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // 连接surface
        bindSurfaceTexture();

        filterWrapper = new FilterWrapper(new GPUImageFilter());
        filterWrapper.initFilter();

        switch (mDisplayOrientation) {
            case 90:
                filterWrapper.setRotation(Rotation.ROTATION_90, mFlipHorizontal, mFlipVertical);
                break;
            case 180:
                filterWrapper.setRotation(Rotation.ROTATION_180, mFlipHorizontal, mFlipVertical);
                break;
            case 270:
                filterWrapper.setRotation(Rotation.ROTATION_270, mFlipHorizontal, mFlipVertical);
                break;
            default:
                break;
        }

        // 摄像头采集数据转换器
        mCameraInputFilter = new MagicCameraInputFilter();
        mCameraInputFilter.init();

        // 滤镜初始化
       /* filterFactory = new FilterFactory(mContext);
        filters = filterFactory.getFilters();*/
        if (null != presetFilterType) {
            setFilter(presetFilterType);
        } else if (null != curFilterType) {
            setFilter(curFilterType);
        }
    }

    /**
     * 绑定surfaceTexture
     */
    public void bindSurfaceTexture() {
        Log.d("TAG", "SurfaceRenderer bindSurfaceTexture");
        mCameraCaptureView.handleSetSurfaceTexture(mSurfaceTexture);
    }

    /**
     * 直接设置滤镜
     *
     * @param filter
     */
    public void setFilter(GPUImageFilter filter) {
        filterWrapper.setFilter(filter);
        filterWrapper.onOutpuSizeChanged(surfaceWidth, surfaceHeight);
        filterWrapper.onSurfaceSizeChanged(surfaceWidth, surfaceHeight);
        mVideoEncoder.setFilter(filter);
    }

    /**
     * 根据type设置滤镜
     *
     * @param filterType
     */
    public void setFilter(GPUFilterType filterType) {
        if (null != filters) {
            GPUImageFilter filter = filters.get(filterType);
            filterWrapper.setFilter(filter);
            filterWrapper.onOutpuSizeChanged(surfaceWidth, surfaceHeight);
            filterWrapper.onSurfaceSizeChanged(surfaceWidth, surfaceHeight);
            mVideoEncoder.setFilter(filter);
            presetFilterType = null; // 重置预置滤镜
        } else // filter还未初始化完成，在初始化后进行设置
        {
            presetFilterType = filterType;
        }
        curFilterType = filterType;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d("TAG", "SurfaceRenderer onSurfaceChanged " + width + "x" + height);

        GLES20.glViewport(0, 0, width, height);
        filterWrapper.onOutpuSizeChanged(width, height);
        filterWrapper.onSurfaceSizeChanged(width, height);
        surfaceWidth = width;
        surfaceHeight = height;

        mCameraInputFilter.onOutputSizeChanged(width, height);
        mCameraInputFilter.initCameraFrameBuffer(surfaceWidth, surfaceHeight);
    }

    private int surfaceWidth;
    private int surfaceHeight;

    int preFPS = 0;
    int fps = 0;
    long preTime = 0;

    // 调试功能，是否开启打印fps
    private boolean isPrintFPS = true;

    // 调试功能，是否开启在录制期间绘制闪烁矩形，
    private boolean isShowTestBox = true;

    float[] mtx = new float[16];

    @SuppressLint("WrongCall")
    @Override
    public void onDrawFrame(GL10 unused) {
        Log.d("TAG", "SurfaceRenderer onDrawFrame");
        if (isPrintFPS) {
            long curTime = System.currentTimeMillis();
            ++fps;
            if (curTime - preTime >= 1000) {
                Log.i("TAG", "SurfaceRenderer fps preview:" + fps);
                preTime = curTime;
                preFPS = fps;
                fps = 0;
            }
        }

        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            Log.d("TAG", "SurfaceRenderer 当前还未设置来源图像的宽高，忽略绘制");
            return;
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mtx);

        // 转换摄像头数据并且绘制
        mCameraInputFilter.setTextureTransformMatrix(mtx);
        mCameraInputFilter.onDraw(mTextureId);
        // 滤镜绘制
        int textureID = mCameraInputFilter.onDrawToTexture(mTextureId, surfaceWidth, surfaceHeight);
        filterWrapper.drawFrame(textureID, mIncomingWidth, mIncomingHeight);

        // 录制
        mp4Recorder(textureID);
    }

    /**
     * 录制视频
     *
     * @param textureID
     */
    public void mp4Recorder(int textureID) {
        // 开启录制时
        if (mRecordingEnabled) {
            switch (mRecordingStatus) {
                // 当前还未开启录制
                case RECORDING_OFF:
                    Log.d("TAG", "SurfaceRenderer 开始录制");
                    // 帧数
                    int tmpFPS = 25;
                    // 视频质量
                    int qualityParam = 256;
                    // 设置视频质量
                    switch (mCameraCaptureView.mRecordQualityType) {
                        case TYPE_HIGH:
                            qualityParam = 128;
                            break;
                        case TYPE_NORMAL_HIGH:
                            qualityParam = 192;
                            break;
                        case TYPE_NORMAL:
                            qualityParam = 256;
                            break;
                        case TYPE_NORMAL_LOW:
                            qualityParam = 384;
                            break;
                        case TYPE_LOW:
                            qualityParam = 512;
                            break;
                        default:
                            break;
                    }
                    // 视频码率
                    long bitrate = mCameraCaptureView.mRecordWidth * mCameraCaptureView.mRecordHeight * 3 * 8 * tmpFPS / qualityParam;
                    Log.d("TAG", "SurfaceRenderer 录制参数：width:" + mCameraCaptureView.mRecordWidth + ",height:" + mCameraCaptureView.mRecordHeight + "bitrate:" + bitrate);
                    // 开始录制
                    mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(mCameraCaptureView.mOutputFile, mCameraCaptureView.mRecordWidth,
                            mCameraCaptureView.mRecordHeight, (int) bitrate, mIncomingWidth, mIncomingHeight, mDisplayOrientation, mFlipHorizontal, mFlipVertical, EGL14.eglGetCurrentContext()));

                    // 通知编码器当前的textureid
                    mVideoEncoder.setTextureId(textureID);

                    // 刷新录制状态
                    mRecordingStatus = RECORDING_ON;
                    // 回调start
                    if (null != mCameraCaptureView.mOnRecordStatusChangedListener) {
                        mCameraCaptureView.mOnRecordStatusChangedListener.onRecordStart(mCameraCaptureView);
                    }

                    break;
                case RECORDING_RESUMED:
//					Log.d(TAG, "恢复录制");
//					mVideoEncoder.updateSharedContext(EGL14
//							.eglGetCurrentContext());
//					mRecordingStatus = RECORDING_ON;
//
//					if(null != mOnRecordStatusChangedListener && mRecordingEnabled)
//					{
//						long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
//						mOnRecordStatusChangedListener.onRecordResume(CameraCaptureView.this, recordTime);
//					}
                    break;
                case RECORDING_ON:
                    break;
                default:
                    throw new RuntimeException("未知状态: " + mRecordingStatus);
            }

            pauseOnRecorder();
        }
        // 关闭录制时
        else {
            switch (mRecordingStatus) {
                // 如果当前正在录制，立即结束
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    Log.d("TAG", "SurfaceRenderer 停止录制");
                    mCameraCaptureView.stopRecordTodo();
                    // 刷新录制状态
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    break;
                default:
                    throw new RuntimeException("未知状态: " + mRecordingStatus);
            }
        }

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

    /**
     * 录制视频中暂停和恢复
     */
    public void pauseOnRecorder() {
        // 恢复
        if (!mPauseEnabled) {
            // 通知过暂停
            if (mIsNotifiedPause) {
                Log.d("TAG", "SurfaceRenderer 恢复录制");
                mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
            }

            // 通知编码器图像数据已产生，可以进行编码
            mVideoEncoder.frameAvailable(mSurfaceTexture);

            if (null != mCameraCaptureView.mOnRecordStatusChangedListener) {
                // 通知过暂停
                if (mIsNotifiedPause) {
                    // 得到当前录制时长，将时间转换为毫秒
                    long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
                    // 回调resume
                    mCameraCaptureView.mOnRecordStatusChangedListener.onRecordResume(mCameraCaptureView, recordTime);
                }

                // 暂停恢复以后的录制时长发生了变化
                mCurTimeCalcRecordTime = System.currentTimeMillis();
                if (mCurTimeCalcRecordTime - mLastTimeCalcRecordTime >= mCameraCaptureView.mIntervalNotifyRecordProcessing) {
                    mLastTimeCalcRecordTime = mCurTimeCalcRecordTime;
                    // 得到当前录制时长，将时间转换为毫秒
                    long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
                    // 回调processing
                    mCameraCaptureView.mOnRecordStatusChangedListener.onRecordProcessing(mCameraCaptureView, recordTime);
                }

            }
            mIsNotifiedPause = false;
        }
        // 暂停
        else {
            pauseTodo();
        }
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

            if (null != mCameraCaptureView.mOnRecordStatusChangedListener) {
                long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
                mCameraCaptureView.mOnRecordStatusChangedListener.onRecordPaused(mCameraCaptureView, recordTime);
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
}
