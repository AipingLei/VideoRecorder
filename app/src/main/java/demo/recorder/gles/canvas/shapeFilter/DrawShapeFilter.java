package demo.recorder.gles.canvas.shapeFilter;


import demo.recorder.gles.canvas.ICanvasGL;

/**
 * Created by Chilling on 2016/11/11.
 */

public interface DrawShapeFilter {
    String getVertexShader();
    String getFragmentShader();

    void onPreDraw(int program, ICanvasGL canvas);
    void destroy();
}
