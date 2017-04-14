package jp.co.cyberagent.android.gpuimage;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImage3x3TextureSamplingFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSobelThresholdFilter;

/**
 * Applies sobel edge detection on the image.
 */
public class GPUImageThresholdEdgeDetection extends GPUImageFilterGroup {
    public GPUImageThresholdEdgeDetection() {
        super();
        addFilter(new GPUImageGrayscaleFilter());
        addFilter(new GPUImageSobelThresholdFilter());
    }

    public void setLineSize(final float size) {
        ((GPUImage3x3TextureSamplingFilter) getFilters().get(1)).setLineSize(size);
    }

    public void setThreshold(final float threshold) {
        ((GPUImageSobelThresholdFilter) getFilters().get(1)).setThreshold(threshold);
    }
}
