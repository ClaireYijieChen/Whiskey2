package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class LockableScrollView extends ScrollView {

	protected boolean scrollLocked = false;

	public LockableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (scrollLocked) { // Locked
			return false;
		} else {
			if (getChildAt(0).onTouchEvent(ev)) { // Got event
				return false;
			}
		}
		return super.onInterceptTouchEvent(ev);
	}

}
