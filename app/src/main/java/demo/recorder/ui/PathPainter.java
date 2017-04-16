package demo.recorder.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.Random;

/**
 * Created by se7en on 2017/4/16.
 * 2 dimension path drawer
 */

public class PathPainter {

    private static final int PATH_COLOR = Color.WHITE;

    private final float PATH_PAINT_SIZE = 3.0f;

    private final float START_END_PAINT_SIZE = 4.0f;

    private static final int START_END_FRAME_COUNT = 10;

    private static final float START_END_RADIUS_MAX = 20.0f;

    private static final float START_END_RADIAS_MIN = 5.0f;

    private final float[][] mPathData ;

    private final Path mPath;

    private final Paint mPathPaint;

    private final Paint mStartEndPaint;

    private int mCurrentFrameIndex;

    private int mCurrentPathIndex = 0;

    private int mFrameCount;

    //start and end point radius fator
    private float mRadiusFator;

    public PathPainter(float[][] aPaths){
        this.mPathData = aPaths;
        mPath = new Path();
        mPathPaint = getPathPaint();
        mStartEndPaint = getStartEndPaint();

    }

    private void init(){
        mFrameCount = mPathData.length + START_END_FRAME_COUNT*2;
        mRadiusFator =  START_END_RADIUS_MAX/(START_END_RADIAS_MIN*START_END_FRAME_COUNT);
    }

    public boolean drawNext(Canvas aCanvas){
        if (mCurrentFrameIndex > mFrameCount-1 ) return false;

        float x =  mPathData[mCurrentPathIndex][0];
        float y = mPathData[mCurrentPathIndex][1];
        if (mCurrentFrameIndex == 0) mPath.moveTo(x,y);
        if (mCurrentPathIndex == 0 || mCurrentPathIndex == mPathData.length-1){
            drawStartEnd(aCanvas,x,y);
            return  true;
        }
        drawPath(aCanvas,x,y);
        return  true;
    }

    private void drawPath(Canvas aCanvas,float x,float y) {
        float previousX = mPathData[mCurrentPathIndex-1][0];
        float previousY = mPathData[mCurrentPathIndex-1][1];
        float cX = (x + previousX) / 2;
        float cY = (y +  previousY) / 2;
        //二次贝塞尔，实现平滑曲线；previousX, previousY为操作点，cX, cY为终点
        mPath.quadTo(previousX, previousY, cX, cY);
        aCanvas.drawPath(mPath,mPathPaint);
        mCurrentFrameIndex++;
    }

    private void drawStartEnd(Canvas aCanvas,float x,float y){
        int sFrameIndex = mCurrentFrameIndex < START_END_FRAME_COUNT ? mCurrentFrameIndex :
                START_END_FRAME_COUNT-(mFrameCount - mCurrentPathIndex);
        if (sFrameIndex > START_END_FRAME_COUNT) return;


        float sPointSize = sFrameIndex*mRadiusFator*START_END_PAINT_SIZE;

        mStartEndPaint.setTextSize(sPointSize);
        float radius = (sFrameIndex+1)*START_END_RADIUS_MAX*mRadiusFator;
        aCanvas.drawCircle(x,y,radius,mStartEndPaint);

        //mStartEndPaint.setTextSize(sPointSize*1.2f);
        mStartEndPaint.setStyle(Paint.Style.FILL);
        aCanvas.drawCircle(x,y,radius/5,mStartEndPaint);
        mCurrentFrameIndex++;
        if (sFrameIndex == START_END_FRAME_COUNT-1 && mCurrentPathIndex==0){
            mCurrentPathIndex++;
        }
    }


    private Paint getPathPaint(){
        Paint sPaint = new Paint();
        sPaint.setAntiAlias(true);
        sPaint.setColor(PATH_COLOR);
        sPaint.setTextSize(PATH_PAINT_SIZE);
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
        float[][] aPaths = new float[80][2];
        aPaths[39][0] = Integer.MIN_VALUE;
        int index = 1;
        while ( aPaths[39][0] == Integer.MIN_VALUE){
            aPaths[index][0] = aPaths[index][0]+ new Random().nextFloat()*(4f);
            aPaths[index][1] = aPaths[index][1]+ new Random().nextFloat()*(4f);
            index++;
        }
        return  new PathPainter(aPaths);
    }

    public int getFrameCount() {
        return  mFrameCount;
    }
}
