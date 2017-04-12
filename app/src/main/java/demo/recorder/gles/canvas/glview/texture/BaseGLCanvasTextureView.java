package demo.recorder.gles.canvas.glview.texture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;

import demo.recorder.gles.canvas.CanvasGL;
import demo.recorder.gles.canvas.ICanvasGL;
import demo.recorder.gles.canvas.Loggers;
import demo.recorder.gles.canvas.OpenGLUtil;
import demo.recorder.gles.canvas.glview.GLView;


/**
 * Created by Chilling on 2016/11/11.
 */

abstract class BaseGLCanvasTextureView extends BaseGLTextureView implements GLViewRenderer {


    protected ICanvasGL mCanvas;
    private int backgroundColor = Color.TRANSPARENT;

    public BaseGLCanvasTextureView(Context context) {
        super(context);
    }

    public BaseGLCanvasTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseGLCanvasTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        super.init();
        setRenderer(this);
    }

    @Override
    public void onSurfaceCreated() {
        Loggers.d("BaseGLCanvasTextureView", "onSurfaceCreated: ");
        mCanvas = new CanvasGL();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Loggers.d("BaseGLCanvasTextureView", "onSurfaceChanged: ");
        mCanvas.setSize(width, height);

    }

    @Override
    public void onDrawFrame() {
        mCanvas.clearBuffer(backgroundColor);
        onGLDraw(mCanvas);
    }

    protected abstract void onGLDraw(ICanvasGL canvas);


    /**
     * If setOpaque(false) used, this method will not work.
     */
    public void setRenderBackgroundColor(@ColorInt int color) {
        this.backgroundColor = color;
    }


    public void getDrawingBitmap(final Rect rect, final GLView.GetDrawingCacheCallback getDrawingCacheCallback) {

        queueEvent(new Runnable() {
            @Override
            public void run() {
                onDrawFrame();
                onDrawFrame();
                final Bitmap bitmapFromGLSurface = OpenGLUtil.createBitmapFromGLSurface(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top, getHeight());

                post(new Runnable() {
                    @Override
                    public void run() {
                        getDrawingCacheCallback.onFetch(bitmapFromGLSurface);
                    }
                });
            }
        });
        requestRender();
    }
}
