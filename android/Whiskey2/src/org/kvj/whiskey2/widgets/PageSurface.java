package org.kvj.whiskey2.widgets;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.data.template.DrawTemplate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class PageSurface extends View {

	private static final float TITLE_FONT_SIZE = 5;
	private static final float TITLE_LEFT = 3;
	private static final float TITLE_TOP = 7;
	private static final float FONT_WIDTH = (float) 0.5;
	protected static final String TAG = "PageSurface";
	int marginLeft = 0;
	int marginTop = 0;
	float zoomFactor = 1;
	Paint paint = new Paint();
	private float density = 1;
	public int index = 0;
	String title = "";
	static int shadowGap = 2;
	static int borderSize = 2;
	List<NoteInfo> notes = new ArrayList<NoteInfo>();
	private float lastDownX = 0;
	private float lastDownY = 0;
	private SheetInfo sheetInfo = null;
	private TemplateInfo templateInfo = null;
	private DrawTemplate templateConfig = null;

	public PageSurface(Context context) {
		super(context);
		paint.setAntiAlias(true);
		density = getContext().getResources().getDisplayMetrics().density;
		setFocusable(true);
		setFocusableInTouchMode(true);
		setBackgroundResource(R.drawable.page);
		setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// Log.i(TAG, "Key handler: " + keyCode);
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						if (notes.size() > 0) {
							notes.get(0).widget.requestFocus();
							return true;
						}
					}
				}
				return false;
			}
		});
		// setOnFocusChangeListener(new OnFocusChangeListener() {
		//
		// @Override
		// public void onFocusChange(View v, boolean hasFocus) {
		// if (hasFocus) {
		// if (notes.size() > 0) {
		// notes.get(0).widget.requestFocus();
		// }
		// }
		// }
		// });
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// Record coordinates
			lastDownX = event.getX();
			lastDownY = event.getY();
		}
		return super.onTouchEvent(event);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int width = getWidth();
		int height = getHeight();
		super.onDraw(canvas);
		paint.setColor(Color.WHITE);
		paint.setStyle(Style.FILL);
		float boxWidth = width - 2 * density;
		float boxHeight = height - 2 * density;
		canvas.drawRect(density * 2, density * 2, boxWidth, boxHeight, paint);
		paint.setColor(Color.BLACK);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setTextSize(TITLE_FONT_SIZE / zoomFactor);
		paint.setStrokeWidth(density * FONT_WIDTH);
		canvas.drawText(title, TITLE_LEFT / zoomFactor, TITLE_TOP / zoomFactor, paint);
		if (null != templateConfig) { // Have template config - draw
		// canvas.translate(density * 2, density * 2);
			try {
				templateConfig.render(templateInfo, sheetInfo, canvas, this);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public float getLastDownX() {
		return lastDownX;
	}

	public float getLastDownY() {
		return lastDownY;
	}

	public float getZoomFactor() {
		return zoomFactor;
	}

	public void setSheetInfo(SheetInfo sheetInfo) {
		this.sheetInfo = sheetInfo;
	}

	public void setTemplateInfo(TemplateInfo templateInfo) {
		this.templateInfo = templateInfo;
	}

	public void setTemplateConfig(DrawTemplate templateConfig) {
		this.templateConfig = templateConfig;
	}
}
