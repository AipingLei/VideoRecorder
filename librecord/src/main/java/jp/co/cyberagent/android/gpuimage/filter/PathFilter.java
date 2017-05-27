package jp.co.cyberagent.android.gpuimage.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSourceOverBlendFilter;


/** 
 * description: describe the class
 * create by: leiap
 * create date: 2017/5/15
 * update date: 2017/5/15
 * version: 1.0
*/

public class PathFilter extends GPUImageSourceOverBlendFilter {
    private Bitmap mBitmap;
    private Canvas mCanvas;

    private long mLastDrawingTime;
    private long mDrawingPeriod;
    PathPainter mPathPainter;
    /**
     * The duration that the paths completely shown in seconds
     */
    private static  final long DURATION = 2000;
    public PathFilter(){
        super();
    }

    @Override
    protected void runPendingOnDrawTasks() {
        super.runPendingOnDrawTasks();
        if (mBitmap == null){
            mBitmap = Bitmap.createBitmap(mOutputWidth,mOutputWidth, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mPathPainter = PathPainter.create();
            mDrawingPeriod = DURATION/mPathPainter.getFrameCount();
        }
        if (System.currentTimeMillis() - mLastDrawingTime < mDrawingPeriod) return;
        mLastDrawingTime = System.currentTimeMillis();
        if (mPathPainter.drawNext(mCanvas)){
            setBitmap(mBitmap,true);
        }
    }

}
