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
import android.graphics.RectF;

public class DrawTemplate {

	protected DataController controller = null;
	protected Paint linePaint = new Paint();
	protected Paint textPaint = new Paint();
	protected float density = 1;
	private static final float FONT_SIZE_XXSMALLEST = (float) 2.0;
	private static final float FONT_SIZE_XSMALLEST = (float) 2.7;
	private static final float FONT_SIZE_SMALLEST = (float) 3.5;
	private static final float FONT_SIZE_SMALL = (float) 4.5;
	private static final float FONT_SIZE_NORMAL = (float) 5.5;
	private static final float FONT_SIZE_LARGE = (float) 6.5;
	private static final float FONT_SIZE_LARGEST = (float) 7.5;
	private static final float FONT_WIDTH = (float) 0.5;
	private static final float PI180 = (float) (Math.PI / 180.0);

	public DrawTemplate(DataController controller) {
		this.controller = controller;
		density = Whiskey2App.getInstance().getResources().getDisplayMetrics().density;
		linePaint.setStyle(Style.STROKE);
		linePaint.setAntiAlias(true);
		textPaint.setStyle(Style.FILL);
		textPaint.setStrokeWidth(density * FONT_WIDTH);
		textPaint.setAntiAlias(true);
	}

	public void render(TemplateInfo tmpl, SheetInfo sheet, Canvas canvas, PageSurface page) throws JSONException {

		JSONArray arr = null;
		if (null != sheet.config) { // Have config
			arr = sheet.config.optJSONArray("draw");
		}
		if (null == arr && null != tmpl.config) {
			// No draw inside sheet - use template
			arr = tmpl.config.optJSONArray("draw");
		}
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
		int fontSize = obj.optInt("size", 0);
		float fontPixel = FONT_SIZE_NORMAL;
		switch (fontSize) {
		case -4:
			fontPixel = FONT_SIZE_XXSMALLEST;
			break;
		case -3:
			fontPixel = FONT_SIZE_XSMALLEST;
			break;
		case -2:
			fontPixel = FONT_SIZE_SMALLEST;
			break;
		case -1:
			fontPixel = FONT_SIZE_SMALL;
			break;
		case 1:
			fontPixel = FONT_SIZE_LARGE;
			break;
		case 2:
			fontPixel = FONT_SIZE_LARGEST;
			break;
		}
		paint.setTextSize(fontPixel / zoom);
	}

	public void draw(JSONArray data, Canvas canvas, float zoom) throws JSONException {
		for (int i = 0; i < data.length(); i++) { // Draw items
			JSONObject obj = data.getJSONObject(i);
			String type = obj.optString("type", "");
			if ("text".equals(type)) { // Draw text
				setTextParameters(obj, textPaint, canvas, zoom);
				canvas.drawText(obj.optString("text", ""), (float) obj.optDouble("x", 0) / zoom,
						(float) obj.optDouble("y", 0) / zoom, textPaint);
			}
			if ("line".equals(type)) { // Draw line
				setLineParameters(obj, linePaint, canvas);
				canvas.drawLine((float) obj.optDouble("x1", 0) / zoom, (float) obj.optDouble("y1", 0) / zoom,
						(float) obj.optDouble("x2", 0) / zoom, (float) obj.optDouble("y2", 0) / zoom, linePaint);
			}
			if ("circle".equals(type)) { // Draw circle
				setLineParameters(obj, linePaint, canvas);
				canvas.drawCircle((float) obj.optDouble("x", 0) / zoom, (float) obj.optDouble("y", 0) / zoom,
						(float) obj.optDouble("r", 0) / zoom, linePaint);
			}
			if ("arc".equals(type)) { // Draw arc
				setLineParameters(obj, linePaint, canvas);
				float x = (float) obj.optDouble("x", 0) / zoom;
				float y = (float) obj.optDouble("y", 0) / zoom;
				float r = (float) obj.optDouble("r", 0) / zoom;
				RectF rect = new RectF(x - r, y - r, x + r, y + r);
				float a = (float) obj.optDouble("sa", 0);
				canvas.drawArc(rect, a, (float) obj.optDouble("ea", 0) - a, false, linePaint);
			}
			if ("move".equals(type)) { // Translate/scale/rotate
				JSONArray items = obj.optJSONArray("items");
				if (null != items && items.length() > 0) { // Have items
					canvas.save();
					if (obj.has("x") && obj.has("y")) { // Translate
						float x = (float) obj.optDouble("x", 0) / zoom;
						float y = (float) obj.optDouble("y", 0) / zoom;
						canvas.translate(x, y);
					}
					if (obj.has("a")) { // Rotate
						canvas.rotate((float) obj.optDouble("a", 0));
					}
					if (obj.has("z")) { // Scale
						float scale = (float) obj.optDouble("z", 0) / 100;
						canvas.scale(scale, scale);
					}
					draw(items, canvas, zoom);
					canvas.restore();
				}
			}
		}
	}
}
