package demo.recorder.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import demo.recorder.R;


/**
 * 带比例的RelativeLayout
 */
public class RateRelativeLayout extends RelativeLayout {
    // 宽高比例，1为正方形
    private float rate = 0;

    public RateRelativeLayout(Context context) {
        super(context);
    }

    public RateRelativeLayout(Context paramContext, AttributeSet paramAttributeSet) {
        super(paramContext, paramAttributeSet);
        if (paramAttributeSet != null) {
            TypedArray a = paramContext.obtainStyledAttributes(
                    paramAttributeSet, R.styleable.RoundImageView);
            float r = a.getFloat(R.styleable.RoundImageView_rate, -1);
            if (r != -1) {
                rate = r;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (rate == 1)
            heightMeasureSpec = widthMeasureSpec;
        else if (rate > 0) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (w / rate), widthMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setRate(float rate) {
        this.rate = rate;
    }
}