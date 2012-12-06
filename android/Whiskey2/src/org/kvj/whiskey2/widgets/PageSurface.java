package org.kvj.whiskey2.widgets;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.bravo7.SuperActivity;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.data.template.DrawTemplate;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class PageSurface extends View {

	class LinkInfo {
		float x1, y1, x2, y2;
		int color;
	}

	private static final float TITLE_FONT_SIZE = 5;
	private static final float TITLE_LEFT = 3;
	private static final float TITLE_TOP = 7;
	private static final float FONT_WIDTH = (float) 0.5;
	protected static final String TAG = "PageSurface";
	private static final float LINK_WIDTH = 1.2f;
	private static final float DOT_SIZE = 10;
	private static final float DOT_GAP = 2;
	int marginLeft = 0;
	int marginTop = 0;
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

	public PageSurface(Context context) {
		super(context);
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
		if (needLinksData) { // Render links
			try {
				for (NoteInfo note : notes) {
					if (null == note.links || note.links.length() == 0) {
						// No links
						continue;
					}
					LinearLayout toolbar = createLinksToolbar(note);
					for (int i = 0; i < note.links.length(); i++) {
						// Every link
						JSONObject link = note.links.getJSONObject(i);
						boolean linkOK = true;
						long linkID = link.optLong("id", -1);
						int index = findInNotes(linkID);
						if (index == -1) { // Not found
							linkOK = false;
						} else {
							NoteInfo other = notes.get(index);
							links.add(renderArrow(note, other, "#ffaaaa"));
						}
						createLinkButton(note, linkID, toolbar, linkOK);
					}
					Log.i(TAG, "Links found: " + links.size());
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			needLinksData = false;
		}
		float lineWidth = LINK_WIDTH / zoomFactor;
		for (LinkInfo link : links) { // Draw links
			linkPaint.setColor(link.color);
			linkPaint.setStrokeWidth(lineWidth);
			linkPaint.setStrokeCap(Cap.ROUND);
			canvas.drawLine(link.x1, link.y1, link.x2, link.y2, linkPaint);
		}
	}

	private void createLinkButton(final NoteInfo note, final long id, LinearLayout toolbar, boolean linkOK) {
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
				SuperActivity.notifyUser(getContext(), "Link: " + id);
			}
		});
		button.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				SuperActivity.showQuestionDialog(getContext(), "Remove link?", "Are you sure want to remove link?",
						new Runnable() {

							@Override
							public void run() {
								removeLink(note, id);
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

	private LinkInfo renderArrow(NoteInfo note1, NoteInfo note2, String color) {
		float lineWidth = LINK_WIDTH / zoomFactor;
		float gap = lineWidth;
		float b1x = note1.widget.getLeft() - marginLeft - gap;
		float b2x = note2.widget.getLeft() - marginLeft - gap;
		float b1w = note1.widget.getWidth() + 2 * gap;
		float b2w = note2.widget.getWidth() + 2 * gap;

		float b1y = note1.widget.getTop() - marginTop - gap;
		float b2y = note2.widget.getTop() - marginTop - gap;
		float b1h = note1.widget.getHeight() + 2 * gap;
		float b2h = note2.widget.getHeight() + 2 * gap;
		float x1 = b1x + b1w / 2;
		float x2 = b2x + b2w / 2;
		float y1 = b1y + b1h / 2;
		float y2 = b2y + b2h / 2;
		float x0 = x1 < x2 ? b2x : b2x + b2w;
		float y0 = y1 < y2 ? b2y : b2y + b2h;
		if (x1 == x2) { // vertival
			x0 = x1;
		} else if (y1 == y2) { // horizontal
			y0 = y1;
		} else {
			float a = (y2 - y1) / (x2 - x1);
			float b = y1 - x1 * a;
			float _y0 = x0 * a + b;
			if (b2y < _y0 && _y0 < b2y + b2h) { // Within sizes
				y0 = _y0;
			} else {
				x0 = (y0 - b) / a;
			}
		}
		LinkInfo info = new LinkInfo();
		info.x1 = x1;
		info.y1 = y1;
		info.x2 = x0;
		info.y2 = y0;
		info.color = Color.parseColor(color);
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

	public boolean removeLink(NoteInfo note, long linkID) {
		return false;
	}
}