package org.kvj.whiskey2.widgets;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.SuperActivity;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.data.template.DrawTemplate;
import org.kvj.whiskey2.widgets.v11.PageDnDDecorator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class PageSurface extends View {

	class LinkInfo {
		float x1, y1, x2, y2, tx, ty;
		int color;
	}

	private static final float TITLE_FONT_SIZE = 5;
	private static final float TITLE_LEFT = 5;
	private static final float TITLE_TOP = 7;
	private static final float FONT_WIDTH = (float) 0.5;
	protected static final String TAG = "PageSurface";
	private static final float LINK_WIDTH = 1.2f;
	private static final float DOT_SIZE = 15;
	private static final float SPOT_SIZE = 20;
	private static final float DOT_GAP = 8;
	public int marginLeft = 0;
	public int marginTop = 0;
	float zoomFactor = 1;
	Paint paint = new Paint();
	Paint linkPaint = new Paint();
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
	private boolean linksDrawn = false;
	private boolean needLinksData = false;
	List<LinkInfo> links = new ArrayList<PageSurface.LinkInfo>();
	private LayoutInflater inflater = null;
	private Path linkPath = new Path();

	public void createHotspots() throws JSONException {
		if (null == sheetInfo.config) { // No spots
			return;
		}
		JSONArray spots = sheetInfo.config.optJSONArray("spots");
		if (null == spots) { // No spots
			return;
		}
		ViewGroup parent = (ViewGroup) getParent();
		for (int i = 0; i < spots.length(); i++) { // Create spots
			View button = new View(getContext());
			button.setFocusable(false);
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (density * SPOT_SIZE),
					(int) (density * SPOT_SIZE));
			params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			JSONObject spot = spots.getJSONObject(i);
			params.leftMargin = (int) (marginLeft + spot.optDouble("x", 0) / zoomFactor - density * SPOT_SIZE / 2);
			params.topMargin = (int) (marginTop + spot.optDouble("y", 0) / zoomFactor - density * SPOT_SIZE / 2);
			button.setBackgroundResource(R.drawable.spot_bg_dnd);
			parent.addView(button, params);
			decorateSpot(button, spot.optString("id", ""), parent);
		}
	}

	private void decorateSpot(View button, String id, ViewGroup parent) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			PageDnDDecorator.decorateSpot(this, button, id, parent);
		}
	}

	public PageSurface(Context context) {
		super(context);
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		paint.setAntiAlias(true);
		linkPaint.setAntiAlias(true);
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

	private Rect getBounds(NoteInfo info) {
		return new Rect(info.widget.getLeft() - marginLeft, info.widget.getTop() - marginTop, info.widget.getRight()
				- marginLeft, info.widget.getBottom() - marginTop);
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
		if (needLinksData) { // Render links
			try {
				JSONArray spots = sheetInfo.config != null ? sheetInfo.config.optJSONArray("spots") : null;
				for (NoteInfo note : notes) {
					if (null == note.links || note.links.length() == 0) {
						// No links
						continue;
					}
					LinearLayout toolbar = createLinksToolbar(note);
					Rect note1 = null;
					Rect note2 = null;
					for (int i = 0; i < note.links.length(); i++) {
						// Every link
						JSONObject link = note.links.getJSONObject(i);
						boolean linkOK = true;
						long linkID = link.optLong("id", -1);
						int index = findInNotes(linkID);
						if (index == -1) { // Not found
							linkOK = false;
							String spot = link.optString("spot", null);
							if (null != spots && null != spot) {
								// Search for spot
								for (int j = 0; j < spots.length(); j++) {
									// Search for spot
									JSONObject spotObject = spots.getJSONObject(j);
									if (spot.equals(spotObject.optString("id", null))) {
										// Found spot
										note1 = getBounds(note);
										int x = (int) (spotObject.optDouble("x", 0) / zoomFactor);
										int y = (int) (spotObject.optDouble("y", 0) / zoomFactor);
										note2 = new Rect(x, y, x, y);
										linkOK = true;
									}
								}
							}
						} else {
							note1 = getBounds(note);
							NoteInfo other = notes.get(index);
							note2 = getBounds(other);
						}
						if (linkOK) { // Have link
							links.add(renderArrow(note1, note2,
									getContext().getResources().getColor(R.color.note_link_color)));
						}
						createLinkButton(note, i, toolbar, linkOK);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			needLinksData = false;
		}
		for (LinkInfo link : links) { // Draw links
			linkPaint.setColor(link.color);
			linkPath.reset();
			linkPath.moveTo(link.x1 + link.tx / zoomFactor, link.y1 + link.ty / zoomFactor);
			linkPath.lineTo(link.x1 - link.tx / zoomFactor, link.y1 - link.ty / zoomFactor);
			linkPath.lineTo(link.x2, link.y2);
			canvas.drawPath(linkPath, linkPaint);
		}
	}

	private void createLinkButton(final NoteInfo note, final int index, LinearLayout toolbar, boolean linkOK) {
		View button = new View(getContext());
		button.setFocusable(false);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) (density * DOT_SIZE),
				(int) (density * DOT_SIZE));
		params.bottomMargin = (int) (density * DOT_GAP);
		button.setBackgroundResource(linkOK ? R.drawable.link_button_bg_ok : R.drawable.link_button_bg_ng);
		toolbar.addView(button, params);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				SuperActivity.notifyUser(getContext(), "Link: " + index);
			}
		});
		button.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				SuperActivity.showQuestionDialog(getContext(), "Remove link?", "Are you sure want to remove link?",
						new Runnable() {

							@Override
							public void run() {
								removeLink(note, index);
							}
						});
				return true;
			}
		});
	}

	private LinearLayout createLinksToolbar(NoteInfo note) {
		ViewGroup parent = (ViewGroup) getParent();
		LinearLayout toolbar = (LinearLayout) inflater.inflate(R.layout.links_toolbar, parent, false);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

		params.leftMargin = (int) (note.x / zoomFactor + marginLeft - density * (DOT_SIZE + DOT_GAP));
		params.topMargin = (int) (note.y / zoomFactor + marginTop);
		parent.addView(toolbar, params);
		toolbar.setVisibility(View.GONE);
		note.linksToolbar = toolbar;
		return toolbar;
	}

	private LinkInfo renderArrow(Rect note1, Rect note2, int color) {
		float x1 = note1.exactCenterX();
		float x2 = note2.exactCenterX();
		float y1 = note1.exactCenterY();
		float y2 = note2.exactCenterY();
		boolean verticalTriangle = Math.abs(x2 - x1) > Math.abs(y2 - y1);
		float triangleSize = 2;
		LinkInfo info = new LinkInfo();
		if (verticalTriangle) {
			info.ty = triangleSize;
		} else {
			info.tx = triangleSize;
		}
		info.x1 = x1;
		info.y1 = y1;
		info.x2 = x2;
		info.y2 = y2;
		info.color = color;
		return info;
	}

	private int findInNotes(long id) {
		for (int i = 0; i < notes.size(); i++) {
			if (id == notes.get(i).id) { // Found
				return i;
			}
		}
		return -1;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (!linksDrawn) {
			linksDrawn = true;
			needLinksData = true;
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

	public boolean removeLink(NoteInfo note, int index) {
		return false;
	}

	public boolean createSpotLink(NoteInfo note, String id) {
		return false;
	}

	public TemplateInfo getTemplateInfo() {
		return templateInfo;
	}

	public SheetInfo getSheetInfo() {
		return sheetInfo;
	}
}