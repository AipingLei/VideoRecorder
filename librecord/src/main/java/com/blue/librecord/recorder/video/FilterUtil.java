package com.blue.librecord.recorder.video;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import jp.co.cyberagent.android.gpuimage.GPUFilterType;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageGlassSphereFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageLaplacianFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSketchFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageSwirlFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageToneCurveFilter;
import jp.co.cyberagent.android.gpuimage.filter.base.GPUImageVignetteFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicAmaroFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicAntiqueFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicBeautyFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicBlackCatFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicBrannanFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicBrooklynFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicCalmFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicCoolFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicEarlyBirdFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicEmeraldFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicEvergreenFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicFairytaleFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicFreudFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicHefeFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicHudsonFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicInkwellFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicKevinFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicLatteFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicLomoFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicN1977Filter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicNashvilleFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicPixarFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicRiseFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicRomanceFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSakuraFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSierraFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSkinWhitenFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSunriseFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSunsetFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSutroFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicSweetsFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicThinFaceFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicToasterFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicWaldenFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicWarmFilter;
import jp.co.cyberagent.android.gpuimage.filter.expand.magic.MagicWhiteCatFilter;

/**
 * Created by blue on 2017/2/16.
 */

public class FilterUtil {

    private Context context;
    // 所有滤镜
    private Map<GPUFilterType, GPUImageFilter> filters;

    public FilterUtil(Context context){
        this.context = context;
        createFilters();
    }

    // 创建所有滤镜
    private void createFilters() {
        if (null == filters) {
            filters = new HashMap<>();
            for (GPUFilterType filterType : GPUFilterType.values()) {
                switch (filterType) {
                    case FILTER_NONE:
                        filters.put(filterType, new GPUImageFilter());
                        break;
                    case FILTER_GPUIMAGE_COLORINVERT:
                        filters.put(filterType, new GPUImageColorInvertFilter());
                        break;
                    case FILTER_GPUIMAGE_GLASSSPHERE:
                        filters.put(filterType, new GPUImageGlassSphereFilter());
                        break;
                    case FILTER_GPUIMAGE_LAPLACIAN:
                        filters.put(filterType, new GPUImageLaplacianFilter());
                        break;
                    case FILTER_GPUIMAGE_SKETCH:
                        filters.put(filterType, new GPUImageSketchFilter());
                        break;
                    case FILTER_GPUIMAGE_TONECURVE:
                        filters.put(filterType, new GPUImageToneCurveFilter(context));
                        break;
                    case FILTER_GPUIMAGE_VIGNETTE:
                        filters.put(filterType, new GPUImageVignetteFilter());
                        break;
                    case FILTER_GPUIMAGE_SWIRL:
                        filters.put(filterType, new GPUImageSwirlFilter());
                        break;
                    case FILTER_MAGIC_AMARO:
                        filters.put(filterType, new MagicAmaroFilter(context));
                        break;
                    case FILTER_MAGIC_ANTIQUE:
                        filters.put(filterType, new MagicAntiqueFilter(context));
                        break;
                    case FILTER_MAGIC_BEAUTY:
                        filters.put(filterType, new MagicBeautyFilter(context));
                        break;
                    case FILTER_MAGIC_BLACKCAT:
                        filters.put(filterType, new MagicBlackCatFilter(context));
                        break;
                    case FILTER_MAGIC_BRANNAN:
                        filters.put(filterType, new MagicBrannanFilter(context));
                        break;
                    case FILTER_MAGIC_BROOKLYN:
                        filters.put(filterType, new MagicBrooklynFilter(context));
                        break;
                    case FILTER_MAGIC_CALM:
                        filters.put(filterType, new MagicCalmFilter(context));
                        break;
                    case FILTER_MAGIC_COOL:
                        filters.put(filterType, new MagicCoolFilter(context));
                        break;
                    case FILTER_MAGIC_EARLYBIRD:
                        filters.put(filterType, new MagicEarlyBirdFilter(context));
                        break;
                    case FILTER_MAGIC_EMERALD:
                        filters.put(filterType, new MagicEmeraldFilter(context));
                        break;
                    case FILTER_MAGIC_EVERGREEN:
                        filters.put(filterType, new MagicEvergreenFilter(context));
                        break;
                    case FILTER_MAGIC_FAIRYTALE:
                        filters.put(filterType, new MagicFairytaleFilter(context));
                        break;
                    case FILTER_MAGIC_FREUD:
                        filters.put(filterType, new MagicFreudFilter(context));
                        break;
                    case FILTER_MAGIC_HEFE:
                        filters.put(filterType, new MagicHefeFilter(context));
                        break;
                    case FILTER_MAGIC_HUDSON:
                        filters.put(filterType, new MagicHudsonFilter(context));
                        break;
                    case FILTER_MAGIC_INKWELL:
                        filters.put(filterType, new MagicInkwellFilter(context));
                        break;
                    case FILTER_MAGIC_KEVIN:
                        filters.put(filterType, new MagicKevinFilter(context));
                        break;
                    case FILTER_MAGIC_LATTE:
                        filters.put(filterType, new MagicLatteFilter(context));
                        break;
                    case FILTER_MAGIC_LOMO:
                        filters.put(filterType, new MagicLomoFilter(context));
                        break;
                    case FILTER_MAGIC_N1977:
                        filters.put(filterType, new MagicN1977Filter(context));
                        break;
                    case FILTER_MAGIC_NASHVILLE:
                        filters.put(filterType, new MagicNashvilleFilter(context));
                        break;
                    case FILTER_MAGIC_PIXAR:
                        filters.put(filterType, new MagicPixarFilter(context));
                        break;
                    case FILTER_MAGIC_RISE:
                        filters.put(filterType, new MagicRiseFilter(context));
                        break;
                    case FILTER_MAGIC_ROMANCE:
                        filters.put(filterType, new MagicRomanceFilter(context));
                        break;
                    case FILTER_MAGIC_SAKURA:
                        filters.put(filterType, new MagicSakuraFilter(context));
                        break;
                    case FILTER_MAGIC_SIERRA:
                        filters.put(filterType, new MagicSierraFilter(context));
                        break;
                    case FILTER_MAGIC_SKINWHITEN:
                        filters.put(filterType, new MagicSkinWhitenFilter(context));
                        break;
                    case FILTER_MAGIC_SUNRISE:
                        filters.put(filterType, new MagicSunriseFilter(context));
                        break;
                    case FILTER_MAGIC_SUNSET:
                        filters.put(filterType, new MagicSunsetFilter(context));
                        break;
                    case FILTER_MAGIC_SUTRO:
                        filters.put(filterType, new MagicSutroFilter(context));
                        break;
                    case FILTER_MAGIC_SWEETS:
                        filters.put(filterType, new MagicSweetsFilter(context));
                        break;
                    case FILTER_MAGIC_TOASTER:
                        filters.put(filterType, new MagicToasterFilter(context));
                        break;
                    case FILTER_MAGIC_WALDEN:
                        filters.put(filterType, new MagicWaldenFilter(context));
                        break;
                    case FILTER_MAGIC_WARM:
                        filters.put(filterType, new MagicWarmFilter(context));
                        break;
                    case FILTER_MAGIC_WHITECAT:
                        filters.put(filterType, new MagicWhiteCatFilter(context));
                        break;
                    case FILTER_GPUIMAGE_THIN_FACE:
                        filters.put(filterType, new MagicThinFaceFilter(context));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public Map<GPUFilterType, GPUImageFilter> getFilters() {
        return filters;
    }
}
