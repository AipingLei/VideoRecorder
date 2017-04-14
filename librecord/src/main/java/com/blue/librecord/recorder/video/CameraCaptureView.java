package com.blue.librecord.recorder.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

/**
 * 视频预览、录制View，可直接使用或继承后进行二次开发；例如:实现正方向的预览视图、添加手势操作等
 * 视频录像view,支持系统4.3+（视频录制需要4.2，硬件编码合成需要4.3）
 *
 * @author leisu@iflytek.com
 */
public class CameraCaptureView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {

    /**
     * 摄像头开启
     */
    protected void onCameraOpened() {
    }

    /**
     * 摄像头关闭
     */
    protected void onCameraClosed() {
    }

    static final int PREVIEW_SIZE_BEST_WIDTH = 720; // 预览最优值
    static final int PREVIEW_SIZE_MAX_WIDTH = 1280; // 预览最大宽度
    static final int PREVIEW_SIZE_MIN_WIDTH = 240; // 预览最小宽度

    protected boolean mRecordingEnabled; // 是否开启录制
    protected boolean mPauseEnabled; // 是否为暂停录制状态
    protected CameraSurfaceRenderer mRenderer;
    protected File mOutputFile; // 输出文件

    protected int mCameraPreviewWidth, mCameraPreviewHeight; // 摄像头预览宽高
    protected Camera mCamera;

    // 视频编码器
    protected TextureMovieEncoder sVideoEncoder;

    /**
     * 摄像头方位，默认前置
     * CAMERA_FACING_FRONT前置
     * CAMERA_FACING_BACK后置
     */
    protected int currentCameraId = CameraInfo.CAMERA_FACING_FRONT; // 当前摄像头id

    /**
     * 录制宽高
     */
    protected int mRecordWidth = 480, mRecordHeight = 480;
    /**
     * 录制质量
     */
    protected RecordQualityType mRecordQualityType = RecordQualityType.TYPE_NORMAL;
    /**
     * 通知录制进度间隔时间，单位：毫秒
     */
    protected int mIntervalNotifyRecordProcessing = 1000;
    /**
     * 录制监听器
     */
    protected OnRecordStatusChangedListener mOnRecordStatusChangedListener;

    // 是否正在预览
    boolean isPreview = false;
    // 是否被onPause暂停了预览
    private boolean isPreviewByOnPause = false;

    public CameraCaptureView(Context context) {
        super(context);
        init();
    }

