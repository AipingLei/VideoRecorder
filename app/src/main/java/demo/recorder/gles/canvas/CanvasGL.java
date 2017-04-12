/*
 *
 *  *
 *  *  * Copyright (C) 2016 ChillingVan
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package demo.recorder.gles.canvas;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import java.util.Map;
import java.util.WeakHashMap;

import demo.recorder.gles.canvas.glcanvas.BasicTexture;
import demo.recorder.gles.canvas.glcanvas.BitmapTexture;
import demo.recorder.gles.canvas.glcanvas.IGLCanvas;
import demo.recorder.gles.canvas.glcanvas.GLES20Canvas;
import demo.recorder.gles.canvas.glcanvas.GLPaint;
import demo.recorder.gles.canvas.glcanvas.RawTexture;
import demo.recorder.gles.canvas.shapeFilter.BasicDrawShapeFilter;
import demo.recorder.gles.canvas.shapeFilter.DrawCircleFilter;
import demo.recorder.gles.canvas.shapeFilter.DrawShapeFilter;
import demo.recorder.gles.canvas.textureFilter.BasicTextureFilter;
import demo.recorder.gles.canvas.textureFilter.FilterGroup;
import demo.recorder.gles.canvas.textureFilter.TextureFilter;

/**
 * Created by Matthew on 2016/9/27.
 */

public class CanvasGL implements ICanvasGL {

    private Map<Bitmap, BasicTexture> bitmapTextureMap = new WeakHashMap<>();
    protected final IGLCanvas IGLCanvas;
    protected final BasicTextureFilter basicTextureFilter;
    private float[] canvasBackgroundColor;
    private float[] surfaceTextureMatrix = new float[16];
    private int width;
    private int height;
    private BasicDrawShapeFilter basicDrawShapeFilter;
    private DrawCircleFilter drawCircleFilter = new DrawCircleFilter();

    public CanvasGL() {
        this(new GLES20Canvas());
    }

    public CanvasGL(IGLCanvas IGLCanvas) {
        this.IGLCanvas = IGLCanvas;
        IGLCanvas.setOnPreDrawShapeListener(new IGLCanvas.OnPreDrawShapeListener() {
            @Override
            public void onPreDraw(int program, DrawShapeFilter drawShapeFilter) {
                drawShapeFilter.onPreDraw(program, CanvasGL.this);
            }
        });
        IGLCanvas.setOnPreDrawTextureListener(new GLES20Canvas.OnPreDrawTextureListener() {
            @Override
            public void onPreDraw(int textureProgram, BasicTexture texture, TextureFilter textureFilter) {
                textureFilter.onPreDraw(textureProgram, texture, CanvasGL.this);
            }
        });
        basicTextureFilter = new BasicTextureFilter();
        basicDrawShapeFilter = new BasicDrawShapeFilter();
        canvasBackgroundColor = new float[4];
    }

    @Override
    public BitmapTexture bindBitmapToTexture(int whichTexture, Bitmap bitmap) {
        GLES20.glActiveTexture(whichTexture);
        GLES20Canvas.checkError();
        BitmapTexture texture = (BitmapTexture) getTexture(bitmap, null);
        texture.onBind(IGLCanvas);
        GLES20.glBindTexture(texture.getTarget(), texture.getId());
        GLES20Canvas.checkError();
        return texture;
    }

    @Override
    public void beginRenderTarget(RawTexture texture) {
        IGLCanvas.beginRenderTarget(texture);
    }

    @Override
    public void endRenderTarget() {
        IGLCanvas.endRenderTarget();
    }

    public IGLCanvas getIGLCanvas() {
        return IGLCanvas;
    }

    @Override
    public void drawSurfaceTexture(BasicTexture texture, SurfaceTexture surfaceTexture, int left, int top, int right, int bottom) {
        drawSurfaceTexture(texture, surfaceTexture, left, top, right, bottom, basicTextureFilter);
    }

    @Override
    public void drawSurfaceTexture(BasicTexture texture, SurfaceTexture surfaceTexture, int left, int top, int right, int bottom, TextureFilter basicTextureFilter) {
        if (surfaceTexture == null) {
            IGLCanvas.drawTexture(texture, left, top, right - left, bottom - top, basicTextureFilter);
        } else {
            surfaceTexture.getTransformMatrix(surfaceTextureMatrix);
            IGLCanvas.drawTexture(texture, surfaceTextureMatrix, left, top, right - left, bottom - top, basicTextureFilter);
        }
    }


