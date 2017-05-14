package demo.recorder.util;

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
        // 有音频数据，先去除音轨
        if (inputMP4FileHasAudioData) {
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
    //"ffmpeg -y -i "concat:/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a|/storage/emulated/0/Android/data/demo.recorder/files/se7en/2.m4a" -acodec copy /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.m4a";
   // ffmpeg -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/handleRecordEvent.pcm_.m4a -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/test.mp3 -filter_complex amerge -ac 2 -c:a libmp3lame -q:a 4 /storage/emulated/0/Android/data/demo.recorder/files/se7en/handleRecordEvent.pcm_.m4a2
    //ffmpeg -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.m4a -filter_complex amix=inputs=2:duration=first:dropout_transition=2 /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.m4a
    public static final boolean muxerMP3() {
        ////ffmpeg -i test.aac -i test.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2  mix.aac
        //String command ="ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/handleRecordEvent.mp4 -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a -acodec copy -vcodec copy /storage/emulated/0/Android/data/demo.recorder/files/se7en/recordFinal.mp4";
        //String command = "ffmpeg -y -i \"concat:/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a|/storage/emulated/0/Android/data/demo.recorder/files/se7en/2.m4a\" -acodec copy /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.m4a";
        String command = "ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.mp4 -vcodec copy -acodec copy -vbsf h264_mp4toannexb /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.ts";
        String command2 = "ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.mp4 -vcodec copy -acodec copy -vbsf h264_mp4toannexb /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.ts";
        //String command3 = "ffmpeg -y -i \"concat:/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.ts|/storage/emulated/0/Android/data/demo.recorder/files/se7en/2.ts\" -c copy -bsf:a aac_adtstoasc /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.mp4";
        String command3 = "ffmpeg -y -i \"concat:/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.ts|/storage/emulated/0/Android/data/demo.recorder/files/se7en/2.ts\" -c copy -bsf:a aac_adtstoasc /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.mp4";
        //String command = "ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.m4a -filter_complex amix=inputs=2:duration=first:dropout_transition=2 /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.m4a";
        //boolean mergeResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command); // 合成音频
        //mergeResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command2); // 合成音频
        boolean mergeResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command3); // 合成音频

        return mergeResult;
    }
    //ffmpeg -i "concat:first.mp3|second.mp3" -acodec copy third.mp3
    //ffmpeg -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.mp3 -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.mp3
    //ffmpeg -i first.mp3 -i second.mp3 -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 third.mp3
    public static final boolean muxer2MP3(String outputMP3File, String inputMP3File1, String inputMP3File2) {
        String command = "ffmpeg -y -i ";
        command += "\"concat:";
        command += inputMP3File1;
        command += "|";
        command += inputMP3File2+"\"";
        command += " -acodec copy ";
        command += outputMP3File;

      /*  command += inputMP3File1;
        command += " -i ";
        command += inputMP3File2;
        command += " -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp3 ";
        command += outputMP3File;*/
        boolean mergeResult = FFmpegCommondHelper.getInstance().runFFmpegCommand(command); // 合成音频
        return mergeResult;
    }


}
