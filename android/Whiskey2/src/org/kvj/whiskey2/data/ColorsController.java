package org.kvj.whiskey2.data;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.util.StateSet;

import com.actionbarsherlock.R;

public class ColorsController {

	private static int colorsTotal = 8;
	private static int[] colors = { 0xFFEFFAB4, 0xFFD1F2A5, 0xFFFFC48C, 0xFFFF9F80, 0xFFF56991, 0xFFFF9E9D, 0xFFFFF7BD,
			0xFFB9D7D9 };
	private static int[] colorBorders = { 0xFFAFBA84, 0xFF91C275, 0xFFCF844C, 0xFFBF5F40, 0xFFB52951, 0xFFBF5E5D,
			0xFFBFB77D, 0xFF799799 };
	private static final int borderSize = 1;
	private static final String TAG = "Colors";

	private List<Drawable> noteColors = new ArrayList<Drawable>();

	class NoteDrawable extends ShapeDrawable {

		private float density;
		private int color;
		private int borderColor;

		public NoteDrawable(int color, int borderColor, float density) {
			super(new RectShape());
			this.density = density;
			this.color = color;
			this.borderColor = borderColor;
		}

		@Override
		protected void onDraw(Shape shape, Canvas canvas, Paint paint) {
			super.onDraw(shape, canvas, paint);
			paint.setColor(color);
			paint.setStyle(Style.FILL);
			canvas.drawRect(0, 0, shape.getWidth(), shape.getHeight(), paint);
			paint.setStrokeWidth(borderSize * density);
			paint.setColor(borderColor);
			paint.setStyle(Style.STROKE);
			canvas.drawRect(0, 0, shape.getWidth(), shape.getHeight(), paint);
		}
	}

	public ColorsController(Context context) {
		final float density = context.getResources().getDisplayMetrics().density;
		for (int i = 0; i < colorsTotal; i++) { // Create colors
			StateListDrawable sld = new StateListDrawable();
			ShapeDrawable bg = new NoteDrawable(colors[i], colorBorders[i], density);
			ShapeDrawable bgSelected = new NoteDrawable(colors[i], context.getResources().getColor(
					R.color.abs__holo_blue_light), density);
			sld.addState(new int[] { android.R.attr.state_focused }, bgSelected);
			sld.addState(StateSet.WILD_CARD, bg);
			noteColors.add(sld);
		}
	}

	public int getColorCount() {
		return noteColors.size();
	}

	public Drawable getColor(int position) {
		if (position < noteColors.size()) { // Within range
			return noteColors.get(position);
		}
		return noteColors.get(0);
	}
}
