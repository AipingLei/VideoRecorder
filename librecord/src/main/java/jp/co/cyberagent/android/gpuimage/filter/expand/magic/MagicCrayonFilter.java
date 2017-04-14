package jp.co.cyberagent.android.gpuimage.filter.expand.magic;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterContext;
import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import android.content.Context;
import android.opengl.GLES20;

import com.blue.librecord.R;

/**
 * 蜡笔，效率极低
 * @author sulei
 */
public class MagicCrayonFilter extends GPUImageFilterContext{
	@Override
	public String toString() {
		return "Crayon:蜡笔";
	}
	private int mSingleStepOffsetLocation;
	//1.0 - 5.0
	private int mStrength;
	
	public MagicCrayonFilter(Context context){
		super(context,OpenGLUtils.readShaderFromRawResource(context, R.raw.crayon));
	}
	
	public void onInit() {
        super.onInit();
        mSingleStepOffsetLocation = GLES20.glGetUniformLocation(getProgram(), "singleStepOffset");
        mStrength = GLES20.glGetUniformLocation(getProgram(), "strength");
        setFloat(mStrength, 2.0f);
    }
    
    public void onDestroy() {
        super.onDestroy();
    }
    
    private void setTexelSize(final float w, final float h) {
		setFloatVec2(mSingleStepOffsetLocation, new float[] {1.0f / w, 1.0f / h});
	}
	
	@Override
    public void onOutputSizeChanged(final int width, final int height) {
        super.onOutputSizeChanged(width, height);
        setTexelSize(width, height);
    }
}
