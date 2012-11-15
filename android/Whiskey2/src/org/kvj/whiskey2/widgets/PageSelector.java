package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;

public class PageSelector extends ViewGroup {

	private static final String TAG = "PSelector";
	float density = 1;
	int minHeight = 25;
	private TranslateAnimation tForward = null;
	private TranslateAnimation tBack = null;
	private int selectedIndex = -1;
	LockableScrollView scrollView = null;

	public PageSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		for (int i = 0; i < 25; i++) { // Add page miniatures
			PageMiniature miniature = new PageMiniature(context);
			miniature.setTitle("" + i + " title...");
			addView(miniature);
		}
		density = context.getResources().getDisplayMetrics().density;
		float moveSize = PageMiniature.width - PageMiniature.showGap;
		tForward = new TranslateAnimation(0, density * moveSize, 0, 0);
		tForward.setFillAfter(true);
		tForward.setDuration(200);
		tBack = new TranslateAnimation(0, 0, 0, 0);
		tBack.setFillAfter(true);
		tBack.setDuration(0);
		setScrollContainer(true);
		setScrollBarStyle(SCROLLBARS_INSIDE_INSET);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (getParent() instanceof LockableScrollView) { //
			scrollView = (LockableScrollView) getParent();
		}
	}

	private int findChild(float x, float y) {
		for (int i = getChildCount() - 1; i >= 0; i--) {
			View child = getChildAt(i);
			// Log.i(TAG, "findChild: " + i + ", " + child.getLeft() + "x" +
			// child.getTop() + " - " + child.getRight()
			// + "x" + child.getBottom() + " = " + x + "x" + y);
			if (child.getLeft() < x && child.getTop() < y && child.getRight() > x && child.getBottom() > y) { // Found
				return i;
			}
		}
		return -1;
	}

	private void stopAnimation(int index) {
		getChildAt(index).startAnimation(tBack);
	}

	private boolean indexChanged(int index) {
		Log.i(TAG, "Index changed: " + index + ", " + selectedIndex + ", " + scrollView);
		if (-1 == index) { // Out of items
			if (-1 != selectedIndex) { // Was selected
				stopAnimation(selectedIndex);
				if (null != scrollView) { // Have scrollView
					scrollView.scrollLocked = false;
				}
				selectedIndex = -1;
				return true;
			}
			return false;
		} else {
			if (-1 != selectedIndex) { // Was selected
				stopAnimation(selectedIndex);
			} else {
				// Begin
				if (null != scrollView) { // Have scrollView
					scrollView.scrollLocked = true;
				}
			}
			// Smth selected
			if (-1 == selectedIndex || selectedIndex != index) { // Changed
				Log.i(TAG, "Starting animation");
				tForward.reset();
				getChildAt(index).startAnimation(tForward);
				selectedIndex = index;
				return true;
			}
			return false;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float x = event.getX();
		float y = event.getY() + getScrollY();
		// Log.i(TAG, "Touch start: " + x + ", " + y + ": " + index);
		if (MotionEvent.ACTION_UP == event.getAction()) {
			return indexChanged(-1);
		}
		if (MotionEvent.ACTION_CANCEL == event.getAction() || MotionEvent.ACTION_OUTSIDE == event.getAction()) {
			return indexChanged(-1);
		}
		if (MotionEvent.ACTION_DOWN == event.getAction() || MotionEvent.ACTION_MOVE == event.getAction()) {
			int index = findChild(x, y);
			return indexChanged(index);
			// if (event.getY() > 600) { // Scroll
			// scrollBy(0, 10);
			// }
			// if (event.getY() < 50 && getScrollY() > 0) { // Scroll
			// scrollBy(0, -10);
			// }
		}
		return false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = PageMiniature.width;
		int height = PageMiniature.height;
		for (int i = 0; i < getChildCount(); i++) { // Add minHeight
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
			if (i < getChildCount() - 1) {
				height += minHeight;
			}
		}
		Log.i(TAG, "onMeasure: " + getChildCount() + ", " + width + "x" + height);
		setMeasuredDimension((int) (density * width), (int) (density * height));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int y = 0;
		Log.i(TAG, "onLayout[" + changed + "]: " + l + "x" + t + ", " + r + "x" + b);
		for (int i = 0; i < getChildCount(); i++) { // Go
			View child = getChildAt(i);
			int left = (int) ((PageMiniature.showGap - PageMiniature.width) * density);
			child.layout(left, y, left + child.getMeasuredWidth(), y + child.getMeasuredHeight());
			y += density * minHeight;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Log.i(TAG, "onDraw: " + getLeft() + "x" + getTop());
		super.onDraw(canvas);
	}
}
