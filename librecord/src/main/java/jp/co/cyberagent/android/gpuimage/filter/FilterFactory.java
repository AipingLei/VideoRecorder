package jp.co.cyberagent.android.gpuimage.filter;


import android.content.Context;

import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSwirlFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicRiseFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicRomanceFilter;


public class FilterFactory {

    public static GPUImageFilter getFilter(GPUFilterType filterType) {
        switch (filterType) {
            case FILTER_NONE:
                return new GPUImageFilter();
            case FILTER_GPUIMAGE_SWIRL:
                return  new GPUImageSwirlFilter();
            case FILTER_GPUIMAGE_MASK:
                return  new PathFilter();
            default:
                return  new GPUImageFilter();
        }
    }

    public static GPUImageFilter getFilter(GPUFilterType filterType,Context aContext) {
        switch (filterType) {
            case FILTER_MAGIC_RISE:
                return  new MagicRiseFilter(aContext);
            case FILTER_MAGIC_ROMANCE:
                return  new MagicRomanceFilter(aContext);
            default:
                return  new GPUImageFilter();
        }
    }
}
