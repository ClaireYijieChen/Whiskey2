package org.kvj.whiskey2.data.template;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.widgets.PageSurface;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class DrawTemplate {

	protected DataController controller = null;
	protected Paint linePaint = new Paint();
	protected Paint textPaint = new Paint();
	protected float density = 1;
	private static final float FONT_WIDTH = (float) 0.5;

	public DrawTemplate(DataController controller) {
		this.controller = controller;
		density = Whiskey2App.getInstance().getResources().getDisplayMetrics().density;
		linePaint.setStyle(Style.STROKE);
		textPaint.setStyle(Style.FILL_AND_STROKE);
		textPaint.setStrokeWidth(density * FONT_WIDTH);
	}

	public void render(TemplateInfo tmpl, SheetInfo sheet, Canvas canvas, PageSurface page) throws JSONException {
		JSONArray arr = tmpl.config.optJSONArray("draw");
		if (null != arr) { // Have data to draw
			draw(arr, canvas, page.getZoomFactor());
		}
	}

	private void setLineParameters(JSONObject obj, Paint paint, Canvas canvas) {
		paint.setColor(Color.parseColor(obj.optString("color", "#000000")));
		paint.setStrokeWidth(density * obj.optInt("width", 1));
	}

	private void setTextParameters(JSONObject obj, Paint paint, Canvas canvas, float zoom) {
		paint.setColor(Color.parseColor(obj.optString("color", "#000000")));
	}

	public void draw(JSONArray data, Canvas canvas, float zoom) throws JSONException {
		for (int i = 0; i < data.length(); i++) { // Draw items
			JSONObject obj = data.getJSONObject(i);
			String type = obj.optString("type", "");
			if ("text".equals(type)) { // Draw text
			}
			if ("line".equals(type)) { // Draw line
				setLineParameters(obj, linePaint, canvas);
				canvas.drawLine((float) obj.optDouble("x1", 0) / zoom, (float) obj.optDouble("y1", 0) / zoom,
						(float) obj.optDouble("x2", 0) / zoom, (float) obj.optDouble("y2", 0) / zoom, linePaint);
			}
		}
	}
}
