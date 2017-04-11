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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import demo.recorder.R;
import demo.recorder.gles.canvas.glcanvas.GLPaint;
import demo.recorder.gles.canvas.glview.GLView;


/**
 * Created by Matthew on 2016/10/5.
 */

public class CompareGLView extends GLView {

    private Bitmap baboon;
    Bitmap textBitmap;
    Canvas normalCanvas;


    public CompareGLView(Context context) {
        super(context);
    }

    public CompareGLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        normalCanvas = new Canvas(textBitmap);
    }

    @Override
    protected void init() {
        super.init();
        baboon = BitmapFactory.decodeResource(getResources(), R.drawable.baboon);
    }

    @Override
    protected void onGLDraw(ICanvasGL canvas) {


        CanvasGL.BitmapMatrix matrix = new CanvasGL.BitmapMatrix();
        matrix.postScale(2.1f, 2.1f);
        matrix.postRotate(90);
        matrix.postTranslate(90, 120);
        matrix.postScale(0.4f, 0.4f, 140, 150);
        matrix.postRotate(10, 128 , 128);
        matrix.postTranslate(90, -120);
        canvas.drawBitmap(baboon, matrix);


        GLPaint paint = new GLPaint();
        paint.setColor(Color.parseColor("#88FF0000"));
        paint.setLineWidth(4);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(360, 0, 380, 40, paint);

        GLPaint paint2 = new GLPaint();
        paint2.setColor(Color.parseColor("#8800FF00"));
        paint2.setLineWidth(4);
        paint2.setStyle(Paint.Style.STROKE);
        canvas.drawRect(360, 40, 380, 80, paint2);

        canvas.drawLine(360, 80, 360, 120, paint);
        // text
        //Bitmap textBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        normalCanvas = new Canvas(textBitmap);
        String text = "text";
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(32);
        normalCanvas.drawColor(Color.WHITE);
        normalCanvas.drawText(text+System.currentTimeMillis(), 100, 100, textPaint);
        canvas.drawBitmap(textBitmap, 500, 80);


        //circle
        GLPaint circlePaint = new GLPaint();
        circlePaint.setColor(Color.parseColor("#88FF0000"));
        circlePaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(430, 30, 30, circlePaint);

        GLPaint strokeCirclePaint = new GLPaint();
        strokeCirclePaint.setColor(Color.parseColor("#88FF0000"));
        strokeCirclePaint.setLineWidth(4);
        strokeCirclePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(490, 30, 30, strokeCirclePaint);
    }
}
