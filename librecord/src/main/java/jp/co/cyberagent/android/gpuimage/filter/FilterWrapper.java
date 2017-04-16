package jp.co.cyberagent.android.gpuimage.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;
import android.annotation.SuppressLint;

@SuppressLint("WrongCall")
public class FilterWrapper {
	private GPUImageFilter mFilter;

	public FilterWrapper(GPUImageFilter aFilter) {
		this.mFilter = aFilter;
		mGLCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mGLCubeBuffer.put(CUBE).position(0);
		mGLTextureBuffer = ByteBuffer
				.allocateDirect(
						TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	public GPUImageFilter getFilter() {
		return this.mFilter;
	}
	
	/**
	 * 设置滤镜
	 * @param filter
	 */
	public void setFilter(GPUImageFilter filter) {
		if(null != filter && this.mFilter != filter) {
			if(this.mFilter != null) {
				this.mFilter.destroy();
			}
			this.mFilter = filter;
			initFilter();
		}
	}

	public void initFilter() {
		this.mFilter.init();
	}

	private FloatBuffer mGLCubeBuffer;
	private FloatBuffer mGLTextureBuffer;
	static final float CUBE[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
			1.0f, };

	private int mOutputWidth;
	private int mOutputHeight;
	private int mImageWidth;
	private int mImageHeight;
	private boolean mFlipHorizontal = false;
	private boolean mFlipVertical = false;
	private Rotation mRotation = Rotation.NORMAL;
	private GPUImage.ScaleType mScaleType = GPUImage.ScaleType.CENTER_CROP;

	public void setRotation(final Rotation rotation) {
		mRotation = rotation;
		adjustImageScaling();
	}

	public void setRotation(final Rotation rotation,
			final boolean flipHorizontal, final boolean flipVertical) {
		mFlipHorizontal = flipHorizontal;
		mFlipVertical = flipVertical;
		setRotation(rotation);
	}

	private float addDistance(float coordinate, float distance) {
		return coordinate == 0.0f ? distance : 1 - distance;
	}

	public void adjustImageScaling() {
		float outputWidth = mOutputWidth;
		float outputHeight = mOutputHeight;
		if (mRotation == Rotation.ROTATION_270
				|| mRotation == Rotation.ROTATION_90) {
			outputWidth = mOutputHeight;
			outputHeight = mOutputWidth;
		}

		float ratio1 = outputWidth / mImageWidth;
		float ratio2 = outputHeight / mImageHeight;
		float ratioMax = Math.max(ratio1, ratio2);
		
		ratioMax = ratioMax*1.4f;
		int imageWidthNew = Math.round(mImageWidth * ratioMax);
		int imageHeightNew = Math.round(mImageHeight * ratioMax);

		float ratioWidth = imageWidthNew / outputWidth;
		float ratioHeight = imageHeightNew / outputHeight;

		float[] cube = CUBE;
		float[] textureCords = TextureRotationUtil.getRotation(mRotation,
				mFlipHorizontal, mFlipVertical);
		if (mScaleType == GPUImage.ScaleType.CENTER_CROP) {
			float distHorizontal = (1 - 1 / ratioWidth) / 2;
			float distVertical = (1 - 1 / ratioHeight) / 2;
			textureCords = new float[] {
					addDistance(textureCords[0], distHorizontal),
					addDistance(textureCords[1], distVertical),
					addDistance(textureCords[2], distHorizontal),
					addDistance(textureCords[3], distVertical),
					addDistance(textureCords[4], distHorizontal),
					addDistance(textureCords[5], distVertical),
					addDistance(textureCords[6], distHorizontal),
					addDistance(textureCords[7], distVertical), };
		} else {
			cube = new float[] { CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
					CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
					CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
					CUBE[6] / ratioHeight, CUBE[7] / ratioWidth, };
		}

		mGLCubeBuffer.clear();
		mGLCubeBuffer.put(cube).position(0);
		mGLTextureBuffer.clear();
		mGLTextureBuffer.put(textureCords).position(0);
	}

	public void onOutpuSizeChanged(int width, int height) {
		this.mOutputWidth = width;
		this.mOutputHeight = height;
		mFilter.onOutputSizeChanged(width, height);
		adjustImageScaling();
	}
	
	public void onSurfaceSizeChanged(int surfaceWidth, int surfaceHeight) {
    	mFilter.onSurfaceSizeChanged(surfaceWidth, surfaceHeight);
    }
	

	@SuppressLint("WrongCall")
	public void drawFrame(int textureId, int width, int height) {
		if (mImageWidth != width) {
			mImageWidth = width;
			mImageHeight = height;
			adjustImageScaling();
		}

		mFilter.onDraw(textureId, mGLCubeBuffer, mGLTextureBuffer);
	}
	public void destory() {
		mFilter.onDestroy();
	}

	public static FilterWrapper buildFilterWrapper(GPUFilterType aFilterType, int aOrientation, boolean aFlipHorizontal, boolean aFlipVertical) {
		FilterWrapper filterWrapper = new FilterWrapper(FilterFactory.getFilter(aFilterType));
		filterWrapper.initFilter();
		switch (aOrientation) {
			case 90:
				filterWrapper.setRotation(Rotation.ROTATION_90, aFlipHorizontal, aFlipVertical);
				break;
			case 180:
				filterWrapper.setRotation(Rotation.ROTATION_180, aFlipHorizontal, aFlipVertical);
				break;
			case 270:
				filterWrapper.setRotation(Rotation.ROTATION_270, aFlipHorizontal, aFlipVertical);
				break;
			default:
				break;
		}
		return  filterWrapper;
	}
	
}
