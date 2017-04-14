package com.iflytek.codec.ffmpeg.encoder;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.AudioInitInputParams;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.EncoderOutputParams;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.InitOutputParams;
import com.iflytek.codec.ffmpeg.encoder.MP4EncoderSoftware.VideoInitInputParams;

/**
 * MP4硬编码器（需要SDK版本18及以上使用），将图像数据编码为h264格式，将音频数据编码为aac格式 
 * @author leisu@iflytek.com
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MP4EncoderHardware 
{
	/*common*/
	private MediaMuxer mMuxer;
	private boolean mMuxerStarted;
//	private static final String TAG = "MP4EncoderHardware";
//	private static final boolean VERBOSE = true;
	private EncoderOutputParams mEncoderOutputParams;
	// Muxer state
	private static int TOTAL_NUM_TRACKS = 0;
	private int numTracksAdded = 0;
	
	/*audio var*/
	private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";
	private static final int AUDIO_SAMPLE = 1024; // aac的sample固定1024
	// Audio state
	int mAudioframeCount = 0;
	private MediaFormat mAudioMediaFormat;
	private MediaCodec mAudioEncoder;
	private int mAudioTrackIndex = -1;
	private BufferInfo mAudioBufferInfo;
	private long mAudioPts; // 音频已编码时间，单位微秒
	private int mAudioPtsStep; // 音频一帧时长
	private int outAudioFrameSize; // 音频每帧大小
	
	
	/*video var*/
	// parameters for the encoder
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
	private static int FRAME_RATE = 15; // 15fps
	private static int TIMEOUT_USEC = 10000;
	private static int COMPRESS_RATIO = 256;
	private static int BIT_RATE; // bit rate CameraWrapper.
	private int mWidth;
	private int mHeight;
	private MediaCodec mVideoEncoder;
	private BufferInfo mVideoBufferInfo;
	private int mVideoTrackIndex = -1;
	byte[] mVideoFrameData;
	FileOutputStream mFileOutputStream = null;
	private int mVideoColorFormat;
	private long mVideoPts; // 视频已编码时间，单位微秒
	private int mVideoPtsStep; // 视频一帧时长
	private int outVideoFrameSize; // 视频每帧大小
	
	public InitOutputParams init(VideoInitInputParams videoInitParams, AudioInitInputParams audioInitParams, String outFileAbsPath)
	{
		InitOutputParams initOutputParams = new InitOutputParams();
		boolean result = false;
		int nextFrameType = 0; // 下一帧需要的数据类型(1-图像数据，2-音频数据)
		TOTAL_NUM_TRACKS = 0;
		do
		{
			
			if(null != audioInitParams)
			{
				result = prepareAudio(audioInitParams);
				if(!result)
				{
					break;
				} 
				else
				{
					TOTAL_NUM_TRACKS++;
					nextFrameType = 2;
				}
			}
			
			if(null != videoInitParams)
			{
				result = prepareVideo(videoInitParams);
				if(!result)
				{
					break;
				} 
				else
				{
					TOTAL_NUM_TRACKS++;
					nextFrameType = 1;
				}
			}
			
			if(result)
			{
				result = prepareMuxer(outFileAbsPath);
				
				if(!result)
				{
					break;
				}
			}
		}while(false);
		
		if(result)
		{
			mEncoderOutputParams = new EncoderOutputParams();
			initOutputParams.isSuccess = true;
			initOutputParams.audioFrameSize = outAudioFrameSize;
			initOutputParams.videoFrameSize = outVideoFrameSize;
			initOutputParams.nextFrameType = nextFrameType;
		}
		return initOutputParams;
	}
	
	/**
	 * 编码一帧数据
	 * @param frameType 帧数据类型(1-图像数据，2-音频数据)
	 * @param data 数据
	 * @param dataLen 数据长度
	 * @return
	 */
	public EncoderOutputParams encodeFrame(int frameType, byte[] data, int dataLen)
	{
		mEncoderOutputParams.isSuccess = false;
		if(frameType==1)
		{
			mEncoderOutputParams.isSuccess = encodeVideoFrame(data);
		} 
		else if(frameType==2)
		{
			mEncoderOutputParams.isSuccess = encodeAudioFrame(data, dataLen);
		}
		
		mEncoderOutputParams.encodedTime = (int) (Math.max(mAudioPts, mVideoPts) / 1000); // 当前已编码总时长
		
		if (null == mVideoEncoder || (null != mVideoEncoder && null != mAudioEncoder && mAudioPts < mVideoPts)) // 下一帧需要音频帧数据
		{
			mEncoderOutputParams.nextFrameType = 2;
		}
		else // 下一帧需要视频帧数据
		{
			mEncoderOutputParams.nextFrameType = 1;
		}
		return mEncoderOutputParams;
	}
	
	public void uninit()
	{
		stopAudio();
		stopVideo();
		stopMuxer();
	}
	
	private void stopMuxer() {
		mMuxer.stop();
		mMuxer.release();
		mMuxer = null;
		mMuxerStarted = false;
		numTracksAdded = 0;
	}
	
	/**
	 * 初始化混合器
	 * @param outFileAbsPath
	 * @return
	 */
	private boolean prepareMuxer(String outFileAbsPath)
	{
		try {
			mMuxer = new MediaMuxer(outFileAbsPath,
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
			mMuxerStarted = false;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean prepareAudio(AudioInitInputParams audioInitParams) 
	{
		
		boolean result = true;
		

		mAudioBufferInfo = new BufferInfo();
		mAudioMediaFormat = new MediaFormat();
		mAudioMediaFormat.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);

		// 2 /* OMX_AUDIO_AACObjectLC */,
		// 5 /* OMX_AUDIO_AACObjectHE */,
		// 39 /* OMX_AUDIO_AACObjectELD */
		mAudioMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
				MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		mAudioMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioInitParams.samplerate);
		mAudioMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioInitParams.channel);
		mAudioMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioInitParams.bitrate);
		// TODO
