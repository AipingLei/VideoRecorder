package jp.co.cyberagent.android.gpuimage.filter.expand.magic;

import com.blue.librecord.R;

import android.content.Context;

/**
 * 童话
 * @author sulei
 */
public class MagicFairytaleFilter extends MagicLookupFilter{

	public MagicFairytaleFilter(Context context) {
		super(context, R.drawable.fairy_tale);
	}
	
	@Override
	public String toString() {
		return "Fairytale:童话";
	}
}
