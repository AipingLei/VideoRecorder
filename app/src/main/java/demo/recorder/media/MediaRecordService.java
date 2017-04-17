package demo.recorder.media;


import android.app.Activity;
import android.app.ProgressDialog;
import android.media.AudioFormat;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import demo.recorder.util.FFmpegMuxer;
import demo.recorder.media.audio.AudioRecorderWrapper;
import demo.recorder.media.audio.PCMDenoiser;
import com.iflytek.codec.ffmpeg.encoder.M4AEncoder;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import demo.recorder.ui.CameraPreview;

import static jp.co.cyberagent.android.gpuimage.GPUFilterType.FILTER_GPUIMAGE_MASK;
import static jp.co.cyberagent.android.gpuimage.GPUFilterType.FILTER_NONE;

/** 
 * description: A service to generate media file(support video and audio only)
 * create by: leiap
 * create date: 2017/4/12
 * update date: 2017/4/12
 * version: 1.0
*/
public class MediaRecordService implements OnRecordStatusChangedListener {

    private static final String TAG = "MediaRecordService";


    private VideoRecordCore mVideoRecordCore;

    List<MediaObject> mMediaList;
    private long mRecordTime;


    private boolean enableAudioDenoise;

    private static  String recordVideo;
    private static  String recordAudio;
    private static  String recordAudioDenoise;
    private Activity mContext;
    private String finalFile;


    public MediaRecordService(Activity aActivity,CameraPreview cameraPreview) {
        this.mMediaList = new ArrayList<>();
        this.mContext = aActivity;
        initFileDir(aActivity);
        initVideoCore(cameraPreview);
        audioRecorderWrapper = new AudioRecorderWrapper(recordAudio);
    }

    private void initFileDir(Activity aActivity) {
        File dir =  aActivity.getExternalFilesDir("se7en");
        if (!dir.exists()){
            dir.mkdir();
        }
        String sBasePath = dir.getAbsolutePath()+File.separator;

        recordVideo =sBasePath+"record.mp4";
        recordAudio =sBasePath+"record.pcm";
        recordAudioDenoise = sBasePath+"record.pcm_.denoise";
        finalFile = sBasePath+"recordFinal.mp4";
    }

    private void initVideoCore(CameraPreview cameraPreview) {
        mVideoRecordCore = new VideoRecordCore(FILTER_NONE,FILTER_GPUIMAGE_MASK);
        mVideoRecordCore.bindPreview(cameraPreview);
        mVideoRecordCore.setCameraHandler(new VideoRecordCore.CameraHandler(mVideoRecordCore));
        mVideoRecordCore.setOnRecordStatusChangedListener(this);
        mVideoRecordCore.configOutputFile(new File(recordVideo));
        mVideoRecordCore.configRecordQualityType(VideoRecordCore.QUALITY_NORMAL_HIGH);
        mVideoRecordCore.configRecordSize(1080, 1080);
        mVideoRecordCore.configIntervalNotifyRecordProcessing(1000);
    }


    public void addMedia(MediaObject mediaObject){
        mMediaList.add(mediaObject);
    }


    public void startRecord() {
        mVideoRecordCore.changeRecordingState(VideoRecordCore.RECORDING_START);
    }

    public void stopRecord() {
        mVideoRecordCore.changeRecordingState(VideoRecordCore.RECORDING_STOP);
    }

    public void pauseRecord() {
        mVideoRecordCore.changeRecordingState(VideoRecordCore.RECORDING_PAUSE);
    }

    public void resumeRecord() {
        mVideoRecordCore.changeRecordingState(VideoRecordCore.RECORDING_RESUME);
    }




    public void pause(){
        mVideoRecordCore.onPause();
/*        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        mGLView.onPause();
        Log.d(TAG, "onPause complete");*/
    }

    public void resume(){
        mVideoRecordCore.openCamera(480, 480);      // updates mCameraPreviewWidth/Height
    }
    public void destroy() {
        mVideoRecordCore.destroy();
        //mCameraHandler.invalidateHandler();     // paranoia
    }

    // 是否支持录音
    private boolean enableAudioRecord = true;
    private AudioRecorderWrapper audioRecorderWrapper;

    @Override
    public void onRecordStart() {
        Log.d("TAG2", "onRecordStart");

        // 开启录音
        if (enableAudioRecord) {
            audioRecorderWrapper.startRecord();
        }
        mRecordTime = 0;
    }

    @Override
    public void onRecordPaused( long curRecoredTime) {
        Log.d("TAG2", "onRecordPaused：" + curRecoredTime);

        // 暂停录音
        if (enableAudioRecord) {
            audioRecorderWrapper.pauseRecord();
        }
        mRecordTime = curRecoredTime;
    }