    @Override
    public void drawBitmap(Bitmap bitmap, BitmapMatrix matrix) {
        drawBitmap(bitmap, matrix, basicTextureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, BitmapMatrix matrix, TextureFilter textureFilter) {
        BasicTexture basicTexture = getTexture(bitmap, textureFilter);
        save();
        IGLCanvas.setMatrix(matrix.obtainResultMatrix());
        IGLCanvas.drawTexture(basicTexture, 0, 0, bitmap.getWidth(), bitmap.getHeight(), textureFilter);
        restore();
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst) {
        drawBitmap(bitmap, new RectF(src), dst, basicTextureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        drawBitmap(bitmap, left, top, basicTextureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, TextureFilter textureFilter) {
        BasicTexture basicTexture = getTexture(bitmap, textureFilter);
        IGLCanvas.drawTexture(basicTexture, left, top, bitmap.getWidth(), bitmap.getHeight(), textureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst) {
        drawBitmap(bitmap, src, new RectF(dst));
    }

    @Override
    public void drawBitmap(Bitmap bitmap, RectF src, RectF dst, TextureFilter textureFilter) {
        if (dst == null) {
            throw new NullPointerException();
        }
        BasicTexture basicTexture = getTexture(bitmap, textureFilter);
        IGLCanvas.drawTexture(basicTexture, src, dst, textureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, int width, int height) {
        drawBitmap(bitmap, left, top, width, height, basicTextureFilter);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top, int width, int height, TextureFilter textureFilter) {
        BasicTexture basicTexture = getTexture(bitmap, textureFilter);
        IGLCanvas.drawTexture(basicTexture, left, top, width, height, textureFilter);
    }

    protected BasicTexture getTexture(Bitmap bitmap, @Nullable TextureFilter textureFilter) {
        throwIfCannotDraw(bitmap);

        BasicTexture resultTexture;
        resultTexture = new BitmapTexture(bitmap);
        bitmapTextureMap.put(bitmap, resultTexture);
      /*  if (bitmapTextureMap.containsKey(bitmap)) {
            resultTexture = bitmapTextureMap.get(bitmap);
        } else {
            resultTexture = new BitmapTexture(bitmap);
            bitmapTextureMap.put(bitmap, resultTexture);
        }*/

        if (textureFilter instanceof FilterGroup) {
            FilterGroup filterGroup = (FilterGroup) textureFilter;
            resultTexture = filterGroup.draw(resultTexture, IGLCanvas);
        }

        return resultTexture;
    }

    @Override
    public void drawCircle(float x, float y, float radius, GLPaint paint) {
        if (paint.getStyle() == Paint.Style.FILL) {
            drawCircleFilter.setLineWidth(0.5f);
        } else {
            drawCircleFilter.setLineWidth(paint.getLineWidth() / (2*radius));
        }
        IGLCanvas.drawCircle(x - radius, y - radius, radius, paint, drawCircleFilter);
    }

    @Override
    public void drawLine(float startX, float startY, float stopX, float stopY, GLPaint paint) {
        IGLCanvas.drawLine(startX, startY, stopX, stopY, paint, basicDrawShapeFilter);
    }


    @Override
    public void drawRect(@NonNull RectF rect, GLPaint paint) {
        drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }

    @Override
    public void drawRect(@NonNull Rect r, GLPaint paint) {
        drawRect(r.left, r.top, r.right, r.bottom, paint);
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, GLPaint paint) {
        if (paint.getStyle() == Paint.Style.STROKE) {
            IGLCanvas.drawRect(left, top, right - left, bottom - top, paint, basicDrawShapeFilter);
        } else {
            IGLCanvas.fillRect(left, top, right - left, bottom - top, paint.getColor(), basicDrawShapeFilter);
        }
    }

    @Override
    public void save() {
        IGLCanvas.save();
    }

    /**
     * @param saveFlags {@link IGLCanvas.SAVE_FLAG_ALL}
     *                  {@link IGLCanvas.SAVE_FLAG_ALPHA}
     *                  {@link IGLCanvas.SAVE_FLAG_MATRIX}
     */
    @Override
    public void save(int saveFlags) {
        IGLCanvas.save(saveFlags);
    }

    @Override
    public void restore() {
        IGLCanvas.restore();
    }


    @Override
    public void rotate(float degrees) {
        IGLCanvas.rotate(degrees, 0, 0, 1);
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        IGLCanvas.translate(px, py);
        rotate(degrees);
        IGLCanvas.translate(-px, -py);
    }

    @Override
    public void scale(float sx, float sy) {
        IGLCanvas.scale(sx, sy, 1);
    }

    @Override
    public void scale(float sx, float sy, float px, float py) {
        IGLCanvas.translate(px, py);
        scale(sx, sy);
        IGLCanvas.translate(-px, -py);
    }

    @Override
    public void translate(float dx, float dy) {
        IGLCanvas.translate(dx, dy);
    }

    @Override
    public void clearBuffer() {
        IGLCanvas.clearBuffer();
    }

    @Override
    public void clearBuffer(int color) {
        canvasBackgroundColor[1] = (float) Color.red(color) / 255;
        canvasBackgroundColor[2] = (float) Color.green(color) / 255;
        canvasBackgroundColor[3] = (float) Color.blue(color) / 255;
        canvasBackgroundColor[0] = (float) Color.alpha(color) / 255;
        IGLCanvas.clearBuffer(canvasBackgroundColor);
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        IGLCanvas.setSize(width, height);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        IGLCanvas.setAlpha(alpha/(float)255);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (BasicTexture bitmapTexture : bitmapTextureMap.values()) {
            bitmapTexture.recycle();
        }
    }

    protected void throwIfCannotDraw(Bitmap bitmap) {
        if (bitmap.isRecycled()) {
            throw new RuntimeException("Canvas: trying to use a recycled bitmap " + bitmap);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (!bitmap.isPremultiplied() && bitmap.getConfig() == Bitmap.Config.ARGB_8888 &&
                    bitmap.hasAlpha()) {
                throw new RuntimeException("Canvas: trying to use a non-premultiplied bitmap "
                        + bitmap);
            }
        }
    }

    public static class BitmapMatrix {
        public static final int TRANSLATE_X = 3;
        public static final int TRANSLATE_Y = 7;
        public static final int SCALE_X = 0;
        public static final int SCALE_Y = 5;
        public static final int MATRIX_SIZE = 16;
        public static final int SKEW_X = 4;
        public static final int SKEW_Y = 1;

        private float[] tempMultipyMatrix4 = new float[MATRIX_SIZE];
        private float[] concatResultMatrix4 = new float[MATRIX_SIZE];

        public BitmapMatrix() {
            reset();
        }

        public void reset() {
            Matrix.setIdentityM(tempMultipyMatrix4, 0);
            Matrix.setIdentityM(concatResultMatrix4, 0);
            Matrix.scaleM(concatResultMatrix4, 0, 1, -1, 1);
        }

        public void postTranslate(float dx, float dy) {

            Matrix.setIdentityM(tempMultipyMatrix4, 0);
            tempMultipyMatrix4[TRANSLATE_X] = dx;
            // 前面加负号是因为OPENGL里面的y坐标轴和Android的相反
            tempMultipyMatrix4[TRANSLATE_Y] = -dy;
            concatResultMatrix4 = multiplyMatrix(tempMultipyMatrix4, concatResultMatrix4);
        }

        public void postScale(float sx, float sy, float px, float py) {
            Matrix.setIdentityM(tempMultipyMatrix4, 0);
            tempMultipyMatrix4[SCALE_X] = sx;
            tempMultipyMatrix4[SCALE_Y] = sy;
            tempMultipyMatrix4[TRANSLATE_X] = px - sx * px;

            // 前面加负号是因为OPENGL里面的y坐标轴和Android的相反
            tempMultipyMatrix4[TRANSLATE_Y] = -(py - sy * py);
            concatResultMatrix4 = multiplyMatrix(tempMultipyMatrix4, concatResultMatrix4);
        }

        public void postScale(float sx, float sy) {
            postScale(sx, sy, 0, 0);
        }

        public void postRotate(float degrees, float px, float py) {

            Matrix.setIdentityM(tempMultipyMatrix4, 0);
            float sin = (float) Math.sin(Math.toRadians(degrees));
            float cos = (float) Math.cos(Math.toRadians(degrees));
            tempMultipyMatrix4[SCALE_X] = cos;
            tempMultipyMatrix4[SKEW_X] = -sin;
            tempMultipyMatrix4[SKEW_Y] = sin;
            tempMultipyMatrix4[SCALE_Y] = cos;

            tempMultipyMatrix4[TRANSLATE_X] = py * sin + px * (1 - cos);
            // 前面加负号是因为OPENGL里面的y坐标轴和Android的相反
            tempMultipyMatrix4[TRANSLATE_Y] = -(py * (1 - cos) - px * sin);
            concatResultMatrix4 = multiplyMatrix(tempMultipyMatrix4, concatResultMatrix4);

        }

        public void postRotate(float degrees) {
            postRotate(degrees, 0, 0);
        }

        private float[] multiplyMatrix(float[] lhs, float[] rhs) {
            float[] resultMatrix4 = new float[MATRIX_SIZE];
            for (int i = 0; i < 4; i++) {
                float x = 0;
                float y = 0;
                float z = 0;
                float w = 0;

                for (int j = 0; j < 4; j++) {
                    float e = matrixGet(lhs, i, j);
                    x += matrixGet(rhs, j, 0) * e;
                    y += matrixGet(rhs, j, 1) * e;
                    z += matrixGet(rhs, j, 2) * e;
                    w += matrixGet(rhs, j, 3) * e;
                }

                matrixSet(resultMatrix4, i, 0, x);
                matrixSet(resultMatrix4, i, 1, y);
                matrixSet(resultMatrix4, i, 2, z);
                matrixSet(resultMatrix4, i, 3, w);
            }
            return resultMatrix4;
        }

        private static void matrixSet(float[] m, int x, int y, float val) {
            m[4 * x + y] = val;
        }

        private static float matrixGet(float[] m, int x, int y) {
            return m[4 * x + y];
        }


        public float[] obtainResultMatrix() {
            float[] resultMatrix4 = new float[MATRIX_SIZE];
            Matrix.transposeM(resultMatrix4, 0, concatResultMatrix4, 0);
            return resultMatrix4;
        }

    }
}
