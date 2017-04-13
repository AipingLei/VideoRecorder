package demo.recorder.encoder;

import android.util.Log;

import java.io.File;

/**
 * ffmpeg 命令行辅助类
 *
 * @author leisu@iflytek.com
 */
public final class FFmpegCommondHelper {
    static {
        System.loadLibrary("iflytekstudio");
    }

    static FFmpegCommondHelper instance;

    public static synchronized FFmpegCommondHelper getInstance() {
        if (null == instance) {
            instance = new FFmpegCommondHelper();
        }
        return instance;
    }

    /**
     * 执行一个ffmpeg命令
     *
     * @param command
     * @return 0-成功，其他-失败
     */
    public boolean runFFmpegCommand(String command) {
        if (command.length() == 0) {
            return false;
        }
        String[] args = command.split(" ");

        for (int i = 0; i < args.length; i++) {
            Log.d("ffmpeg-jni", args[i]);
            if (i > 1 && args[i - 1].equals("-i")) {
                String inputFile = args[i];
                if (!new File(inputFile).isFile()) {
                    return false;
                }
            }
        }
        return run(args.length, args) == 0;
    }

    public native int run(int argc, String[] args);
}