    @Override
    public void onRecordProcessing( long curRecoredTime) {
        Log.d("TAG2", "onRecordProcessing：" + curRecoredTime);
        mRecordTime = curRecoredTime;
    }

    @Override
    public void onRecordResume(long curRecoredTime) {
        Log.d("TAG2", "onRecordResume：" + curRecoredTime);
        if (enableAudioRecord) {
            audioRecorderWrapper.resumeRecord();
        }
        mRecordTime = curRecoredTime;
    }



    @Override
    public void onRecordStop(File recordFile, long recordedTotalTime) {
        Log.d("TAG2", "onRecordStop:" + recordFile + ",time:" + recordedTotalTime);

        // 停止录音
        if (enableAudioRecord) {
            audioRecorderWrapper.stopRecord();
        }
        // 设置音频编码参数
        MP4EncoderSoftware.AudioInitInputParams audioParams = new MP4EncoderSoftware.AudioInitInputParams();
        audioParams.bit = audioRecorderWrapper.FORMAT == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8;
        audioParams.bitrate = 128000;
        audioParams.channel = audioRecorderWrapper.CHNNEL == AudioFormat.CHANNEL_IN_STEREO ? 2 : 1;
        audioParams.samplerate = audioRecorderWrapper.SAMPLE;

        if(enableAudioDenoise){
            // 音频降噪完了编码合成
            audioDenoise(recordAudio,recordAudioDenoise,audioParams);
        }else {
            // 编码音频并且合成视频
            encodeAndMuxer(recordAudio, audioParams, recordVideo, finalFile);
        }
    }

    /**
     * 音频降噪
     *
     * @param pcmInputFile
     * @param pcmOutputFile
     * @param audioInitInputParams
     */
    public void audioDenoise(final String pcmInputFile, final String pcmOutputFile, final MP4EncoderSoftware.AudioInitInputParams audioInitInputParams) {
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final ProgressDialog dialog = ProgressDialog.show(mContext, "", "processing audioDenoise");
                dialog.setCancelable(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PCMDenoiser PCMDenoiser = new PCMDenoiser(pcmInputFile, pcmOutputFile, audioInitInputParams);
                        PCMDenoiser.startToDenoise(new PCMDenoiser.PCMDenoiseListener() {
                            @Override
                            public void onDenoiseStart() {
                            }

                            @Override
                            public void onDenoiseFailed() {
                                dialog.dismiss();
                            }

                            @Override
                            public void onDenoiseSuccess() {
                                dialog.dismiss();
                                // 编码音频并且合成视频
                                encodeAndMuxer(recordAudio, audioInitInputParams, recordVideo, finalFile);
                            }
                        });
                    }
                }).start();
            }
        });
    }

    /**
     * 编码与合成
     *
     * @param pcmInputFile
     * @param audioInitInputParams
     * @param mp4InputFile
     * @param outMP4File
     */
    public void encodeAndMuxer(final String pcmInputFile, final MP4EncoderSoftware.AudioInitInputParams audioInitInputParams, final String mp4InputFile, final String outMP4File) {
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final ProgressDialog dialog = ProgressDialog.show(mContext, "", "video encoding!");
                dialog.setCancelable(false);

                new Thread() {
                    public void run() {
                        final String m4aFile = pcmInputFile + "_.m4a";
                        M4AEncoder m4aEncoder = new M4AEncoder(pcmInputFile, audioInitInputParams, 0, m4aFile);
                        m4aEncoder.encode(new M4AEncoder.M4AEncoderListener() {

                            @Override
                            public void onEncodeSuccess(File encodedFile) {
                                showMessage("video encoding success，prepare for mix");

                                boolean result = FFmpegMuxer.muxerMP4(mp4InputFile, false, encodedFile.getAbsolutePath(), outMP4File);

                                if (result) {
                                    showMessage("mix success，the file is in：" + outMP4File);
                                } else {
                                    showMessage("mix failed");
                                }
                                dialog.dismiss();
                            }

                            @Override
                            public void onEncodeStart() {
                            }

                            @Override
                            public boolean onEncodeProcessing(int percent) {
                                return false;
                            }

                            @Override
                            public void onEncodeFailed(int percent) {
                                showMessage("audio encoding failed，percent：" + percent);
                                dialog.dismiss();
                            }
                        });
                    }
                }.start();
            }
        });
    }

    /**
     * 当前系统是否可以使用视频录制
     *
     * @return
     */
    public static final boolean isCanUseInCurrentSystem() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    private void showMessage(final String message) {
        mContext.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        });
    }




}
