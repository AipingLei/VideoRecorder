/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package demo.recorder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Camera-related utility functions.
 */
public class CameraUtils {
    private static final String TAG = MainActivity.TAG;

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    /**
     * Attempts to find a fixed preview frame rate that matches the desired frame rate.
     * <p>
     * It doesn't seem like there's a great deal of flexibility here.
     * <p>
     * TODO: follow the recipe from http://stackoverflow.com/questions/22639336/#22645327
     *
     * @return The expected frame rate, in thousands of frames per second.
     */
    public static int chooseFixedPreviewFps(Camera.Parameters parms, int desiredThousandFps) {
        List<int[]> supported = parms.getSupportedPreviewFpsRange();

        for (int[] entry : supported) {
            //Log.d(TAG, "entry: " + entry[0] + " - " + entry[1]);
            if ((entry[0] == entry[1]) && (entry[0] == desiredThousandFps)) {
                parms.setPreviewFpsRange(entry[0], entry[1]);
                return entry[0];
            }
        }

        int[] tmp = new int[2];
        parms.getPreviewFpsRange(tmp);
        int guess;
        if (tmp[0] == tmp[1]) {
            guess = tmp[0];
        } else {
            guess = tmp[1] / 2;     // shrug
        }

        Log.d(TAG, "Couldn't find match for " + desiredThousandFps + ", using " + guess);
        return guess;
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    static final int PREVIEW_SIZE_BEST_WIDTH = 720; // 预览最优值
    static final int PREVIEW_SIZE_MAX_WIDTH = 1280; // 预览最大宽度
    static final int PREVIEW_SIZE_MIN_WIDTH = 240; // 预览最小宽度

    /**
     * 根据摄像头支持的所有预览参数
     *
     * @param parameters
     * @return
     */
    public static Camera.Size determineBestPreviewSize(Context aContext,Camera.Parameters parameters,int orientation) {
        return determineBestSize(aContext,parameters.getSupportedPreviewSizes(), PREVIEW_SIZE_BEST_WIDTH, PREVIEW_SIZE_MAX_WIDTH, PREVIEW_SIZE_MIN_WIDTH,orientation);
    }


    public static Camera.Size determineBestSize(Context aContext,List<Camera.Size> sizes, int bestWidth, int maxWidth, int minWidth,int orientation) {
        Camera.Size bestSize = null;
        Camera.Size bestSize2 = null;
        Camera.Size size;
        int numOfSizes = sizes.size();

        WindowManager wm = (WindowManager) aContext.getSystemService(Context.WINDOW_SERVICE);

        // 优先匹配屏幕尺寸比例相同的，如果未匹配上，再匹配尺寸比例最接近的
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

        final int displayOrientation =orientation;

        // 得到当前屏幕的宽高比
        float scale;
        switch (displayOrientation) {
            case 90:
            case 270: {
                scale = (float) height / width;
            }
            break;
            default: {
                scale = (float) width / height;
            }
            break;
        }

        // 对摄像头支持分辨率参数进行排序
        // 先按宽从大到小排
        // 宽相同的情况下，按照高从大到小排
        // 宽和高都相同的情况下，按照数字从大到小排
        Collections.sort(sizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width != rhs.width) {
                    return lhs.width - rhs.width;
                } else if (lhs.height != rhs.height) {
                    return lhs.height - rhs.height;
                } else {
                    return (lhs.width + lhs.height) - (rhs.width + rhs.height);
                }
            }
        });

        int sizeWidth;
        int sizeHeight;
        // 默认情况下摄像头返回的宽高是相反
        // 300x400 宽返回400 高返回300
        for (int i = 0; i < numOfSizes; i++) {
            size = sizes.get(i);
            switch (displayOrientation) {
                case 90:
                case 270: {
                    sizeWidth = size.width;
                    sizeHeight = size.height;
                }
                break;
                default:
                    sizeWidth = size.height;
                    sizeHeight = size.width;
                    break;
            }

            // 摄像头的宽高比和当前屏幕的宽高比差距小于0.01，近似相等
            boolean isDesireRatio = Math.abs(((float) sizeWidth / sizeHeight) - scale) <= 0.01;
            // 宽高比相同的情况下，宽度越大越好
            boolean isBetterSize = (bestSize == null) || sizeWidth > bestSize.width;
            if (isDesireRatio && isBetterSize) {
                if (sizeWidth <= maxWidth && sizeHeight <= maxWidth && (sizeWidth >= minWidth && sizeHeight >= minWidth)) {
                    if (null != bestSize) {
                        if (Math.abs(sizeWidth - bestWidth) <= Math.abs(bestSize.width - bestWidth)) {
                            bestSize = size;
                        }
                    } else {
                        bestSize = size;
                    }
                }
            }

            if (null == bestSize2) {
                bestSize2 = size;
            }
            // 第二优先级
            // 摄像头的宽高比和当前屏幕的宽高比差距越小越好
            boolean isDesireRatio2 = Math.abs(((float) sizeWidth / sizeHeight) - scale) <= Math.abs(((float) bestSize2.width / bestSize2.height) - scale);
            if (isDesireRatio2) {
                if (sizeWidth <= maxWidth && sizeWidth >= minWidth) {
                    if (Math.abs(sizeWidth - bestWidth) <= Math.abs(bestSize2.width - bestWidth)) {
                        bestSize2 = size;
                    }
                }
            }
        }

        if (bestSize == null) {
            if (bestSize2 == null) {
                Log.d("TAG", "CaptureView cannot find the best camera size");
                return sizes.get(sizes.size() - 1);
            } else {
                bestSize = bestSize2;
            }
        }
        return bestSize;
    }

    /**
     * 找出最适合的预览界面分辨率
     *
     * @param screenResolution
     * @param screenOrientation
     * @param cameraParameters
     * @param cameraOrientation
     * @return
     */
    public static Point findBestPreviewResolution(Camera.Parameters cameraParameters, Point screenResolution, int screenOrientation, int cameraOrientation) {
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize(); //默认的预览尺寸
        Log.d(TAG, "camera default resolution " + defaultPreviewResolution.width + "x" + defaultPreviewResolution.height);
        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            return new Point(defaultPreviewResolution.width, defaultPreviewResolution.height);
        }
        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });
        // printlnSupportedPreviewSize(supportedPreviewResolutions);
        // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
        // 由于camera的分辨率是width>height，这里先判断我们的屏幕和相机的角度是不是相同的方向(横屏 or 竖屏),然后决定比较的时候要不要先交换宽高值
        final boolean isCandidatePortrait = screenOrientation % 180 != cameraOrientation % 180;
        final double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;
        // 移除不符合条件的分辨率
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;
            // 移除低于下限的分辨率，尽可能取高分辨率
            if (width * height < PREVIEW_SIZE_MIN_WIDTH) {
                it.remove();
                continue;
            }
            //移除宽高比差异较大的
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > PREVIEW_SIZE_MAX_WIDTH) {
                it.remove();
                continue;
            }
            // 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(width, height);
                Log.d(TAG, "found preview resolution exactly matching screen resolutions: " + exactPoint);
                return exactPoint;
            }
            //删掉宽高比比屏幕小的,防止左右出现白边
            if (aspectRatio - screenAspectRatio < 0) {
                it.remove();
                continue;
            }
        }
        // 如果没有找到合适的，并且还有候选的像素，则设置分辨率最大的
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            Point largestSize = new Point(largestPreview.width, largestPreview.height);
            Log.d(TAG, "using largest suitable preview resolution: " + largestSize);
            return largestSize;
        }
        //如果最后集合空了且本身支持640*480,则选择640*480
        if (supportedPreviewResolutions.isEmpty()) {
            it = rawSupportedSizes.iterator();
            while (it.hasNext()) {
                final Camera.Size next = it.next();
                if (next.width == 640 && next.height == 480) {
                    return new Point(next.width, next.height);
                }
            }
        }
        // 没有找到合适的，就返回默认的
        Point defaultResolution = new Point(defaultPreviewResolution.width, defaultPreviewResolution.height);
        Log.i(TAG, "No suitable preview resolutions, using default: " + defaultResolution);
        return defaultResolution;
    }

}
