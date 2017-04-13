package demo.recorder.media;

import java.io.File;

/** 
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/13
 * update date: 2017/4/13
 * version: 1.0
*/
public interface OnRecordStatusChangedListener {
    void onRecordStart();

    void onRecordPaused(long curRecoredTime);

    void onRecordProcessing(long curRecoredTime);

    void onRecordResume(long curRecoredTime);

    void onRecordStop(File recordFile, long recordedTotalTime);
}
