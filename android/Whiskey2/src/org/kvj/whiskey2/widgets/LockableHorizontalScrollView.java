package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class LockableHorizontalScrollView extends HorizontalScrollView {

	private boolean locked = true;

	public LockableHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// if we can scroll pass the event to the superclass
			if (!locked)
				return super.onTouchEvent(ev);
			// only continue to handle the touch event if scrolling enabled
			return false; // mScrollable is always false at this point
		default:
			return super.onTouchEvent(ev);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		// Don't do anything with intercepted touch events if
		// we are not scrollable
		if (locked)
			return false;
		else
			return super.onInterceptTouchEvent(ev);
	}
}
