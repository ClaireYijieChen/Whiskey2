package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

public class PageMiniature extends View {

	private static final String TAG = "PMiniature";
	static int width = 160;
	static int height = 60;
	static int textMargin = 20;
	static int showGap = 20;
	static int textSize = 15;
	int charsToShow = 0;
	static int shadowGap = 2;
	Paint paint = new Paint();
	String title = "Long long long text";
	private float density = 1;

	public PageMiniature(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PageMiniature(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		density = getContext().getResources().getDisplayMetrics().density;
		int dWidth = (int) (width * density);
		int dHeight = (int) (height * density);
		setMeasuredDimension(dWidth, dHeight);
		charsToShow = title.length();
		paint.setTextSize(density * textSize);
		while (paint.measureText(title, 0, charsToShow) > (width - 2 * textMargin) * density) {
			charsToShow--;
		}
		// Log.i(TAG, "onMeasure: " + dWidth + "x" + dHeight + ", " +
		// charsToShow);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Log.i(TAG, "onDraw: " + getLeft() + "x" + getTop() + ": " + title);
		paint.setShadowLayer(density * shadowGap, density * shadowGap, density * shadowGap, Color.BLACK);
		paint.setStrokeWidth(2);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		float boxWidth = density * (width - 2 * shadowGap);
		float boxHeight = density * (height - 2 * shadowGap);
		canvas.drawRect(0, 0, boxWidth, boxHeight, paint);
		paint.setShadowLayer(0, 0, 0, 0);
		paint.setColor(Color.BLACK);
		paint.setStyle(Style.STROKE);
		canvas.drawRect(0, 0, boxWidth, boxHeight, paint);
		paint.setStrokeWidth(0);
		paint.setColor(Color.BLACK);
		canvas.drawText(title, 0, charsToShow, density * textMargin, density * (2 * textSize), paint);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		// Log.i(TAG, "onLayout[" + changed + "]: " + left + "x" + top + ", " +
		// right + "x" + bottom);
		super.onLayout(changed, left, top, right, bottom);
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
