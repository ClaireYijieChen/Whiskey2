package org.kvj.whiskey2.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class BookmarkSign extends View {

	private static final float DEFAULT_ZOOM = 15;
	private static final float DEFAULT_SHADOW = 1;
	private float zoom = 5;
	private float shadowSize = 1;
	private int color = Color.parseColor("#ff0000");
	private int borderColor = Color.parseColor("#88888888");
	private float density = 1;
	private Paint paint = new Paint();
	private Path path = null;

	public BookmarkSign(Context context, AttributeSet attrs) {
		super(context, attrs);
		density = context.getResources().getDisplayMetrics().density;
		this.zoom = density * DEFAULT_ZOOM;
		this.shadowSize = density * DEFAULT_SHADOW;
		init();
	}

	public BookmarkSign(Context context, float zoom, String color) {
		super(context);
		density = context.getResources().getDisplayMetrics().density;
		this.zoom = zoom;
		this.shadowSize = density * DEFAULT_SHADOW;
		this.color = Color.parseColor(color);
		init();
	}

	private void init() {
		path = new Path();
		path.moveTo(0, 0);
		path.lineTo(zoom, 0);
		path.lineTo(zoom, 2 * zoom);
		path.lineTo(zoom / 2, (float) (zoom * 1.5));
		path.lineTo(0, 2 * zoom);
		path.close();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		float width = zoom + shadowSize;
		float height = zoom * 2 + shadowSize;
		setMeasuredDimension((int) width, (int) height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		paint.setColor(borderColor);
		paint.setStrokeWidth(shadowSize);
		paint.setShadowLayer(shadowSize, shadowSize, shadowSize, borderColor);
		paint.setStyle(Style.STROKE);
		canvas.drawPath(path, paint);
		paint.setStyle(Style.FILL);
		paint.setColor(color);
		paint.setShadowLayer(0, 0, 0, 0);
		canvas.drawPath(path, paint);
	}
}
