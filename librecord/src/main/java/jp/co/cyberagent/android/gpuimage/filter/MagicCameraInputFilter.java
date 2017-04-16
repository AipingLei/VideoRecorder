package jp.co.cyberagent.android.gpuimage.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

/**
 * 摄像头数据转换filter，android摄像头默认采集的数据是 GL_TEXTURE_EXTERNAL_OES，此类将数据输出为GL_TEXTURE_2D
 * @author sulei
 */
public class MagicCameraInputFilter extends GPUImageFilter{
	private static final String CAMERA_INPUT_VERTEX_SHADER = ""+
			"attribute vec4 position;\n" +    
            "attribute vec4 inputTextureCoordinate;\n" +    
            "\n" +  
            "uniform mat4 textureTransform;\n" +    
            "varying vec2 textureCoordinate;\n" +    
            "\n" +  
            "void main()\n" +    
            "{\n" +    
            "	textureCoordinate = (textureTransform * inputTextureCoordinate).xy;\n" +    
            "	gl_Position = position;\n" +    
            "}";  
	
	private static final String CAMERA_INPUT_FRAGMENT_SHADER = ""+
			"#extension GL_OES_EGL_image_external : require\n" +    
			"varying highp vec2 textureCoordinate;\n" +    
			"\n" +  
			"uniform samplerExternalOES inputImageTexture;\n" +    
			"\n" +  
			"void main()\n" +    
			"{\n" +    
			"	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +    
			"}";
	
	private float[] mTextureTransformMatrix;
    private int mTextureTransformMatrixLocation;
    
    protected static int[] mFrameBuffers = null;
    protected static int[] mFrameBufferTextures = null;
    private int mFrameWidth = -1;
    private int mFrameHeight = -1;  
    
	public MagicCameraInputFilter(){
		super(CAMERA_INPUT_VERTEX_SHADER, CAMERA_INPUT_FRAGMENT_SHADER);
	}
	
	public void onInit() {
        super.onInit();
        mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgId, "textureTransform");
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
    }
	
	public void setTextureTransformMatrix(float[] mtx){
		mTextureTransformMatrix = mtx;
    }
	
	public void onDraw(int textureId) {
        GLES20.glUseProgram(mGLProgId);
        if(!isInitialized()) {
            return;
        }
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);
        
        if(textureId != OpenGLUtils.NO_TEXTURE){
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
	        GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }
	
	@Override
	public void onDraw(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        if(!isInitialized()) {
            return;
        }
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);
        
        if(textureId != OpenGLUtils.NO_TEXTURE){
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
	        GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
    }
	
	/**
	 * 顶点坐标
	 */
	protected  FloatBuffer mGLCubeBuffer;
	
	/**
	 * 纹理坐标
	 */
	protected  FloatBuffer mGLTextureBuffer;
	
	/**
	 * 将GL_TEXTURE_EXTERNAL_OES的数据绘制到GL_TEXTURE_2D并输出
	 * @param textureId
	 * @param surfaceWidth
	 * @param surfaceHeight
	 * @return
	 */
	public int onDrawToTexture(final int textureId, final int surfaceWidth, final int surfaceHeight) {
		if(mFrameBuffers == null)
			return OpenGLUtils.NO_TEXTURE;
		GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);
    	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
    	GLES20.glUseProgram(mGLProgId);
        if(!isInitialized()) {
            return OpenGLUtils.NO_TEXTURE;
        }
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);
        
        if(textureId != OpenGLUtils.NO_TEXTURE){
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
	        GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
		return mFrameBufferTextures[0];
	}

	public void initCameraFrameBuffer(int width, int height) {
		if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
			destroyFramebuffers();
        if (mFrameBuffers == null) {
        	mFrameWidth = width;
			mFrameHeight = height;
        	mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];

            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
            
            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
	}	
	
	public void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }
}
