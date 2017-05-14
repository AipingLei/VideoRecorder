package demo.recorder;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.yixia.videoeditor.adapter.DeviceUtils;
import com.yixia.videoeditor.adapter.UtilityAdapter;

import java.io.File;

public class TestActivity extends AppCompatActivity {
    /** 应用包名 */
    private static String mPackageName;
    /** 应用版本名称 */
    private static String mAppVersionName;
    /** 应用版本号 */
    private static int mAppVersionCode;
    /** 视频缓存路径 */
    private static String mVideoCachePath;
    /** SDK版本号 */
    public final static String VCAMERA_SDK_VERSION = "1.2.0";
    /** FFMPEG执行失败存的log文件 */
    public final static String FFMPEG_LOG_FILENAME = "ffmpeg.log";
    /** 执行FFMPEG命令保存路径 */
    public final static String FFMPEG_LOG_FILENAME_TEMP = "temp_ffmpeg.log";

    /**
     * 初始化SDK
     *
     * @param context
     */
    public static void initialize(Context context) {
        mPackageName = context.getPackageName();

        mAppVersionName = getVerName(context);
        mAppVersionCode = getVerCode(context);

        //初始化底层库
        UtilityAdapter.FFmpegInit(context, String.format("versionName=%s&versionCode=%d&sdkVersion=%s&android=%s&device=%s",
                mAppVersionName, mAppVersionCode, VCAMERA_SDK_VERSION, DeviceUtils.getReleaseVersion(), DeviceUtils.getDeviceModel()));
    }

    /** 获取当前应用的版本名称 */
    public static String getVerName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }

    /**
     * 获取当前应用的版本号
     * @param context
     * @return
     */
    public static int getVerCode(Context context) {
        int verCode = -1;
        try {
            verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return verCode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File dir =  this.getExternalFilesDir("se7en");
        if (!dir.exists()){
            dir.mkdir();
        }
        initialize(this);
        String sBasePath = dir.getAbsolutePath()+File.separator;
        String command = "ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.mp4 -vcodec copy -acodec copy -vbsf h264_mp4toannexb /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.ts";
        String command2 = "ffmpeg -y -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.mp4 -vcodec copy -acodec copy -vbsf h264_mp4toannexb /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.ts";
        String command3 = "ffmpeg -y -i \"concat:/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.ts|/storage/emulated/0/Android/data/demo.recorder/files/se7en/2.ts\" -c copy -bsf:a aac_adtstoasc /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.mp4";
        String command4 = "ffmpeg -y -i " + "/storage/emulated/0/Android/data/demo.recorder/files/se7en/3.mp4" + " -i " + "/storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a" + " -acodec copy -vcodec copy " + "/storage/emulated/0/Android/data/demo.recorder/files/se7en/4.mp4";
        String command5 = "ffmpeg -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/1.m4a -itsoffset 5 -i /storage/emulated/0/Android/data/demo.recorder/files/se7en/2.m4a -filter_complex amix=inputs=2:duration=first:dropout_transition=2  /storage/emulated/0/Android/data/demo.recorder/files/se7en/3.m4a";
      /*  int i = UtilityAdapter.FFmpegRun("", command);
         i = UtilityAdapter.FFmpegRun("", command2);
         i = UtilityAdapter.FFmpegRun("", command3);*/
        int i = UtilityAdapter.FFmpegRun("", command5);
        //int i = UtilityAdapter.FFmpegRun("", command5);

       /* i = UtilityAdapter.FFmpegRun("", command2);
        i = UtilityAdapter.FFmpegRun("", command3);
        i = UtilityAdapter.FFmpegRun("", command4);*/

        Log.i("Log.i", System.currentTimeMillis()+"   aaa");

        /*  new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = FFmpegMuxer.muxerMP3();
                System.out.println(success);
            }
        }).start();*/

    }

}
