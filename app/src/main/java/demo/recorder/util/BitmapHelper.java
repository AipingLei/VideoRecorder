package demo.recorder.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;

/**
 * Created by se7en on 2017/4/9.
 */

public class BitmapHelper {

    public static Bitmap createBitmap(int width,int height){
        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(width, height, conf); // this creates a MUTABLE bitmap
        return bmp;
    }

    public static Bitmap createBitmap(Context aContext, String aPath){
        Bitmap sBitmap = null;
        try {
            sBitmap = BitmapFactory.decodeStream(aContext.getResources().getAssets().open(aPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sBitmap;
    }



}
