package demo.recorder.media;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import demo.recorder.CameraCaptureActivity;
import demo.recorder.CameraUtils;
import demo.recorder.TextureMovieEncoder;
import demo.recorder.ui.CameraPreview;

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

        Camera.Parameters parms = mCamera.getParameters();
        CameraUtils.choosePreviewSize(parms, 640, 480);
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

    @Override
    public void onSurfaceCreated(SurfaceTexture aSurfaceTexture) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(SurfaceTexture aSurfaceTexture, int aTexureID) {
        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
        //
        // TODO: be less lame.
        mVideoEncoder.setTextureId(aTexureID);

        // Tell the video encoder thread that a new frame is available.
        // This will be ignored if we're not actually recording.
        mVideoEncoder.frameAvailable(aSurfaceTexture);
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
}
