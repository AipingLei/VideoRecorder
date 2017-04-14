package jp.co.cyberagent.android.gpuimage.filter;

import java.lang.reflect.Constructor;

import android.content.Context;

public class GPUImageFilterContext extends GPUImageFilter
{
	protected Context mContext;
	
	public GPUImageFilterContext(Context paramContext) {
		super(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        mContext = paramContext;
    }
	
	public GPUImageFilterContext(Context paramContext,String fragmentShaderString) {
		super(NO_FILTER_VERTEX_SHADER, fragmentShaderString);
        mContext = paramContext;
    }
	
	
	@Override
	public GPUImageFilter clone() 
	{
		GPUImageFilter result = null;
		try {
			Constructor<?> construstor = this.getClass().getConstructor(Context.class);
			
			result = (GPUImageFilter) construstor.newInstance(this.mContext);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			throw new RuntimeException("no construstor found at " + this.getClass().getName());
		} catch (Exception e) 
		{
			e.printStackTrace();
			throw new RuntimeException("construst failed at " + this.getClass().getName());
		}
		
		return result;
	}
}
