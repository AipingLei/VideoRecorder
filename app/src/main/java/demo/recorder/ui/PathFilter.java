package demo.recorder.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSourceOverBlendFilter;


/**
 * Created by se7en on 2017/4/16.
 */

public class PathFilter extends GPUImageSourceOverBlendFilter {
    private final Bitmap mBitmap;
    private Canvas mCanvas;
    Paint mPaint;

    long mLastDrawingTime;
    final long mDrawingPeriod;
    PathPainter mPathPainter;
    /**
     * The duration that the paths completely shown in seconds
     */
    private static  final long DURATION = 5000;
    public PathFilter(){
        super();
        mBitmap = Bitmap.createBitmap(480,480, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mPathPainter = PathPainter.create();
        mDrawingPeriod = DURATION/mPathPainter.getFrameCount();
    }

    @Override
    protected void runPendingOnDrawTasks() {
        super.runPendingOnDrawTasks();
        if (System.currentTimeMillis() - mLastDrawingTime < mDrawingPeriod) return;
        if (mPathPainter.drawNext(mCanvas)){
            mLastDrawingTime = System.currentTimeMillis();
            setBitmap(mBitmap,true);
        }
    }
}
