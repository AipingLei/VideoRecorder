package demo.recorder.ui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;



import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import demo.recorder.gles.GlUtil;
import demo.recorder.media.TexureObserver;
import jp.co.cyberagent.android.gpuimage.filter.FilterWrapper;
import jp.co.cyberagent.android.gpuimage.filter.MagicCameraInputFilter;

/**
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
 */
public  class CameraPreview extends GLSurfaceView implements GLSurfaceView.Renderer{
    private static final String TAG = "CameraPreview";

    private OnSizeChangeCallback onSizeChangeCallback;

    protected GL10 gl;

    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private TexureObserver mObServer;
    // 滤镜控制器
    private FilterWrapper mFilterWrapper;
    private MagicCameraInputFilter mCameraInputFilter;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setObServer(TexureObserver observer){
        mObServer = observer;
    }

    protected void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GlUtil.checkGlError("glGenTextures");
        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
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
        // Tell the ObServer to enable the camera preview.
        mObServer.onSurfaceCreated(mSurfaceTexture);
        // 摄像头采集数据转换器
        mCameraInputFilter = new MagicCameraInputFilter();
        mCameraInputFilter.init();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        mFilterWrapper.onOutpuSizeChanged(width, height);
        mFilterWrapper.onSurfaceSizeChanged(width, height);
        mIncomingWidth = width;
        mIncomingHeight = height;
        mCameraInputFilter.onOutputSizeChanged(mIncomingWidth, mIncomingHeight);
        mCameraInputFilter.initCameraFrameBuffer(mIncomingWidth, mIncomingHeight);
        mObServer.onSurfaceChanged(gl,width,height);
    }

    float[] mtx = new float[16];

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
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mtx);
        // 转换摄像头数据并且绘制
        mCameraInputFilter.setTextureTransformMatrix(mtx);
        mCameraInputFilter.onDraw(mTextureId);
        // 滤镜绘制
        int textureID = mCameraInputFilter.onDrawToTexture(mTextureId, mIncomingWidth, mIncomingHeight);
        mFilterWrapper.drawFrame(textureID, mIncomingWidth, mIncomingHeight);
        mObServer.onDrawFrame(mSurfaceTexture,textureID);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (onSizeChangeCallback != null) {
            onSizeChangeCallback.onSizeChange(w, h, oldw, oldh);
        }
    }

    public void setFilterWrapper(FilterWrapper filterWrapper) {
        this.mFilterWrapper = filterWrapper;
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
    }
}