//		audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, audioInitParams.bit);
		mAudioMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
		
		
		try {
			mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE);
		} catch (Exception e) 
		{
			e.printStackTrace();
			result = false;
		}
		mAudioEncoder.configure(mAudioMediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mAudioEncoder.start();
		
		mAudioTrackIndex = -1;
		
		// 计算音频相关参数
		outAudioFrameSize = AUDIO_SAMPLE * audioInitParams.channel; // 计算一帧sample大小
		int bytesPerSecond = audioInitParams.samplerate * audioInitParams.bit * audioInitParams.channel / 8;
		mAudioPtsStep = (int) ((float)1000000 / ((float)bytesPerSecond/outAudioFrameSize)); // 计算音频每一帧时长
		
		return result;
	}
	
	
	/**
	 * 停止音频编码
	 */
	private void stopAudio() {
		if(null != mAudioEncoder)
		{
			try {
				
				mAudioEncoder.stop();
				mAudioEncoder.release();
				mAudioEncoder = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 编码音频数据
	 * 
	 * @param input
	 * @param inputLen
	 */
	private boolean encodeAudioFrame(byte[] input, int inputLen) 
	{

		// transfer previously encoded data to muxer
		boolean result = drainAudioEncoder(mAudioEncoder, mAudioBufferInfo, false);
		// send current frame data to encoder
		if(!result)
		{
			return false;
		}
		
		try {
			ByteBuffer[] inputBuffers = mAudioEncoder.getInputBuffers();
			int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(input);

				// if (eosReceived) {
				// mAudioEncoder.queueInputBuffer(inputBufferIndex, 0,
				// input.length, presentationTimeUs,
				// MediaCodec.BUFFER_FLAG_END_OF_STREAM);

				// } else {
				mAudioEncoder.queueInputBuffer(inputBufferIndex, 0,
						input.length, mAudioPts, 0);
				
				mAudioPts += mAudioPtsStep; // 计算音频已编码总时长
				// }
			}
		} catch (Throwable t) {
			t.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	private boolean drainAudioEncoder(MediaCodec encoder,
			BufferInfo bufferInfo,
			boolean endOfStream) 
	{
		final int TIMEOUT_USEC = 100;
		ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
		while (true) {
			int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo,
					TIMEOUT_USEC);
			if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (!endOfStream) {
					break; // out of while
				} else {
				}
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				encoderOutputBuffers = encoder.getOutputBuffers();
			} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// should happen before receiving buffers, and should only
				// happen once

				// TODO
//				if (mMuxerStarted) {
//					throw new RuntimeException(
//							"format changed after muxer start");
//				}
				MediaFormat newFormat = encoder.getOutputFormat();

				// now that we have the Magic Goodies, start the muxer
				mAudioTrackIndex = mMuxer.addTrack(newFormat);
				numTracksAdded++;
				if (numTracksAdded == TOTAL_NUM_TRACKS) {
					mMuxer.start();
					mMuxerStarted = true;
				}

			} else if (encoderStatus < 0) {
			} else {
				ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
				if (encodedData == null) {
					return false;
				}

				if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
					bufferInfo.size = 0;
				}

				if (bufferInfo.size != 0) {
					
					// TODO
//					if (!mMuxerStarted) {
//						throw new RuntimeException("muxer hasn't started");
//					}

					// adjust the ByteBuffer values to match BufferInfo (not
					// needed?)
					encodedData.position(bufferInfo.offset);
					encodedData.limit(bufferInfo.offset + bufferInfo.size);
					mMuxer.writeSampleData(mAudioTrackIndex, encodedData,
							bufferInfo);
				}

				encoder.releaseOutputBuffer(encoderStatus, false);

				if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					if (!endOfStream) {
					} else {
					}
					break; // out of while
				}
			}
		}
		return true;
	}
	
	
	/**
	 * 准备视频编码器
	 * @param videoInitInputParams
	 * @return
	 */
	private boolean prepareVideo(VideoInitInputParams videoInitInputParams)
	{
		this.mWidth = videoInitInputParams.inputWidth;
		this.mHeight = videoInitInputParams.inputHeight;
		FRAME_RATE = videoInitInputParams.fps;
		//COMPRESS_RATIO = 32 * (int) Math.pow(2, videoInitInputParams.quality);
		COMPRESS_RATIO = 256; // 值越小，压缩比就越低，质量越好
		//BIT_RATE = mWidth * mHeight * 3 * 8 * FRAME_RATE / COMPRESS_RATIO;
		BIT_RATE = videoInitInputParams.bitrate;
		mVideoFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
		
		
		mVideoBufferInfo = new BufferInfo();
		MediaCodecInfo codecInfo = selectVideoCodec(MIME_TYPE);
		if (codecInfo == null) 
		{
			return false;
		}
		
		mVideoColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
		if(mVideoColorFormat==0)
		{
			return false;
		}
		
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
				this.mWidth, this.mHeight);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mVideoColorFormat);
		
		int keyFrameInterval = FRAME_RATE/8;
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
				keyFrameInterval>0 ? keyFrameInterval : 1);
		
		try {
			mVideoEncoder = MediaCodec.createByCodecName(codecInfo.getName());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		mVideoEncoder.configure(mediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mVideoEncoder.start();
		
		mVideoTrackIndex = -1;
		
		// 计算视频相关参数
		outVideoFrameSize = mWidth * mHeight * 3 >> 1; // 计算视频一帧大小
		mVideoPtsStep = 1000 / FRAME_RATE * 1000; // 计算一帧视频时长
		return true;
	}
	
	/**
	 * 查找视频编码器
	 * @param mimeType
	 * @return
	 */
	private static MediaCodecInfo selectVideoCodec(String mimeType) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {
				continue;
			}
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					return codecInfo;
				}
			}
		}
		return null;
	}
	
	/**
	 * 查找视频像素格式
	 * @param codecInfo
	 * @param mimeType
	 * @return
	 */
	private static int selectColorFormat(MediaCodecInfo codecInfo,
			String mimeType) {
		MediaCodecInfo.CodecCapabilities capabilities = codecInfo
				.getCapabilitiesForType(mimeType);
		for (int i = 0; i < capabilities.colorFormats.length; i++) {
			int colorFormat = capabilities.colorFormats[i];
			if (isRecognizedFormat(colorFormat)) {
				return colorFormat;
			}
		}
		return 0;
	}
	
	/**
	 * Returns true if this is a color format that this test code understands
	 * (i.e. we know how to read and generate frames in this format).
	 */
	private static boolean isRecognizedFormat(int colorFormat) {
		switch (colorFormat) {
		// these are the formats we know how to handle for this test
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			return false;
		}
	}
	
	public void stopVideo() {
		if(null != mVideoEncoder)
		{
			try {
				mVideoEncoder.stop();
				mVideoEncoder.release();
				mVideoEncoder = null;
				
				mVideoFrameData = null;
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}
	
	/**
	 * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
	 * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV Apply NV21 to
	 * I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
	 */
	private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
			int width, int height) {
		System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
		for (int i = width * height; i < nv21bytes.length; i += 2) {
			i420bytes[i] = nv21bytes[i + 1];
			i420bytes[i + 1] = nv21bytes[i];
		}
	}
	
	private boolean encodeVideoFrame(byte[] input) 
	{
//		NV21toI420SemiPlanar(input, mVideoFrameData, this.mWidth, this.mHeight);
		System.arraycopy(input, 0, mVideoFrameData, 0, input.length);
		
		ByteBuffer[] inputBuffers = mVideoEncoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mVideoEncoder.getOutputBuffers();
		int inputBufferIndex = mVideoEncoder.dequeueInputBuffer(TIMEOUT_USEC);
		
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(mVideoFrameData);
			
			
			//int flag = isEndVideo ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
			mVideoEncoder.queueInputBuffer(inputBufferIndex, 0,
					mVideoFrameData.length, mVideoPts, 0);
			mVideoPts += mVideoPtsStep; // 计算视频已编码总时长
		} else {
			return false;
		}

		int outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
		do {
			if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				outputBuffers = mVideoEncoder.getOutputBuffers();
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				MediaFormat newFormat = mVideoEncoder.getOutputFormat();

                mVideoTrackIndex = mMuxer.addTrack(newFormat);
                
                numTracksAdded++;
				if (numTracksAdded == TOTAL_NUM_TRACKS) {
					mMuxer.start();
					mMuxerStarted = true;
				}
			} else if (outputBufferIndex < 0) {
			} else {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				if (outputBuffer == null) {
					return false;
                }
				
				if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mVideoBufferInfo.size = 0;
                }

				if (mVideoBufferInfo.size != 0) {
					if (!mMuxerStarted) {
						MediaFormat newFormat = mVideoEncoder.getOutputFormat();
						mVideoTrackIndex = mMuxer.addTrack(newFormat);
						numTracksAdded++;
						if (numTracksAdded == TOTAL_NUM_TRACKS) {
							mMuxer.start();
							mMuxerStarted = true;
						}
					}

					outputBuffer.position(mVideoBufferInfo.offset);
					outputBuffer.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
					
					mMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, mVideoBufferInfo);
				}

				mVideoEncoder.releaseOutputBuffer(outputBufferIndex, false);
			}
			outputBufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, TIMEOUT_USEC);
		} while (outputBufferIndex >= 0);
		return true;
	}
}
