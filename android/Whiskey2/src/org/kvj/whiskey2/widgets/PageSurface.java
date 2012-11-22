package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.View;

public class PageSurface extends View {

	private static final float TITLE_FONT_SIZE = 5;
	private static final float TITLE_LEFT = 3;
	private static final float TITLE_TOP = 7;
	private static final float FONT_WIDTH = 1;
	int marginLeft = 0;
	int marginTop = 0;
	float zoomFactor = 1;
	Paint paint = new Paint();
	private float density = 1;
	public int index = 0;
	String title = "";
	static int shadowGap = 2;
	static int borderSize = 2;

	public PageSurface(Context context) {
		super(context);
		density = getContext().getResources().getDisplayMetrics().density;
		setFocusable(true);
		setFocusableInTouchMode(true);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int width = getWidth();
		int height = getHeight();
		super.onDraw(canvas);
		paint.setShadowLayer(density * shadowGap, 2 * density * shadowGap, 2 * density * shadowGap, Color.BLACK);
		paint.setStrokeWidth(density * borderSize);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		float boxWidth = width - 2 * density * shadowGap;
		float boxHeight = height - 2 * density * shadowGap;
		canvas.drawRect(0, 0, boxWidth, boxHeight, paint);
		paint.setShadowLayer(0, 0, 0, 0);
		paint.setColor(Color.BLACK);
		paint.setStyle(Style.STROKE);
		canvas.drawRect(0, 0, boxWidth, boxHeight, paint);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setTextSize(TITLE_FONT_SIZE / zoomFactor);
		paint.setStrokeWidth(density * FONT_WIDTH);
		canvas.drawText(title, TITLE_LEFT / zoomFactor, TITLE_TOP / zoomFactor, paint);
	}
}
