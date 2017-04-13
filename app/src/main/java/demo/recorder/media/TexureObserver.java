package demo.recorder.media;

import android.graphics.SurfaceTexture;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/** 
 * description: describe the class
 * create by: leiap
 * create date: 2017/4/13
 * update date: 2017/4/13
 * version: 1.0
*/
public interface TexureObserver {

    /**
     * description: the SurfaceTexture is created or recreated.
     * note that this method may be called by GLSurfaceView's
     * render thread
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    void onSurfaceCreated(SurfaceTexture aSurfaceTexture) ;

    /**
     * description:notify the observer that SurfaceSize has changed
     * note that this method may be called by GLSurfaceView's
     * render thread
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    void onSurfaceChanged(GL10 gl, int width, int height) ;
    /**
     * description:notify the observer that the
     * note that this method may be called by GLSurfaceView's
     * render thread
     * params:
     * @return :
     * create by: leiap
     * update date: 2017/4/13
     */
    void onDrawFrame(SurfaceTexture aSurfaceTexture, int aTexureID) ;
}
