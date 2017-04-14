package com.blue.librecord.recorder.video;

import java.io.File;

/**
 * 录制状态变化监听器
 * Created by blue on 2017/2/14.
 */

public interface OnRecordStatusChangedListener {
    /**
     * 录制开始
     *
     * @param cameraCaptureView
     */
    void onRecordStart(CameraCaptureView cameraCaptureView);

    /**
     * 录制暂停
     *
     * @param cameraCaptureView
     * @param curRecoredTime    当前已录制的时间,单位：毫秒
     */
    void onRecordPaused(CameraCaptureView cameraCaptureView, long curRecoredTime);

    /**
     * 录制进行中
     *
     * @param cameraCaptureView
     * @param curRecoredTime    当前已录制的时间,单位：毫秒
     */
    void onRecordProcessing(CameraCaptureView cameraCaptureView, long curRecoredTime);

    /**
     * 录制恢复
     *
     * @param cameraCaptureView
     * @param curRecoredTime    当前已录制的时间,单位：毫秒
     */
    void onRecordResume(CameraCaptureView cameraCaptureView, long curRecoredTime);

    /**
     * 录制结束
     *
     * @param cameraCaptureView
     * @param recordFile        录制文件
     * @param recordedTotalTime 录制总时间,单位:毫秒
     */
    void onRecordStop(CameraCaptureView cameraCaptureView, File recordFile, long recordedTotalTime);
}
