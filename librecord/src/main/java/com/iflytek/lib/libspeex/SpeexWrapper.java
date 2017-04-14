package com.iflytek.lib.libspeex;


/**
 * LibSpeex库使用封装类
 * @author leisu@iflytek.com
 *
 */
public class SpeexWrapper 
{
	static
	{
		System.loadLibrary("SpeexWrapper");
	}
	
	
	/**
	 * 降噪器初始化
	 * @param samplingRate 准备降噪的数据采样率
	 * @param noiseSuppress 降噪允许的最大衰减幅度(值范围：-100至-1,默认值：-15)
	 * @return
	 */
	public static native boolean nativeDenoiseInit(int samplingRate, int noiseSuppress);
	
	/**
	 * 获取降噪器最佳buffer大小，初始化成功后调用有效
	 * @return
	 */
	public static native int nativeDenoiseGetBestBufferSize();
	
	/**
	 * 对PCM数据进行降噪
	 * @param inData pcm数据
	 * @param inDataLen 数据长度（最好是bestBufferSize的整数倍数）
	 * @param outData 降噪后的pcm数据
	 * @return
	 */
	public static native boolean nativeDenoise(byte[] inData, int inDataLen, byte[] outData);
	
	/**
	 * 释放降噪器
	 * @return
	 */
	public static native boolean nativeDenoiseUnInit();
	
}
