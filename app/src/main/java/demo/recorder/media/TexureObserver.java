package demo.recorder.media;

import android.graphics.SurfaceTexture;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * Created by SE7EN on 2017/4/12.
 */

public interface TexureObserver {
    void onSurfaceCreated(SurfaceTexture aSurfaceTexture) ;

    void onSurfaceChanged(GL10 gl, int width, int height) ;

    void onDrawFrame(SurfaceTexture aSurfaceTexture, int aTexureID) ;
}
