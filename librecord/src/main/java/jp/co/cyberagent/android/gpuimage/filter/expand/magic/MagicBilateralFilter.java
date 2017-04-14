/**
 * @author wysaid
 * @mail admin@wysaid.org
 *
*/

package jp.co.cyberagent.android.gpuimage.filter.expand.magic;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterContext;
import jp.co.cyberagent.android.gpuimage.util.MagicFilterParam;
import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import android.content.Context;
import android.opengl.GLES20;

import com.blue.librecord.R;


public class MagicBilateralFilter extends GPUImageFilterContext {
	@Override
	public String toString() {
		return "Bilateral";
	}
	
	private float mDistanceNormalizationFactor = 4.0f;
	private int mDisFactorLocation;
	private int mSingleStepOffsetLocation;
	
	public MagicBilateralFilter(Context context) {
		super(context, 
				MagicFilterParam.mGPUPower == 1 ? 
					OpenGLUtils.readShaderFromRawResource(context, R.raw.bilateralfilter):
					OpenGLUtils.readShaderFromRawResource(context, R.raw.bilateralfilter_low));
	}
	
	@Override
	public void onInit() {
		super.onInit();
		mDisFactorLocation = GLES20.glGetUniformLocation(getProgram(), "distanceNormalizationFactor");
		mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
	}
	
	@Override
	public void onInitialized() {
		super.onInitialized();
		setDistanceNormalizationFactor(mDistanceNormalizationFactor);
	}
	
	public void setDistanceNormalizationFactor(final float newValue) {
		mDistanceNormalizationFactor = newValue;
		setFloat(mDisFactorLocation, newValue);
	}
	
	private void setTexelSize(final float w, final float h) {
		setFloatVec2(mSingleStepOffsetLocation, new float[] {2.0f / w, 2.0f / h});
	}
	
	@Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        setTexelSize(width, height);
    }
}
