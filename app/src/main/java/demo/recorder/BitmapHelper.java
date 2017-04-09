package demo.recorder;

import android.graphics.Bitmap;

/**
 * Created by se7en on 2017/4/9.
 */

public class BitmapHelper {

    public static Bitmap createBitmap(int width,int height){
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(width, height, conf); // this creates a MUTABLE bitmap
        return bmp;
    }



}
