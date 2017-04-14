package com.blue.librecord.muxer;

import com.iflytek.codec.ffmpeg.FFmpegCommondHelper;

import java.io.File;

/**
 * ffmpeg音视频合成器
 *
 * @author leisu@iflytek.com
 */
public class FFmpegMuxer {
    /**
     * 合成mp4
     *
     * @param inputMP4File             待合成的mp4视频文件
     * @param inputMP4FileHasAudioData 待合成的mp4视频文件中是否含有音频数据；true-则会先去掉视频中的音轨（整体合成速度会稍微慢一点），false-不进行处理，直接合成
     * @param inputM4AFile             待合成的m4a音频文件
     * @param outputMP4File            合成后的输出mp4视频文件
     * @return
     */
    public static final boolean muxerMP4(String inputMP4File, boolean inputMP4FileHasAudioData, String inputM4AFile, String outputMP4File) {
        String tmpInputMP4File = inputMP4File;
        if (inputMP4FileHasAudioData) // 有音频数据，先去除音轨
        {
            String command = "ffmpeg -y -i " + inputMP4File + " -an -vcodec copy " + tmpInputMP4File + "_tmp";
            boolean removeAudioResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command);
            if (!removeAudioResult) {
                return false;
            }
        }

        String command = "ffmpeg -y -i " + tmpInputMP4File + " -i " + inputM4AFile + " -acodec copy -vcodec copy " + outputMP4File;
        boolean mergeResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command); // 合成音视频

        if (inputMP4FileHasAudioData) {
            new File(tmpInputMP4File).delete(); // 删除临时文件
        }
        return mergeResult;
    }

}
