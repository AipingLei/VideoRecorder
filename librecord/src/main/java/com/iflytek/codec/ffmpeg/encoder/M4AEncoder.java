package com.iflytek.codec.ffmpeg.encoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.AudioInitInputParams;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.EncoderOutputParams;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.InitOutputParams;

/**
 * m4a编码器，采用硬件编码，android sdk要求大于等于18
 * @author leisu@iflytek.com
 */
public class M4AEncoder 
{
	/**
	 * 源pcm文件
	 */
	private String mPcmInputFile;
	/**
	 * 输出M4A文件
	 */
	private String mOutM4AFile;
	/**
	 * 音频参数
	 */
	private AudioInitInputParams mAudioInitInputParams;
	
	/**
	 * 
	 * @param pcmInputFile 输入pcm文件
	 * @param audioInitInputParams 输入pcm的参数
	 * @param offset 文件处理的起始位置
	 * @param outM4AFile 输出m4a文件
	 */
	public M4AEncoder(String pcmInputFile, AudioInitInputParams audioInitInputParams, int offset, String outM4AFile)
	{
		this.mPcmInputFile = pcmInputFile;
		this.mAudioInitInputParams = audioInitInputParams;
		this.mOutM4AFile = outM4AFile;
	}
	
	public interface M4AEncoderListener
	{
		void onEncodeStart();
		void onEncodeFailed(int percent);
		void onEncodeSuccess(File encodedFile);
		
		/**
		 * 
		 * @param percent
		 * @return 是否需要停止编码，true-停止，false-继续编码
		 */
		boolean onEncodeProcessing(int percent);
	}
	
	/**
	 * 开始编码（同步执行）
	 * @return
	 */
	public boolean encode(M4AEncoderListener listener)
	{
		boolean result = false;
		AudioInitInputParams aiip = mAudioInitInputParams;
		if(null != listener)
		{
			listener.onEncodeStart();
		}
		
		int percent = 0;
		int lastNotifyPercent = 0;
		MP4EncoderWrapper mp4EncoderWrapper = new MP4EncoderWrapper(true);
		InitOutputParams initOutputParams = mp4EncoderWrapper.init(null, aiip, mOutM4AFile);
		
		if(initOutputParams.isSuccess)
		{
			byte[] inputBuffer = new byte[initOutputParams.audioFrameSize];
			int encodedTotalBytes = 0; // 已编码数据长度
			FileInputStream fis = null;
			try {
				
				File inputFile = new File(mPcmInputFile);
				long fileLength = inputFile.length();
				fis = new FileInputStream(inputFile);
				
				while(true)
				{
					int len = fis.read(inputBuffer);
					if(len<=0)
					{
						result = true;
						break;
					}
					EncoderOutputParams encoderResult = mp4EncoderWrapper.encodeFrame(2, inputBuffer, len);
					
					if(!encoderResult.isSuccess)
					{
						result = false;
						break;
					}
					else
					{
						encodedTotalBytes+= len;
						percent = (int) (((float)encodedTotalBytes / fileLength) * 100);
						
						if(null != listener)
						{
							if(lastNotifyPercent != percent)
							{
								boolean isNeedStop = listener.onEncodeProcessing(percent);	
								lastNotifyPercent = percent;
								if(isNeedStop) // 停止编码
								{
									result = false;
									break;
								}
							}
						}
					}
				}
				mp4EncoderWrapper.uninit();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally
			{
				if(null != fis)
				{
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		else
		{
			if(null != listener)
			{
				listener.onEncodeFailed(percent);
			}	
		}
		
		
		if(null != listener)
		{
			if(result)
			{
				listener.onEncodeSuccess(new File(mOutM4AFile));
			}
			else
			{
				listener.onEncodeFailed(percent);
			}
		}
		return result;
	}
	
}
