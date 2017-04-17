package demo.recorder.media.audio;

import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware;
import com.iflytek.lib.libspeex.SpeexWrapper;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 音频降噪
 */
public class PCMDenoiser {

    private byte outNoiseBuf[] = null;
    private byte inNoiseBuf[] = null;
    // 降噪的最小buffer
    private int denoiseGetBestBufferSize = -1;

    private String pcmInputFile;
    private String pcmOutputFile;
    private MP4EncoderSoftware.AudioInitInputParams audioInitInputParams;

    public PCMDenoiser(String pcmInputFile, String pcmOutputFile, MP4EncoderSoftware.AudioInitInputParams audioInitInputParams) {
        this.pcmInputFile = pcmInputFile;
        this.pcmOutputFile = pcmOutputFile;
        this.audioInitInputParams = audioInitInputParams;
    }

    /**
     * 开始降噪
     *
     * @param pcmDenoiseListener
     */
    public void startToDenoise(PCMDenoiseListener pcmDenoiseListener) {
        FileInputStream in;
        BufferedOutputStream out;
        try {
            // 音频数据采样率
            int sampleRate = audioInitInputParams.samplerate;
            // 初始化降噪器
            if (SpeexWrapper.nativeDenoiseInit(sampleRate, -50)) {
                denoiseGetBestBufferSize = SpeexWrapper.nativeDenoiseGetBestBufferSize();
                if (denoiseGetBestBufferSize > 0) {
                    inNoiseBuf = new byte[denoiseGetBestBufferSize];
                    outNoiseBuf = new byte[denoiseGetBestBufferSize];
                }
            } else {
                if (null != pcmDenoiseListener) {
                    pcmDenoiseListener.onDenoiseFailed();
                }
                return;
            }
            if (null != pcmDenoiseListener) {
                pcmDenoiseListener.onDenoiseStart();
            }
            in = new FileInputStream(pcmInputFile);
            out = new BufferedOutputStream(new FileOutputStream(pcmOutputFile));
            int length = 0;
            while ((length = in.read(inNoiseBuf, 0, inNoiseBuf.length)) > -1) {
                if (length == inNoiseBuf.length) {
                    SpeexWrapper.nativeDenoise(inNoiseBuf, denoiseGetBestBufferSize, outNoiseBuf);
                    out.write(outNoiseBuf, 0, denoiseGetBestBufferSize);
                }
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            if (null != pcmDenoiseListener) {
                pcmDenoiseListener.onDenoiseFailed();
            }
        } finally {
            SpeexWrapper.nativeDenoiseUnInit();
        }

        if (null != pcmDenoiseListener) {
            pcmDenoiseListener.onDenoiseSuccess();
        }
    }

    public interface PCMDenoiseListener {
        void onDenoiseStart();

        void onDenoiseFailed();

        void onDenoiseSuccess();
    }

}
