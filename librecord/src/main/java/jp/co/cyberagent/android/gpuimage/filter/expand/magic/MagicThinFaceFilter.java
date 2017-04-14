package jp.co.cyberagent.android.gpuimage.filter.expand.magic;

import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.util.OpenGLUtils;
import android.content.Context;
import android.graphics.PointF;
import android.opengl.GLES20;

import com.blue.librecord.R;

/**
 * 瘦脸
 * @author sulei
 *
 */
public class MagicThinFaceFilter extends MagicBeautyFilter{

	    private float mAngle; // 拉伸角度
	    private int mAngleLocation;
	    private float mRadius; // 拉伸范围半径
	    private int mRadiusLocation;
	    private PointF mControlPointA; // 控制点1
	    private int mControlPointALocation;
	    private PointF mControlPointB; // 控制点2
	    private int mControlPointBLocation;
	    private float mAspectRatio; // 拉伸强度系数
	    private int mAspectRatioLocation;
	    

	    public MagicThinFaceFilter(Context context) {
	        this(context, 1.0f, 0.06f, new PointF(0.58f, 0.8f), new PointF(0.42f, 0.8f), 2.0f);
	    }

	    public MagicThinFaceFilter(Context context, float radius, float angle, PointF controlPointA, PointF controlPointB, float aspectRatio) {
	        super(context, OpenGLUtils.readShaderFromRawResource(context, R.raw.thin_face));
	    	
	        this.mRadius = radius;
	        this.mAngle = angle;
	        this.mControlPointA = controlPointA;
	        this.mControlPointB = controlPointB;
	        this.mAspectRatio = aspectRatio;
	    }

	    @Override
	    public void onInit() {
	        super.onInit();
	        this.mAngleLocation = GLES20.glGetUniformLocation(getProgram(), "angle");
	        this.mRadiusLocation = GLES20.glGetUniformLocation(getProgram(), "radius");
	        this.mControlPointALocation = GLES20.glGetUniformLocation(getProgram(), "controlPointA");
	        this.mControlPointBLocation = GLES20.glGetUniformLocation(getProgram(), "controlPointB");
	        this.mAspectRatioLocation = GLES20.glGetUniformLocation(getProgram(), "aspectRatio");
	    }

	    @Override
	    public void onInitialized() {
	        super.onInitialized();
	        setRadius(mRadius);
	        setAngle(mAngle);
	        setControlPointA(mControlPointA);
	        setControlPointB(mControlPointB);
	        setAspectRatio(mAspectRatio);
	    }

	    public void setRadius(float radius) {
	        mRadius = radius;
	        setFloat(mRadiusLocation, radius);
	    }

	    public void setAngle(float angle) {
	        mAngle = angle;
	        setFloat(mAngleLocation, angle);
	    }
	    
	    public void setAspectRatio(float aspectRatio)
	    {
	    	mAspectRatio = aspectRatio;
	        setFloat(mAspectRatioLocation, aspectRatio);
	    }

	    public void setControlPointA(PointF controlPointA) {
	        mControlPointA = controlPointA;
	        setPoint(mControlPointALocation, mControlPointA);
	    }
	    
	    public void setControlPointB(PointF controlPointB) {
	        mControlPointB = controlPointB;
	        setPoint(mControlPointBLocation, mControlPointB);
	    }
	    
	    @Override
	    public void onDraw(int textureId, FloatBuffer cubeBuffer,
	    		FloatBuffer textureBuffer) {
	    	super.onDraw(textureId, cubeBuffer, textureBuffer);
	    }
}
