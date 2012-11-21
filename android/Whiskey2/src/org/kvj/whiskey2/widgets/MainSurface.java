package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public class MainSurface extends RelativeLayout {

	private static final String TAG = "MainSurface";

	public MainSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Log.i(TAG, "attached to window: " + getWidth() + ", " + getHeight());
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		Log.i(TAG, "measure: " + widthMeasureSpec + "x" + heightMeasureSpec);
	}

}
