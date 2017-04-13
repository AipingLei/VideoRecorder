package demo.recorder.encoder;

import android.text.TextUtils;


/**
 * MP4编码器封装类 
 * @author leisu@iflytek.com
 */
public class MP4EncoderWrapper 
{
	
	boolean isUseEncoderHW = false;
	
	/**
	 * 
	 * @param isUseEncodeHardware 是否使用硬件编码，要求android sdk>=18
	 */
	public MP4EncoderWrapper(boolean isUseEncodeHardware)
	{
		this.isUseEncoderHW = isUseEncodeHardware;
	}

	MP4EncoderHardware encoderHW;
	
	/**
	 * 初始化编码器,初始化成功时会写入MP4文件头
	 * @param videoInitParams
	 * @param audioInitParams
	 * @param outFileAbsPath
	 */
	public MP4EncoderSoftware.InitOutputParams init(MP4EncoderSoftware.VideoInitInputParams videoInitParams, MP4EncoderSoftware.AudioInitInputParams audioInitParams, String outFileAbsPath)
	{
		
		if(null == videoInitParams && null == audioInitParams || TextUtils.isEmpty(outFileAbsPath))
		{
			MP4EncoderSoftware.InitOutputParams initOutputParams = new MP4EncoderSoftware.InitOutputParams();
			initOutputParams.isSuccess = false;
			return initOutputParams;
		}
		
		if(isUseEncoderHW)
		{
			encoderHW = new MP4EncoderHardware();
			return encoderHW.init(videoInitParams, audioInitParams, outFileAbsPath);
		} 
		else
		{
			return MP4EncoderSoftware.init(videoInitParams, audioInitParams, outFileAbsPath);
		}
	}
	
	/**
	 * 编码一帧数据
	 * @param frameType 帧数据类型(1-图像数据，2-音频数据)
	 * @param data 数据
	 * @param length 数据长度
	 * @return 
	 */
	public MP4EncoderSoftware.EncoderOutputParams encodeFrame(int frameType, byte[] data, int length)
	{
		if(isUseEncoderHW)
		{
			return encoderHW.encodeFrame(frameType, data, length);
		} 
		else
		{
			return MP4EncoderSoftware.encodeFrame(frameType, data, length);
		}
	}
	
	/**
	 * 释放资源，编码结束，会写入MP4文件尾
	 */
	public void uninit()
	{
		if(isUseEncoderHW)
		{
			encoderHW.uninit();
			encoderHW = null;
		} 
		else
		{
			MP4EncoderSoftware.uninit();
		}
	}
}
