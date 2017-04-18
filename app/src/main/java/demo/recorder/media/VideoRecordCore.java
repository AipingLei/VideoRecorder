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
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import demo.recorder.util.CameraUtils;
import demo.recorder.TextureMovieEncoder;
import demo.recorder.ui.CameraPreview;
import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.filter.FilterWrapper;


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

    private CameraPreview mCameraPreview;
    private TextureMovieEncoder mVideoEncoder;

    private int mDisplayWidth, mDisplayHeight;
    /**
     * Video output prams
     */
    private int mRecordWidth,mRecordHeight;
    private File mOutputFile;
    /**
     * define the video recording process is running or not
     */
    //private boolean isRunning;

    private int mRecordQualityType;
    protected OnRecordStatusChangedListener mOnRecordStatusChangedListener;
    /**
     * Video quality
     */
    public static final int QUALITY_HIGH = 0;
    public static final int QUALITY_NORMAL_HIGH = 1;
    public static final int QUALITY_NORMAL = 2;
    public static final int QUALITY_NORMAL_LOW = 3;
    public static final int QUALITY_LOW = 4;

    /**
     * handleRecordEvent control
     */
    private int mFrameCount;
    //private boolean mIsNotifiedPause;
    //private boolean mPauseEnabled;

    protected static final int RECORDING_IDEL = 0;
    protected static final int RECORDING_START= 1;
    protected static final int RECORDING_STARTED = 2;
    protected static final int RECORDING_RESUME = 3;
    protected static final int RECORDING_RESUMED = 4;
    protected static final int RECORDING_PAUSE = 5;
    protected static final int RECORDING_PAUSED = 6;
    protected static final int RECORDING_STOP = 7;
    protected static final int RECORDING_STOPPED = 8;

    private int mRecordingState = RECORDING_IDEL;
    private long mCurTimeCalcRecordTime;
    private long mLastTimeCalcRecordTime;
    protected int mIntervalNotifyRecordProcessing = 1000;


    public VideoRecordCore(GPUFilterType aDisplayFilterType,GPUFilterType aReocordFilterType) {
        this.mDisplayFilterType = aDisplayFilterType;
        this.mRecordFilterType = aReocordFilterType;
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
     * Sets mDisplayWidth and mDisplayHeight to the actual width/height of the preview.
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


        Camera.Size size = CameraUtils.determineBestPreviewSize(mCameraPreview.getContext(),parms,info.orientation);
        //parms.setPreviewSize(size.width, size.height);
        CameraUtils.setCameraDisplayOrientation((Activity)mCameraPreview.getContext(),id,mCamera);

        List<String> supportedFocusModes = parms.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parms.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        //parms.setPreviewSize(1080,1440);
        // leave the frame rate set to default
        mCamera.setParameters(parms);
        // setCameraDisplayOrientation(this,id,mCamera);
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        mDisplayWidth = mCameraPreviewSize.width;
        mDisplayHeight = mCameraPreviewSize.height;
        mCameraPreview.queueEvent(new Runnable() {
            @Override public void run() {
                mCameraPreview.setCameraPreviewSize(mDisplayWidth, mDisplayHeight);
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

    private CameraHandler mCameraHandler;
    @Override
    public void onSurfaceCreated(SurfaceTexture aSurfaceTexture) {
        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                VideoRecordCore.CameraHandler.MSG_SET_SURFACE_TEXTURE, aSurfaceTexture));
      /*  isRunning = mVideoEncoder.isRecording();
        if (isRunning) {
            mRecordingStatus = RECORDING_RESUME;
        }else {
            mRecordingStatus = RECORDING_OFF;
        }*/
        initFilterParam();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }


    @Override
    public void onDrawFrame(SurfaceTexture aSurfaceTexture, int aTextureID) {
        handleRecordEvent(aSurfaceTexture,aTextureID);
    }

    public void changeRecordingState(final int aState){
        if (null == mOutputFile) throw new IllegalStateException();
        if (this.mRecordingState == aState) return;
        mRecordingState = aState;
        if (aState == RECORDING_STOP){
                    mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "changeRecordingState: was " + VideoRecordCore.this.mRecordingState + " now " + aState);
                mRecordingState = aState;
            }
        });
        }else {
            mRecordingState = aState;
        }
    }

    public void destroy() {
        releaseCamera();
        stopRecording();
        if (mCameraHandler != null){
            mCameraHandler.invalidateHandler();
        }
    }

    /*public boolean isRecordingDisable(int aRecordingState) {
        return aRecordingState != RECORDING_RESUMED && aRecordingState != RECORDING_STARTED ;
    }*/

    /*public void changeRecordingState(final boolean isRecording) {
        if (null == mOutputFile) {
            throw new IllegalStateException();
        }
        if (this.isRunning == isRecording) return;
        mCameraPreview.queueEvent(new Runnable() {
            @Override
            public void run() {
                Log.d("TAG", "SurfaceRenderer changeRecordingState: was " + VideoRecordCore.this.isRunning + " now " + isRecording);
                VideoRecordCore.this.isRunning = isRecording;
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
    }*/

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
            mVideoRecordCore = new WeakReference<>(aVideoRecordCore);
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
     * description: Handle the handleRecordEvent event, this method is kinda weird
     * because the video should only run after the first SurfaceTexture is
     * drawing, besides,the encoder need to know the textureID which to share.
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    public void handleRecordEvent(SurfaceTexture aSurfaceTexture,int aTextureID) {
        switch (mRecordingState){
            case RECORDING_START:
                startRecording(aTextureID);
            case RECORDING_RESUME:
                resumeRecording();
                break;
            case RECORDING_PAUSE:
                pauseRecording();
                break;
            case RECORDING_STOP:
                stopRecording();
                break;
            default:
                break;
        }
        handleRecording(aSurfaceTexture);
    }


    private void resumeRecording() {
        if (mRecordingState != RECORDING_RESUME) return;
        //mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext(),null);
        mRecordingState = RECORDING_RESUMED;
        if(null != mOnRecordStatusChangedListener) {
            long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
            mOnRecordStatusChangedListener.onRecordResume(recordTime);
            // 暂停恢复以后的录制时长发生了变化
            mCurTimeCalcRecordTime = System.currentTimeMillis();
            if (mCurTimeCalcRecordTime - mLastTimeCalcRecordTime >= mIntervalNotifyRecordProcessing) {
                mLastTimeCalcRecordTime = mCurTimeCalcRecordTime;
                // 回调processing
                mOnRecordStatusChangedListener.onRecordProcessing(recordTime);
            }
        }
        mRecordingState = RECORDING_RESUMED;
    }

    private void startRecording(int textureID) {
        if (mRecordingState != RECORDING_START) return;
        Log.d(TAG, "start recording");
        // fps
        int tmpFPS = 25;
        // quality
        int qualityParam = getQualityParam();
        // bitrate
        long bitrate = mRecordWidth * mRecordHeight * 3 * 8 * tmpFPS / qualityParam;

        Log.d(TAG, " handleRecordEvent param：width:" + mRecordWidth + ",height:" + mRecordHeight + "bitrate:" + bitrate);
        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(mOutputFile, mRecordWidth, mRecordHeight, (int) bitrate, mDisplayWidth,
                mDisplayHeight, EGL14.eglGetCurrentContext()));
        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        mVideoEncoder.setTextureId(textureID);
        mRecordingState = RECORDING_STARTED;
        // notify the Listener
        if (null != mOnRecordStatusChangedListener) {
            mOnRecordStatusChangedListener.onRecordStart();
        }
    }

    private void pauseRecording() {
        if (mRecordingState != RECORDING_PAUSE) return;
        mRecordingState = RECORDING_PAUSED;
        mVideoEncoder.pauseRecording();
        Log.d(TAG, "SurfaceRenderer pauseRecording");
        if (null != mOnRecordStatusChangedListener) {
            long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000; // 将时间转换为毫秒
            mOnRecordStatusChangedListener.onRecordPaused(recordTime);
        }
    }

    public void stopRecording() {
        mVideoEncoder.stopRecording(new TextureMovieEncoder.OnStopOverListener() {
            @Override
            public void onStopOver() {
                if (null != mOnRecordStatusChangedListener) {
                    final long recordTime = mVideoEncoder.getRecordedTimeNanos() / 1000000;
                    Log.d(TAG, "sRecordTime3->"+recordTime);
                    mRecordingState = RECORDING_STOPPED;
                    mOnRecordStatusChangedListener.onRecordStop(mOutputFile, recordTime);
                }
            }
        });
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


    private void handleRecording(SurfaceTexture aSurfaceTexture) {
        if (mRecordingState != RECORDING_STARTED && mRecordingState != RECORDING_RESUMED) return;

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(aSurfaceTexture);
    }

    public void setCameraHandler(CameraHandler mCameraHandler) {
        this.mCameraHandler = mCameraHandler;
    }


    public void setOnRecordStatusChangedListener(OnRecordStatusChangedListener listener) {
        this.mOnRecordStatusChangedListener = listener;
    }

    private void drawTestBox() {
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(0, 0, 100, 100);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    public void configRecordQualityType(int type) {
        this.mRecordQualityType = type;
    }

    public void configRecordSize(int recordWidth, int recordHeight) {
        this.mRecordWidth = recordWidth;
        this.mRecordHeight = recordHeight;
    }

    public void configOutputFile(File outputFile) {
        this.mOutputFile = outputFile;
    }

    public void configIntervalNotifyRecordProcessing(int interval) {
        this.mIntervalNotifyRecordProcessing = interval;
    }

    /**
     * the filter for displaying by camera preview
     */
    private GPUFilterType mDisplayFilterType;
    /**
     * the filter for recording by movie encoder
     */
    private GPUFilterType mRecordFilterType;

    private void initFilterParam(){
        mVideoEncoder.setFilterWrapper(createFilterWrapper(mRecordFilterType,mRecordWidth,mRecordHeight));
        mCameraPreview.setFilterWrapper(createFilterWrapper(mDisplayFilterType,mDisplayWidth,mDisplayHeight));
    }

    private FilterWrapper createFilterWrapper(GPUFilterType aFilterType,int width,int height){
        FilterWrapper sFilterWrapper = FilterWrapper.buildFilterWrapper(aFilterType,0,false,false);
        sFilterWrapper.initFilter();
        sFilterWrapper.onOutpuSizeChanged(width, height);
        sFilterWrapper.onSurfaceSizeChanged(width, height);
        return  sFilterWrapper;
    }

    public long getReocordTime(){
        return  mVideoEncoder.getRecordedTimeNanos()/1000000;
    }


}
