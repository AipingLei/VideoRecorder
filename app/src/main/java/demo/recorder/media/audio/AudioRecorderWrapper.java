package demo.recorder.media.audio;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

/**
 * 音频录制封装类
 */
public class AudioRecorderWrapper implements Runnable
{
	AudioRecord mAudioRecord;
	// 录制频率即采样率
	public int SAMPLE = 44100;
	// 录制通道，可以为AudioFormat.CHANNEL_CONFIGURATION_MONO和AudioFormat.CHANNEL_CONFIGURATION_STEREO
	public int CHNNEL = AudioFormat.CHANNEL_IN_STEREO;
	// 录制编码格式，可以为AudioFormat.ENCODING_16BIT和8BIT,其中16BIT的仿真性比8BIT好，但是需要消耗更多的电量和存储空间
	public int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	private int minBufferSize;
	private String outFilePath;
	public AudioRecorderWrapper(String outFilePath)
	{
		minBufferSize = AudioRecord.getMinBufferSize(SAMPLE, CHNNEL, FORMAT);
		mAudioRecord = new AudioRecord(AudioSource.MIC, SAMPLE, CHNNEL, FORMAT, minBufferSize);
		
		this.outFilePath = outFilePath;
	}
	
	FileOutputStream fos;
	
	private Thread mThread;
	
	
	private synchronized boolean startRecord(boolean isResume)
	{
		if(null == mThread || !mThread.isAlive())
		{
			if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
			{
				if(!isResume)
				{
					try {
						fos = new FileOutputStream(outFilePath);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						return false;
					}	
				}
				mAudioRecord.startRecording();
			}
			
			mThread = new Thread(this);
			mThread.start();			
		}
		return true;
	}
	
	public synchronized boolean startRecord()
	{
		return startRecord(false);
	}
	
	public synchronized void stopRecord()
	{
		if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
		{
			mAudioRecord.stop();
			
			if(null != fos)
			{
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			if(null != mThread && mThread.isAlive())
			{
				try {
					mThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized void pauseRecord()
	{
		if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
		{
			mAudioRecord.stop();
			if(null != mThread && mThread.isAlive())
			{
				try {
					mThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public synchronized void resumeRecord()
	{
		if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
		{
			startRecord(true);	
		}
	}

	@Override
	public void run() 
	{
		try {

			byte[] buffer = new byte[minBufferSize];
			while(true) {
				int result = mAudioRecord.read(buffer, 0, minBufferSize);
				if(result == AudioRecord.ERROR_INVALID_OPERATION) {
					if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
						break;
					}
				}else if(result == AudioRecord.ERROR_BAD_VALUE) {
					if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
						break;
					}
				}else if(result == AudioRecord.ERROR) {
					if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
					{
						break;
					}
				}else {
					if(result<=0) {
						if(mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
							break;
						}
					}else {
						try {
							fos.write(buffer, 0, result);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
