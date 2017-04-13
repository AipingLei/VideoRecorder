package demo.recorder.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import demo.recorder.gles.canvas.CanvasGL;
import demo.recorder.gles.canvas.glcanvas.CameraTexture;
import demo.recorder.media.TexureObserver;

/**
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
 */
public  class CameraPreview extends GLSurfaceView implements GLSurfaceView.Renderer{
    private static final String TAG = "CameraPreview";
    protected CanvasGL mCanvas;

    protected CameraTexture mTexture;

    private OnSizeChangeCallback onSizeChangeCallback;

    protected GL10 gl;

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private TexureObserver mObServer;
    private Bitmap bitmap;
    private Canvas normalCanvas;
    Paint mPaint = new Paint();



    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setObServer(TexureObserver observer){
        mObServer = observer;
    }

    protected void init() {
        //setZOrderOnTop(true);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        bitmap = Bitmap.createBitmap(480,480, Bitmap.Config.ARGB_8888);
        mPaint = new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth((float) 3.0);              //线宽
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCanvas = new CanvasGL();
        mTexture = new CameraTexture(mIncomingWidth,mIncomingHeight,false, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);

        mTextureId = mTexture.getId();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mObServer.onSurfaceCreated(mSurfaceTexture);
        // Tell the UI thread to enable the camera preview.
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mCanvas.setSize(width, height);
        mObServer.onSurfaceChanged(gl,width,height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();
        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            Log.i(TAG, "Drawing before incoming texture size set; skipping");
            return;
        }
/*
        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);*/
        this.gl = gl;
        mCanvas.clearBuffer();
        onGLCanvasDraw(mCanvas);
        mObServer.onDrawFrame(mSurfaceTexture,mTextureId);
    }


    int[] prePoint = new int[2];
    int mFramecount;

    private void onGLCanvasDraw(CanvasGL aCanvas) {
        mFramecount++;
        //normalCanvas = new Canvas(bitmap);
        aCanvas.drawSurfaceTexture(mTexture,mSurfaceTexture,0,0,mIncomingWidth,mIncomingHeight);
       /* boolean invalidata = false;
        if (mFramecount % 10 ==0){
            Path path = new Path();                     //Path对象
            path.moveTo(prePoint[0], prePoint[1]);                         //起始点
            int fator = (mFramecount % 20 == 0)? -1:1;
            prePoint[0] += 20;
            if (prePoint[0] > 480) prePoint[0]=0;
            prePoint[1] += 70*fator;
            path.lineTo(prePoint[0], prePoint[1]);
            normalCanvas.drawPath(path, mPaint);                   //绘制任意多边形
            aCanvas.drawBitmap(bitmap, 0, 0,true);
        }else{
            aCanvas.drawBitmap(bitmap, 0, 0,false);
        }*/
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (onSizeChangeCallback != null) {
            onSizeChangeCallback.onSizeChange(w, h, oldw, oldh);
        }
    }

    interface OnSizeChangeCallback {
        void onSizeChange(int w, int h, int oldw, int oldh);
    }

    public void setOnSizeChangeCallback(CameraPreview.OnSizeChangeCallback onSizeChangeCallback) {
        this.onSizeChangeCallback = onSizeChangeCallback;
    }


    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            Log.d(TAG, "renderer pausing -- releasing SurfaceTexture");
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        mIncomingWidth = mIncomingHeight = -1;
    }


    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    /**
     * Records the size of the incoming camera preview frames.
     * <p>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        Log.d(TAG, "setCameraPreviewSize");
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;
    }

    /*   @Override
       public void onDrawFrame(GL10 unused) {
           if (VERBOSE) Log.d(TAG, "onDrawFrame tex=" + mTextureId);
           boolean showBox = false;

           // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
           // was there before.
           mSurfaceTexture.updateTexImage();

           // If the recording state is changing, take care of it here.  Ideally we wouldn't
           // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
           // makes it hard to do elsewhere.
           if (mRecordingEnabled) {
               switch (mRecordingStatus) {
                   case RECORDING_OFF:
                       Log.d(TAG, "START recording");
                       // start recording
                       mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
                               mOutputFile, 640, 480, 1000000, EGL14.eglGetCurrentContext()));
                       mRecordingStatus = RECORDING_ON;
                       break;
                   case RECORDING_RESUMED:
                       Log.d(TAG, "RESUME recording");
                       mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                       mRecordingStatus = RECORDING_ON;
                       break;
                   case RECORDING_ON:
                       // yay
                       break;
                   default:
                       throw new RuntimeException("unknown status " + mRecordingStatus);
               }
           } else {
               switch (mRecordingStatus) {
                   case RECORDING_ON:
                   case RECORDING_RESUMED:
                       // stop recording
                       Log.d(TAG, "STOP recording");
                       mVideoEncoder.stopRecording();
                       mRecordingStatus = RECORDING_OFF;
                       break;
                   case RECORDING_OFF:
                       // yay
                       break;
                   default:
                       throw new RuntimeException("unknown status " + mRecordingStatus);
               }
           }

           // Set the video encoder's texture name.  We only need to do this once, but in the
           // current implementation it has to happen after the video encoder is started, so
           // we just do it here.
           //
           // TODO: be less lame.
           mVideoEncoder.setTextureId(mTextureId);

           // Tell the video encoder thread that a new frame is available.
           // This will be ignored if we're not actually recording.
           mVideoEncoder.frameAvailable(mSurfaceTexture);

           if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
               // Texture size isn't set yet.  This is only used for the filters, but to be
               // safe we can just skip drawing while we wait for the various races to resolve.
               // (This seems to happen if you toggle the screen off/on with power button.)
               Log.i(TAG, "Drawing before incoming texture size set; skipping");
               return;
           }
           if (mIncomingSizeUpdated) {
               mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
               mIncomingSizeUpdated = false;
           }

           // Draw the video frame.
           mSurfaceTexture.getTransformMatrix(mSTMatrix);
           mFullScreen.drawFrame(mTextureId, mSTMatrix);
           // Draw a flashing box if we're recording.  This only appears on screen.
           showBox = (mRecordingStatus == RECORDING_ON);
       }*/
}
