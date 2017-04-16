package com.blue.librecord.recorder.video;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;

import java.util.Random;

/**
 * Created by se7en on 2017/4/16.
 * 2 dimension path drawer
 */

public class PathPainter {

    private static final int PATH_COLOR = Color.WHITE;

    private final float PATH_PAINT_SIZE = 8.0f;

    private final float START_END_PAINT_SIZE = 10.0f;

    private static final int START_END_FRAME_COUNT = 6;

    private static final float START_END_RADIUS_MAX = 25.0f;

    private final float[][] mPathData ;

    private final Path mPath;

    private final Paint mPathPaint;

    private final Paint mStartEndPaint;

    private int mCurrentFrameCount;

    private int mCurrentPathIndex = 0;

    private int mFrameCount;

    //start and end point radius fator
    private float mRadiusFator;

    private Xfermode normalMode;
    private Xfermode clearMode;
    public PathPainter(float[][] aPaths){
        this.mPathData = aPaths;
        mPath = new Path();
        mPathPaint = getPathPaint();
        mStartEndPaint = getStartEndPaint();
        mFrameCount = mPathData.length + START_END_FRAME_COUNT*2;
        mRadiusFator =  1.0f/START_END_FRAME_COUNT;
        normalMode = mStartEndPaint.getXfermode();
        clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
    }


    public boolean drawNext(Canvas aCanvas){
        if (mCurrentFrameCount > mFrameCount-1 ) return false;
        int sIndex = mCurrentPathIndex == mPathData.length ? mCurrentPathIndex-1:mCurrentPathIndex;
        float x =  mPathData[sIndex][0];
        float y = mPathData[sIndex][1];
        if (mCurrentFrameCount == 0) mPath.moveTo(x,y);
        if (mCurrentPathIndex == 0 || mCurrentPathIndex == mPathData.length){
            drawStartEnd(aCanvas,mPathData[sIndex][0], mPathData[sIndex][1]);
            return  true;
        }
        drawPath(aCanvas,mPathData[sIndex][0],mPathData[sIndex][1]);
        return  true;
    }

    private void drawPath(Canvas aCanvas,float x,float y) {
        float previousX = mPathData[mCurrentPathIndex-1][0];
        float previousY = mPathData[mCurrentPathIndex-1][1];
        float cX = (x + previousX) / 2;
        float cY = (y +  previousY) / 2;
        mPath.quadTo(cX, cY, x, y);
        aCanvas.drawPath(mPath,mPathPaint);
        mCurrentFrameCount++;
        mCurrentPathIndex++;
    }

    private void drawStartEnd(Canvas aCanvas,float x,float y){

        int sFrameIndex = mCurrentFrameCount < START_END_FRAME_COUNT ? mCurrentFrameCount :
                START_END_FRAME_COUNT-(mFrameCount - mCurrentFrameCount-1);
        if (sFrameIndex > START_END_FRAME_COUNT - 1) return;
        //clear
        mStartEndPaint.setXfermode(clearMode);
        aCanvas.drawCircle(x,y,START_END_RADIUS_MAX,mStartEndPaint);

        mStartEndPaint.setXfermode(normalMode);
        float sPointSize = (sFrameIndex+1)*mRadiusFator*START_END_PAINT_SIZE;
        mStartEndPaint.setStyle(Paint.Style.STROKE);
        mStartEndPaint.setStrokeWidth(sPointSize);
        float radius = (sFrameIndex+1)*START_END_RADIUS_MAX*mRadiusFator;
        aCanvas.drawCircle(x,y,radius,mStartEndPaint);
        mStartEndPaint.setStyle(Paint.Style.FILL);
        aCanvas.drawCircle(x,y,radius/3.0f,mStartEndPaint);
        mCurrentFrameCount++;
        if (mCurrentFrameCount == START_END_FRAME_COUNT&&mCurrentPathIndex==0){
            mCurrentPathIndex++;
        }
    }


    private Paint getPathPaint(){
        Paint sPaint = new Paint();
        sPaint.setAntiAlias(true);
        sPaint.setStyle(Paint.Style.STROKE);
        sPaint.setColor(PATH_COLOR);
        sPaint.setStrokeWidth(PATH_PAINT_SIZE);
        return sPaint;
    }

    private Paint getStartEndPaint(){
        Paint sPaint = new Paint();
        sPaint.setAntiAlias(true);
        sPaint.setColor(PATH_COLOR);
        sPaint.setTextSize(START_END_PAINT_SIZE);
        return sPaint;
    }

    public static PathPainter create() {
        float[][] aPaths = new float[20][2];
        aPaths[19][0] = Integer.MIN_VALUE;
        aPaths[0][0] = 100.0f;
        aPaths[0][1] = 100.0f;
        int index = 1;
        while ( aPaths[19][0] == Integer.MIN_VALUE){
            aPaths[index][0] = aPaths[index-1][0]+ new Random().nextInt(50)*(1f);
            aPaths[index][1] = aPaths[index-1][1]+ new Random().nextInt(50)*(1f);
            index++;
        }
        return  new PathPainter(aPaths);
    }

    public int getFrameCount() {
        return  mFrameCount;
    }
}