    public CameraCaptureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Log.d("TAG", "CaptureView init");
        setEGLContextClientVersion(2); // 设置gl版本为2.0
        sVideoEncoder = new TextureMovieEncoder();
        mRecordingEnabled = sVideoEncoder.isRecording();
        mRenderer = new CameraSurfaceRenderer(this);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * 配置录制质量，质量越高，画面越清晰，文件越大
     *
     * @param type
     */
    public void configRecordQualityType(RecordQualityType type) {
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

    /**
     * 设置录制监听器
     *
     * @param listener
     */
    public void setOnRecordStatusChangedListener(OnRecordStatusChangedListener listener) {
        this.mOnRecordStatusChangedListener = listener;
    }

    /**
     * 开始录制
     */
    public void startRecord() {
        if (!mRecordingEnabled) {
            if (null == mOutputFile) {
                throw new IllegalStateException("请先通过configOutputFile配置视频输出文件");
            }
            mRecordingEnabled = true;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.changeRecordingState(mRecordingEnabled);
                }
            });
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (mRecordingEnabled) {
            mRecordingEnabled = false;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.changeRecordingState(mRecordingEnabled);
                }
            });
        }
    }

    /**
     * 停止录制以后需要做的事
     */
    public void stopRecordTodo() {
        // 得到当前录制时长，将时间转换为毫秒
        final long recordTime = sVideoEncoder.getRecordedTimeNanos() / 1000000;
        // 停止录制并且回调对应函数
        sVideoEncoder.stopRecording(new TextureMovieEncoder.OnStopOverListener() {

            @Override
            public void onStopOver() {
                if (null != mOnRecordStatusChangedListener) {
                    mOnRecordStatusChangedListener.onRecordStop(CameraCaptureView.this, mOutputFile, recordTime);
                }
            }
        });
    }

    /**
     * 暂停录制
     */
    public void pauseRecord() {
        if (!mPauseEnabled) {
            mPauseEnabled = true;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.changePauseState(mPauseEnabled);
                }
            });
        }
    }

    /**
     * 恢复录制
     */
    public void resumeRecord() {
        if (mPauseEnabled) {
            mPauseEnabled = false;
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.changePauseState(mPauseEnabled);
                }
            });
        }
    }

    /**
     * 是否已启用录制
     */
    public boolean isRecordEnabled() {
        return this.mRecordingEnabled;
    }

    /**
     * 是否已暂停录制
     *
     * @return
     */
    public boolean isRecordPaused() {
        return this.mPauseEnabled;
    }

    @Override
    public void onResume() {
        Log.d("TAG", "CaptureView onResume");
        if (isPreviewByOnPause) {
            startPreview();
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (null != mRenderer) {
                        mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
                    }
                }
            });
        }
        isPreviewByOnPause = false;
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d("TAG", "CaptureView onPause");
        if (null != mCamera && isPreview) {
            isPreviewByOnPause = true;
            stopPreview();
        }
        if (isRecordEnabled()) {
            stopRecordTodo();
        }
        queueEvent(new Runnable() {
            @Override
            public void run() {
                // 这里的pause要销毁SurfaceTexture所以调用notifyPausing而不是pauseRecord
                mRenderer.notifyPausing();
            }
        });
        super.onPause();
    }

    /**
     * 根据摄像头支持的所有预览参数
     *
     * @param parameters
     * @return
     */
    protected Size determineBestPreviewSize(Camera.Parameters parameters) {
        return determineBestSize(parameters.getSupportedPreviewSizes(), PREVIEW_SIZE_BEST_WIDTH, PREVIEW_SIZE_MAX_WIDTH, PREVIEW_SIZE_MIN_WIDTH);
    }

    protected Size determineBestSize(List<Size> sizes, int bestWidth, int maxWidth, int minWidth) {
        Size bestSize = null;
        Size bestSize2 = null;
        Size size;
        int numOfSizes = sizes.size();

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        // 优先匹配屏幕尺寸比例相同的，如果未匹配上，再匹配尺寸比例最接近的
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        final int displayOrientation = mCameraInfo.orientation;

        // 得到当前屏幕的宽高比
        float scale;
        switch (displayOrientation) {
            case 90:
            case 270: {
                scale = (float) height / width;
            }
            break;
            default: {
                scale = (float) width / height;
            }
            break;
        }

        // 对摄像头支持分辨率参数进行排序
        // 先按宽从大到小排
        // 宽相同的情况下，按照高从大到小排
        // 宽和高都相同的情况下，按照数字从大到小排
        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                if (lhs.width != rhs.width) {
                    return lhs.width - rhs.width;
                } else if (lhs.height != rhs.height) {
                    return lhs.height - rhs.height;
                } else {
                    return (lhs.width + lhs.height) - (rhs.width + rhs.height);
                }
            }
        });

        int sizeWidth;
        int sizeHeight;
        // 默认情况下摄像头返回的宽高是相反
        // 300x400 宽返回400 高返回300
        for (int i = 0; i < numOfSizes; i++) {
            size = sizes.get(i);
            switch (displayOrientation) {
                case 90:
                case 270: {
                    sizeWidth = size.width;
                    sizeHeight = size.height;
                }
                break;
                default:
                    sizeWidth = size.height;
                    sizeHeight = size.width;
                    break;
            }

            // 摄像头的宽高比和当前屏幕的宽高比差距小于0.01，近似相等
            boolean isDesireRatio = Math.abs(((float) sizeWidth / sizeHeight) - scale) <= 0.01;
            // 宽高比相同的情况下，宽度越大越好
            boolean isBetterSize = (bestSize == null) || sizeWidth > bestSize.width;
            if (isDesireRatio && isBetterSize) {
                if (sizeWidth <= maxWidth && sizeHeight <= maxWidth && (sizeWidth >= minWidth && sizeHeight >= minWidth)) {
                    if (null != bestSize) {
                        if (Math.abs(sizeWidth - bestWidth) <= Math.abs(bestSize.width - bestWidth)) {
                            bestSize = size;
                        }
                    } else {
                        bestSize = size;
                    }
                }
            }

            if (null == bestSize2) {
                bestSize2 = size;
            }
            // 第二优先级
            // 摄像头的宽高比和当前屏幕的宽高比差距越小越好
            boolean isDesireRatio2 = Math.abs(((float) sizeWidth / sizeHeight) - scale) <= Math.abs(((float) bestSize2.width / bestSize2.height) - scale);
            if (isDesireRatio2) {
                if (sizeWidth <= maxWidth && sizeWidth >= minWidth) {
                    if (Math.abs(sizeWidth - bestWidth) <= Math.abs(bestSize2.width - bestWidth)) {
                        bestSize2 = size;
                    }
                }
            }
        }

        if (bestSize == null) {
            if (bestSize2 == null) {
                Log.d("TAG", "CaptureView cannot find the best camera size");
                return sizes.get(sizes.size() - 1);
            } else {
                bestSize = bestSize2;
            }
        }

        return bestSize;
    }

    /**
     * 获取屏幕方向，并校正摄像头方向
     */
    protected void determineDisplayOrientation() {
        int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // 调整镜像角度
        } else {
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        if (null != mCamera) {
            mCamera.setDisplayOrientation(result);
        }

    }

    private boolean isFlipHorizontal() {
        return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT ? true : false;
    }

    /**
     * 是否有摄像头
     *
     * @return
     */
    public boolean hasCamera() {
        return Camera.getNumberOfCameras() > 0;
    }

    private int supportBackCamera = 0;

    /**
     * 是否支持后置摄像头
     *
     * @return
     */
    public boolean isSupportBackCamera() {
        if (supportBackCamera != 0) {
            return supportBackCamera > 0;
        }
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; ++i) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                supportBackCamera = 1;
                break;
            } else {
                supportBackCamera = -1;
            }
        }
        return supportBackCamera > 0;
    }

    private int supportFrontCamera = 0;

    /**
     * 是否支持前置摄像头
     *
     * @return
     */
    public boolean isSupportFrontCamera() {
        if (supportFrontCamera != 0) {
            return supportFrontCamera > 0;
        }
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; ++i) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                supportFrontCamera = 1;
                break;
            } else {
                supportFrontCamera = -1;
            }
        }
        return supportFrontCamera > 0;
    }

    /**
     * 设置滤镜
     *
     * @param filter
     */
    public void setFilter(final GPUImageFilter filter) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setFilter(filter);
            }
        });
    }

    /**
     * 设置滤镜
     *
     * @param filterType
     */
    public void setFilter(final GPUFilterType filterType) {
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setFilter(filterType);
            }
        });
    }

    /**
     * 获取当前摄像头id
     *
     * @return
     */
    public int getCurrentCameraId() {
        return this.currentCameraId;
    }

    /**
     * 切换至前置摄像头
     */
    public void switchCameraToFront() {
        if (isSupportFrontCamera() && this.currentCameraId == CameraInfo.CAMERA_FACING_BACK) {
            closeCamera();
            this.currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            openCamera();
            mRenderer.bindSurfaceTexture();
        }
    }

    /**
     * 切换至后置摄像头
     */
    public void switchCameraToBack() {
        if (isSupportBackCamera() && this.currentCameraId == CameraInfo.CAMERA_FACING_FRONT) {
            closeCamera();
            this.currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            openCamera();
            mRenderer.bindSurfaceTexture();
        }
    }

    /**
     * 开始预览摄像头数据
     *
     * @return
     */
    public synchronized boolean startPreview() {
        Log.d("TAG", "CaptureView startPreview");
        boolean result = openCamera();
        if (result) {
            isPreview = true;
        }
        if (null != mCamera) {
            mRenderer.bindSurfaceTexture();
        }

        return result;
    }

    /**
     * 停止预览
     */
    public synchronized void stopPreview() {
        Log.d("TAG", "CaptureView stopPreview");
        isPreview = false;
        stopRecord();
        closeCamera();
    }

    private CameraInfo mCameraInfo = new CameraInfo();

    /**
     * 打开摄像头
     */
    protected boolean openCamera() {
        Log.d("TAG", "CaptureView openCamera");
        if (mCamera != null) {
            throw new RuntimeException("摄像头已经初始化，不能重复进行初始化");
        }

        closeCamera();

        // 得到摄像头的个数
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == currentCameraId) {
                try {
                    // 打开当前选中的摄像头
                    mCamera = Camera.open(i);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if (mCamera == null) {
            Log.d("TAG", "CaptureView 无法打开选中的摄像头，即将尝试打开默认摄像头");
            try {
                mCamera = Camera.open();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (mCamera == null) {
            Log.e("TAG", "CaptureView 无法打开摄像头");
            return false;
        }

        // 获取该摄像头的各项参数
        Camera.Parameters parms = mCamera.getParameters();
        // 获取到该摄像头的最佳预览尺寸
        Size bestPreviewSize = determineBestPreviewSize(parms);
        Log.d("TAG", "CaptureView bestPreviewSize:" + bestPreviewSize.width + "," + bestPreviewSize.height);
        parms.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        parms.setRecordingHint(true);

        // 设置聚焦
        List<String> supportedFocusModes = parms.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            // 开启连续对焦功能
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        // 矫正摄像头角度
        determineDisplayOrientation();
        mCamera.setParameters(parms);

        Size mCameraPreviewSize = parms.getPreviewSize();

        mCameraPreviewWidth = mCameraPreviewSize.height;
        mCameraPreviewHeight = mCameraPreviewSize.width;

        mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);

        onCameraOpened();
        return true;
    }

    /**
     * 停止预览，并释放摄像头
     */
    private void closeCamera() {
        Log.d("TAG", "CaptureView closeCamera");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

            onCameraClosed();
        }
    }


    /**
     * 连接SurfaceTexture并开始预览
     *
     * @param st
     */
    protected void handleSetSurfaceTexture(SurfaceTexture st) {
        Log.d("TAG", "CaptureView handleSetSurfaceTexture st:" + st);
        if (null != st) {
            st.setOnFrameAvailableListener(this);
            try {
                if (null != mCamera) {
                    mCamera.setPreviewTexture(st);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            if (isPreview) {
                mCamera.startPreview();
            }
        }
    }

    public int getFPS() {
        return mRenderer.preFPS;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d("TAG", "CaptureView onFrameAvailable");
        // 请求渲染图像，回调renderer的onDrawFrame方法
        requestRender();
    }
}
