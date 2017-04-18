/*
package jp.co.cyberagent.android.gpuimage.filter;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSourceOverBlendFilter;


*/
/**
 * Created by se7en on 2017/4/16.
 *//*


public class ProgressFilter extends GPUImageSourceOverBlendFilter {
    private Bitmap mBitmap;
    private Canvas mCanvas;

    private long mLastDrawingTime;
    private long mDrawingPeriod;
    */
/**
     * The duration that the paths completely shown in seconds
     *//*

    private static  final long DURATION = 1000;
    public ProgressFilter(){
        super();
    }



    public void setProgress(float progress){

    }

    @Override
    protected void runPendingOnDrawTasks() {
        super.runPendingOnDrawTasks();
        if (mBitmap == null){
            mBitmap = Bitmap.createBitmap(mOutputWidth,20, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mDrawingPeriod = DURATION/mPathPainter.getFrameCount();
        }
        if (System.currentTimeMillis() - mLastDrawingTime < mDrawingPeriod) return;
        mLastDrawingTime = System.currentTimeMillis();



        if (mPathPainter.drawNext(mCanvas)){
            setBitmap(mBitmap,true);
        }
    }

}
*/
