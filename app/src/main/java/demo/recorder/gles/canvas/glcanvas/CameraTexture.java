package demo.recorder.gles.canvas.glcanvas;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import javax.microedition.khronos.opengles.GL11;

/**
 * description: 在Android中Camera产生的preview texture是以一种特殊的格式传送的，
 * 因此shader里的纹理类型并不是普通的sampler2D,而是samplerExternalOES, 在shader
 * 的头部也必须声明OES 的扩展。除此之外，external OES的纹理和Sampler2D在使用时没有
 * 差别。
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
*/

public class CameraTexture extends BasicTexture{
    private static final String TAG = "CameraTexture";

    private final boolean mOpaque;
    private boolean mIsFlipped = false;
    private int target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

    public CameraTexture(int width, int height, boolean opaque) {
        this(width, height, opaque, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    }

    public CameraTexture(int width, int height, boolean opaque, int target) {
        mOpaque = opaque;
        setSize(width, height);
        this.target = target;
    }


    @Override
    public boolean isOpaque() {
        return mOpaque;
    }

    @Override
    public boolean isFlippedVertically() {
        return mIsFlipped;
    }

    public void setIsFlippedVertically(boolean isFlipped) {
        mIsFlipped = isFlipped;
    }

    public void prepare(IGLCanvas canvas) {
        GLId glId = canvas.getGLId();
        mId = glId.generateTexture();

        if (target == GLES20.GL_TEXTURE_2D) {
            canvas.initializeTextureSize(this, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        }
        canvas.setTextureParameters(this);
        mState = STATE_LOADED;
        setAssociatedCanvas(canvas);
    }

    @Override
    protected boolean onBind(IGLCanvas canvas) {
        if (isLoaded()) return true;
        Log.w(TAG, "lost the content due to context change");
        return false;
    }

    @Override
     public void yield() {
         // we cannot free the secondBitmap because we have no backup.
     }

    @Override
    public int getTarget() {
        return target;
    }
}
