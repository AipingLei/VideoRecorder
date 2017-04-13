package demo.recorder.encoder;

/**
 * MP4软编码器,将图像数据编码为h264格式，将音频数据编码为aac格式 
 * @author leisu@iflytek.com
 */
public class MP4EncoderSoftware 
{
	static
	{
		System.loadLibrary("ijkffmpeg");
		System.loadLibrary("iflytekstudio");
	}
	
	/**
	 * 音频初始化参数 
	 */
	public static class AudioInitInputParams
	{
		public int bitrate; // 待编码数据的比特率（64000、128000、256000）
		public int samplerate; // 待编码数据的采样率(11025,22050,44100)
		public int channel; // 声道数(1-单声道、2-双声道）
		public int bit; // 位数（8、16）
	}
	
	/**
	 * 视频初始化参数
	 */
	public static class VideoInitInputParams
	{
		public int inputWidth; // 输入图像宽度
		public int inputHeight; // 输入图像高度
		public int intputDataType; // 输入数据类型(1-nv21、2-yuv420p、3-rgba)
		public int fps; // 编码后的帧率
		public int quality;// 视频质量(1-3，快、中、慢)
		public int bitrate; // 比特率，影响文件大小（比特率算法：width * height * 3 * 8 * fps / compressRate，其中compressRate是压缩比例，一般是256，即压缩256倍）
	}
	
	/**
	 * 初始化输出参数
	 */
	public static class InitOutputParams
	{
		public boolean isSuccess; // 是否初始化成功
		public int audioFrameSize; // 音频每次采样的数据大小，编码时每次传入的音频数据必须是这么大
		public int videoFrameSize; // 图像每帧数据大小，编码时每次传入的图像数据必须是这么大
		public int nextFrameType; // 下一帧需要的数据类型(1-图像数据，2-音频数据)
	}
	
	/**
	 * 编码时输出参数 
	 */
	public static class EncoderOutputParams
	{
		public boolean isSuccess; // 是否编码成功
		public int encodedTime; // 已编码数据的总时长，单位：毫秒
		public int nextFrameType; // 下一帧需要的数据类型(1-图像数据，2-音频数据)
	}
	
	/**
	 * 初始化编码器,初始化成功时会写入MP4文件头
	 * @param videoInitParams
	 * @param audioInitParams
	 * @param outFileAbsPath
	 */
	public native static InitOutputParams init(VideoInitInputParams videoInitParams, AudioInitInputParams audioInitParams, String outFileAbsPath);
	
	/**
	 * 编码一帧数据
	 * @param frameType 帧数据类型(1-图像数据，2-音频数据)
	 * @param data 数据
	 * @param length 数据长度
	 * @return 
	 */
	public native static EncoderOutputParams encodeFrame(int frameType, byte[] data, int length);
	
	/**
	 * 释放资源，编码结束，会写入MP4文件尾
	 */
	public native static void uninit();
}
