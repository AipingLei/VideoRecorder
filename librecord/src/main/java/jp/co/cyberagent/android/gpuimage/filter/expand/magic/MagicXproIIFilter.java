package jp.co.cyberagent.android.gpuimage.filter.expand.magic;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterContext;
import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import android.content.Context;
import android.opengl.GLES20;

import com.blue.librecord.R;

public class MagicXproIIFilter extends GPUImageFilterContext{
	private int[] inputTextureHandles = {-1,-1};
	private int[] inputTextureUniformLocations = {-1,-1};
	
	public MagicXproIIFilter(Context context){
		super(context,OpenGLUtils.readShaderFromRawResource(context, R.raw.xproii_filter_shader));
	}
	
	public void onDestroy() {
        super.onDestroy();
        GLES20.glDeleteTextures(inputTextureHandles.length, inputTextureHandles, 0);
        for(int i = 0; i < inputTextureHandles.length; i++)
        	inputTextureHandles[i] = -1;
    }
	
	protected void onDrawArraysAfter(){
		for(int i = 0; i < inputTextureHandles.length
				&& inputTextureHandles[i] != OpenGLUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0+i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		}
	}
	  
	protected void onDrawArraysPre(){
		for(int i = 0; i < inputTextureHandles.length 
				&& inputTextureHandles[i] != OpenGLUtils.NO_TEXTURE; i++){
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0+i);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTextureHandles[i]);
			GLES20.glUniform1i(inputTextureUniformLocations[i], i+1);
		}
	}
	
	public void onInit(){
		super.onInit();
		inputTextureHandles[0] = OpenGLUtils.loadTextureFromR(mContext, R.drawable.xpromap);
		inputTextureHandles[1] = OpenGLUtils.loadTextureFromR(mContext, R.drawable.vignettemap_new);
	}
}
