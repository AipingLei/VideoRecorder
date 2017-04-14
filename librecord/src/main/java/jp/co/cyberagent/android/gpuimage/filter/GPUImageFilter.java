/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.android.gpuimage.filter;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PointF;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;

/**
 * 
 * MagicFairytaleFilter 童话
 * MagicSunriseFilter 日出
 * MagicSunsetFilter 日落
 * MagicWhiteCatFilter 白猫
 * MagicBlackCatFilter 黑猫
 * MagicBeautyFilter 美肤
 * MagicSkinWhitenFilter 美白,与MagicBilateralFilter滤镜同时使用,效果更好,不过性能消耗较大
 * MagicHealthyFilter 健康，效率低
 * MagicSweetsFilter 甜品
 * MagicRomanceFilter 浪漫
 * MagicSakuraFilter 樱花
 * MagicWarmFilter 温暖
 * MagicAntiqueFilter 复古
 * MagicNostalgiaFilter 怀旧
 * MagicCalmFilter 平静
 * MagicLatteFilter 拿铁
 * MagicCoolFilter 冰冷
 * MagicEmeraldFilter 祖母绿
 * MagicEvergreenFilter 常青
 * MagicCrayonFilter 蜡笔，效率极低
 * MagicSketchFilter 素描，效率极低,使用GPUImageSketchFilter替代
 * MagicAmaroFilter Amaro
 * MagicBrannanFilter Brannan
 * MagicBrooklynFilter Brooklyn
 * MagicEarlyBirdFilter EarlyBird 晨起
 * MagicFreudFilter Freud
 * MagicHefeFilter Hefe
 * MagicHudsonFilter Hudson
 * MagicInkwellFilter Inkwell 黑白
 * MagicKevinFilter Kevin
 * MagicLomoFilter Lomo
 * MagicN1977Filter 1977复古
 * MagicNashvilleFilter Nashville
 * MagicPixarFilter Pixar
 * MagicRiseFilter Rise 日出
 * MagicSierraFilter Sierra
 * MagicSutroFilter Sutro
 * MagicToasterFilter Toaster
 * MagicWaldenFilter Walden
 * 
 * GPUImageColorInvertFilter 反色
 * GPUImageVignetteFilter 四周暗色
 * GPUImageSketchFilter 素描
 * GPUImageToneCurveFilter 亮白，有类似美白效果
 * GPUImageNormalBlendFilter、GPUImageSourceOverBlendFilter 覆盖一张图片，普通叠加模式
 * GPUImageAlphaBlendFilter  覆盖一张图片，图片半透明
 * GPUImageGaussianBlurFilter 高斯模糊
 * GPUImageGlassSphereFilter 透过玻璃球效果，类似猫眼、鱼眼效果
 * GPUImageLaplacianFilter 拉普拉斯矩阵，类似雕塑效果
 * GPUImageSwirlFilter 漩涡扭曲效果
 * 
 */
@SuppressLint("WrongCall")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public class GPUImageFilter implements Cloneable
{
	@Override
	public String toString() {
		return "GPUImageFilter";
	}

	public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    private LinkedList<Runnable> mRunOnDraw;
    private String mFragmentShader;
    private final String mVertexShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mGLStrengthLocation;
    protected int mOutputWidth;
    protected int mOutputHeight;
    private boolean mIsInitialized;
    protected int mSurfaceWidth, mSurfaceHeight;
    
    public void onSurfaceSizeChanged(int surfaceWidth, int surfaceHeight)
    {
    	this.mSurfaceWidth = surfaceWidth;
    	this.mSurfaceHeight = surfaceHeight;
    }
    
    private int muTexMatrixLoc;

    private static final boolean IS_USE_EXTERNAL_OES = false;
    protected int mTextureTarget = GLES20.GL_TEXTURE_2D;
    private boolean isSettedExternalOES = false;
    
    protected void useExternalOES(boolean useOES)
    {
    	if(isSettedExternalOES)
    	{
    		return;
    	}
    	isSettedExternalOES = true;
    	
    	if(useOES)
    	{
    		StringBuffer sb = new StringBuffer();
    		sb.append("#extension GL_OES_EGL_image_external : require");
    		sb.append("\n");
    		sb.append(mFragmentShader.replaceFirst("sampler2D", "samplerExternalOES"));
    		mFragmentShader = sb.toString();
    		this.mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    	}
    	else
    	{
    		this.mTextureTarget = GLES20.GL_TEXTURE_2D;
    	}
    }
    
    public GPUImageFilter() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public GPUImageFilter(final String vertexShader, final String fragmentShader) {
        mRunOnDraw = new LinkedList<Runnable>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        useExternalOES(IS_USE_EXTERNAL_OES);
    }

    public final void init() {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    public void onInit() {
        mGLProgId = OpenGLUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        
        muTexMatrixLoc = GLES20.glGetUniformLocation(mGLProgId, "uTexMatrix");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,
                "inputTextureCoordinate");
        mGLStrengthLocation = GLES20.glGetUniformLocation(mGLProgId,
                "strength");
        mIsInitialized = true;
    }

    public void onInitialized() {
    	setFloat(mGLStrengthLocation, 1.0f);
    }

    public final void destroy() {
    	if(mIsInitialized)
    	{
    		mIsInitialized = false;
            GLES20.glDeleteProgram(mGLProgId);
            onDestroy();	
    	}
    }

    public void onDestroy() {
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }
    
    public void onDraw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        if (textureId != OpenGLUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glBindTexture(mTextureTarget, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        onDrawArraysAfter();
        GLES20.glBindTexture(mTextureTarget, 0);

        GLES20.glUseProgram(0);
    }

    protected void onDrawArraysPre() {}
    protected void onDrawArraysAfter() {}

    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public int getProgram() {
        return mGLProgId;
    }

    public int getAttribPosition() {
        return mGLAttribPosition;
    }

    public int getAttribTextureCoordinate() {
        return mGLAttribTextureCoordinate;
    }

    public int getUniformTexture() {
        return mGLUniformTexture;
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public static String loadShader(String file, Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream ims = assetManager.open(file);

            String re = convertStreamToString(ims);
            ims.close();
            return re;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
    
    @Override
    public GPUImageFilter clone() 
    {
//    	String json = JSON.toJSONString(this);
//    	
//    	GPUImageFilter obj = JSON.parseObject(json, this.getClass());
//    	obj.init();
//    	obj.onOutputSizeChanged(mOutputWidth, mOutputHeight);
//    	return obj;
//    	try {
//    		GPUImageFilter result = (GPUImageFilter) super.clone();
//    		result.mRunOnDraw = new LinkedList<Runnable>();
//    		result.onOutputSizeChanged(this.mOutputWidth, this.mOutputHeight);
//			return result;
//		} catch (CloneNotSupportedException e) {
//			e.printStackTrace();
//		}
//    	return null;
    	
    	GPUImageFilter result = null;
		try {
			Constructor<?> construstor = this.getClass().getConstructor();
			
			result = (GPUImageFilter) construstor.newInstance();
			result.init();
			result.onOutputSizeChanged(mOutputWidth, mOutputHeight);
			result.onSurfaceSizeChanged(mSurfaceWidth, mSurfaceHeight);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new RuntimeException("no construstor found at " + this.getClass().getName());
		} catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException("construst failed at " + this.getClass().getName());
		}
		
		return result;
    }
    
    
}
