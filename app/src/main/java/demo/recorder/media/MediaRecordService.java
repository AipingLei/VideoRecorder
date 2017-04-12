package demo.recorder.media;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import demo.recorder.MainActivity;
import demo.recorder.ui.CameraPreview;

/** 
 * description: A service to generate media file(support video and audio now)
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
*/
public class MediaRecordService {

    private static final String TAG = MainActivity.TAG;

    private static final boolean VERBOSE = false;

    protected static final int RECORDING_OFF = 0;
    protected static final int RECORDING_ON = 1;
    protected static final int RECORDING_RESUMED = 2;

    private File mOutputFile;

    private File mCacheDir;

    private VideoRecordCore mVideoRecordCore;

    List<MediaObject> mMediaList;


    public MediaRecordService(CameraPreview cameraPreview) {
        this.mMediaList = new ArrayList<>();
        mVideoRecordCore = new VideoRecordCore();
        mVideoRecordCore.bindPreview(cameraPreview);
        cameraPreview.setCameraHandler(new VideoRecordCore.CameraHandler(mVideoRecordCore));
    }


    public void addMedia(MediaObject mediaObject){
        mMediaList.add(mediaObject);
    }

    public void start(){

    }

    public void stop(){

    }

    public void pause(){
        mVideoRecordCore.onPause();
/*        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        mGLView.onPause();
        Log.d(TAG, "onPause complete");*/
    }

    public void resume(){
      /*  updateControls();
        openCamera(1280, 720);      // updates mCameraPreviewWidth/Height

        // Set the preview aspect ratio.
        AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
        layout.setAspectRatio((double) mCameraPreviewWidth / mCameraPreviewHeight);

        mGLView.onResume();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);
            }
        });
        Log.d(TAG, "onResume complete: " + this);*/
        mVideoRecordCore.openCamera(480, 480);      // updates mCameraPreviewWidth/Height
    }
    public void destroy() {
        //mCameraHandler.invalidateHandler();     // paranoia
    }



}
