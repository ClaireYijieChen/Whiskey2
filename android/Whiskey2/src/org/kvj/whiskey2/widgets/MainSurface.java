package org.kvj.whiskey2.widgets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.kvj.bravo7.SuperActivity;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;
import org.kvj.whiskey2.widgets.v11.BookmarkDnDDecorator;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.NoteDnDInfo;
import org.kvj.whiskey2.widgets.v11.PageDnDDecorator;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainSurface extends RelativeLayout {

	public static interface OnPageZoomListener {
		public void onShow();
	}

	private static final String TAG = "MainSurface";
	public static final int PAGE_MARGIN = 10;
	public static final int TEXT_PADDING = 1;
	static final float ZOOM_STEP = 0.05f;
	private static final float TEXT_SIZE = 5;
	private static final float COLLPASED_HEIGHT = (float) 8.5;
	private static final float BOOKMARK_WIDTH = 6;
	private static final float BOOKMARK_GAP = 2;
	private static final int ICON_SIZE = 12;
	private static final int IMAGE_MARGIN = 6;
	private static final float ICON_MARGIN = 1;
	private static final float FILE_TEXT_SIZE = 4;
	private static final float SPIRAL_CENTER_WIDTH = 8;
	private static final float SPIRAL_RIGHT_WIDTH = 11;
	private static final float SPIRAL_RIGHT_GAP = 4;
	private static final float SPIRAL_LEFT_GAP = 8;
	private static final int SPIRAL_ITEM_HEIGHT = 9;
	protected static final int COVER_MARGIN = 8;
	private static final float MAX_TEXT_DPI = 20;
	private static final int TAG_FILE_PATH = R.id.tag_file_path;
	private static final int TAG_EXPANDED = R.id.tag_expanded;
	protected static final float ZOOM_BEST_FIT = -1;
	protected static final float ZOOM_FIT_PAGE = -2;
	protected static final float ZOOM_FIT_WIDTH = -3;
	private static final float BEST_FIT_DPI = 16;

	private boolean layoutCreated = false;
	private float density = 1;
	float zoom = 1.0f;

	private float pageMargin;
	ViewGroup parent = null;
	private SheetsAdapter adapter = null;
	private int index = -1;
	private FragmentActivity activity;
	private ViewGroup toolbar = null;
	private LayoutInflater inflater = null;
	protected NoteInfo currentNote = null;
	private int width = 0;
	private int height = 0;
	private OnPageZoomListener pageZoomListener = null;
	private boolean forceLayout = false;

	public MainSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		density = getContext().getResources().getDisplayMetrics().density;
		pageMargin = density * PAGE_MARGIN;
	}

	public void setController(int index, SheetsAdapter adapter, FragmentActivity fragmentActivity) {
		this.index = index;
		this.adapter = adapter;
		this.activity = fragmentActivity;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (width <= 0 || height <= 0) {
			width = parent.getMeasuredWidth();
			height = parent.getMeasuredHeight();
		}
		if (!layoutCreated && width > 0 && height > 0) {
			createLayout(false);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		parent = (ViewGroup) getParent();
	}

	private PageSurface createPage(int index, SheetInfo sheet, TemplateInfo template) {
		final PageSurface page = new PageSurface(getContext()) {
			@Override
			public boolean removeLink(NoteInfo note, long linkID) {
				if (adapter.getController().removeLink(note, linkID)) {
					adapter.getController().notifyNoteChanged(note, false);
					return true;
				}
				return false;
			}
		};
		page.setTemplateConfig(adapter.getController().getDrawTemplate());
		page.setTemplateInfo(template);
		page.setSheetInfo(sheet);
		page.index = index;
		page.title = sheet.title;
		return page;
	}

	private boolean render() {
		layoutCreated = true;
		currentNote = null;
		SheetInfo sheet = adapter.getItem(index);
		if (null == sheet) { // Invalid sheet - nothing to show
			return false;
		}
		TemplateInfo template = adapter.getController().getTemplate(sheet.templateID);
		final List<PageSurface> pages = new ArrayList<PageSurface>();
		pages.add(createPage(index, sheet, template));
		int contentWidth = sheet.getWidth(template);
		int contentHeight = sheet.getHeight(template);
		float pageHeight = height - 2 * pageMargin;
		float pageWidth = contentWidth * pageHeight / contentHeight + density * SPIRAL_RIGHT_WIDTH;
		if (pageWidth > width - 2 * pageMargin || zoom == ZOOM_FIT_WIDTH) {
			// Too wide
			pageWidth = width - 2 * pageMargin;
			// Recalculate height
			pageHeight = (pageWidth - density * SPIRAL_RIGHT_WIDTH) * contentHeight / contentWidth;
		}
		float _zoomFactor = (contentHeight / pageHeight);
		float textDPI = TEXT_SIZE / _zoomFactor / density;
		float maxZoomLimiter = 1f;
		if (textDPI > MAX_TEXT_DPI) { // Non zoomed text is too big
			maxZoomLimiter = textDPI / MAX_TEXT_DPI;
		}
		if (zoom == ZOOM_FIT_WIDTH || zoom == ZOOM_FIT_PAGE) {
			// No zoom/fix zoom
			zoom = 1f;
			maxZoomLimiter = 1f;
		}
		if (zoom == ZOOM_BEST_FIT) { // Fix text DPI
			zoom = textDPI / BEST_FIT_DPI;
		}
		final float zoomFactor = _zoomFactor * zoom * maxZoomLimiter;
		// Finally apply zoom
		pageWidth /= zoom * maxZoomLimiter;
		pageHeight /= zoom * maxZoomLimiter;
		int minTemplateHeight = template.height;
		SheetInfo otherSheet = adapter.getItem(index + 1);
		if (null != otherSheet) { // Not empty
			TemplateInfo otherTemplate = adapter.getController().getTemplate(otherSheet.templateID);
			float otherTemplateWidth = otherSheet.getWidth(otherTemplate) / zoomFactor;
			float otherTemplateHeight = otherSheet.getHeight(otherTemplate) / zoomFactor;
			if (2 * pageMargin + contentWidth / zoomFactor + density * SPIRAL_CENTER_WIDTH + otherTemplateWidth < width) {
				// Width is OK
				if (2 * pageMargin + otherTemplateHeight <= height) {
					// Height is OK also
					pageWidth = sheet.getWidth(template) / zoomFactor + density * SPIRAL_CENTER_WIDTH
							+ otherTemplateWidth;
					if (otherSheet.getHeight(otherTemplate) < minTemplateHeight) { // Smaller
						minTemplateHeight = otherSheet.getHeight(otherTemplate);
					}
					pages.add(createPage(index + 1, otherSheet, otherTemplate));
				}
			}
		}
		final int totalWidth = (int) pageWidth;
		final int totalHeight = (int) pageHeight;
		final int spiralHeight = minTemplateHeight;
		loadNotes(pages, new Runnable() {

			@Override
			public void run() {
				removeAllViews();
				int coverWidth = (int) (totalWidth + COVER_MARGIN * density);
				int coverHeight = (int) (totalHeight + COVER_MARGIN * density);
				int coverLeft = (width - coverWidth) / 2;
				int coverTop = (height - coverHeight) / 2;
				int leftFix = 0;
				int topFix = 0;
				if (coverLeft < 0) { // Fix
					leftFix = -coverLeft;
				}
				if (coverTop < 0) { // Fix
					topFix = -coverTop;
				}
				ImageView cover = new ImageView(getContext());
				cover.setBackgroundResource(R.drawable.notepad_bg);
				RelativeLayout.LayoutParams coverParams = new RelativeLayout.LayoutParams(coverWidth, coverHeight);
				coverParams.leftMargin = coverLeft + leftFix;
				coverParams.topMargin = coverTop + topFix;
				coverParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				coverParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				addView(cover, coverParams);

				float left = (width - totalWidth) / 2 + leftFix;
				View spiral = null;
				RelativeLayout.LayoutParams spiralParams = null;
				for (int i = 0; i < pages.size(); i++) {
					PageSurface page = pages.get(i);
					page.zoomFactor = zoomFactor;
					float top = (height - page.getTemplateInfo().height / page.zoomFactor) / 2 + topFix;
					renderPage(page, (int) left, (int) top);
					left += page.getSheetInfo().getWidth(page.getTemplateInfo()) / page.zoomFactor;
					if (i == 0) { // First page - render spiral
						spiral = new View(getContext());
						boolean center = pages.size() > 1;
						float spiralWidth = center ? SPIRAL_CENTER_WIDTH + SPIRAL_RIGHT_GAP + SPIRAL_LEFT_GAP
								: SPIRAL_RIGHT_WIDTH + SPIRAL_RIGHT_GAP;

						double _spiralHeight = spiralHeight / page.zoomFactor;
						double heightFix = density * SPIRAL_ITEM_HEIGHT;
						_spiralHeight = Math.floor(_spiralHeight / heightFix) * heightFix;
						// (int) (Math.floor(spiralHeight / page.zoomFactor /
						// SPIRAL_ITEM_HEIGHT) * SPIRAL_ITEM_HEIGHT);
						spiralParams = new RelativeLayout.LayoutParams((int) (density * spiralWidth),
								(int) _spiralHeight);
						spiralParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
						spiralParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
						spiralParams.leftMargin = (int) (left - density * SPIRAL_RIGHT_GAP);
						spiralParams.topMargin = (int) ((height - _spiralHeight) / 2 + topFix);
						spiral.setBackgroundResource(center ? R.drawable.spiral_center_bg : R.drawable.spiral_right_bg);

						left += SPIRAL_CENTER_WIDTH * density;
					}
				}
				addView(spiral, spiralParams);
				for (int i = 0; i < pages.size(); i++) {
					// Render page items (notes/bmarks)
					PageSurface page = pages.get(i);
					renderPageItems(page, i != 0);
				}
				toolbar = (ViewGroup) inflater.inflate(R.layout.float_note_toolbar, MainSurface.this, false);
				addToolbarButton(R.drawable.float_edit, new OnClickListener() {

					@Override
					public void onClick(View v) {
						if (null != currentNote) { // Edit note
							startEditor(currentNote);
						}
					}
				});
				toolbar.setVisibility(View.GONE);
				addView(toolbar);
			}
		});

		return true;
	}

	private void renderPageItems(final PageSurface page, boolean leftToolbar) {
		for (final NoteInfo info : page.notes) { // Create textview's
			View view = createNoteTextItem(page, info, leftToolbar);
			info.widget = view;
			decorateNoteView(view, info, page);
			view.setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// Down - record finger count
						info.touchedPoints = event.getPointerCount();
					}
					return false;
				}
			});
		}
		List<BookmarkInfo> bmarks = adapter.getController().getBookmarks(page.getSheetInfo().id);
		if (null != bmarks) { // Have bookmarks -> create
			int bmarkWidth = (int) (BOOKMARK_WIDTH / page.zoomFactor);
			int bmarkGap = (int) (BOOKMARK_GAP / page.zoomFactor);
			int leftBMark = (int) (page.getTemplateInfo().width / page.zoomFactor - bmarkGap - bmarkWidth);
			for (final BookmarkInfo info : bmarks) { // Create bookmarks
				RelativeLayout.LayoutParams bmarkParams = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				bmarkParams.topMargin = page.marginTop;
				bmarkParams.leftMargin = page.marginLeft + leftBMark;
				BookmarkSign sign = new BookmarkSign(getContext(), bmarkWidth, info.color);
				addView(sign, bmarkParams);
				leftBMark -= bmarkGap + bmarkWidth;
				decorateBookmark(page.getSheetInfo(), info, sign);
				sign.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						SuperActivity.notifyUser(getContext(), info.name);
					}
				});
			}
		}
	}

	private void renderPage(final PageSurface page, int left, int top) {
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) (page.getSheetInfo().getWidth(
				page.getTemplateInfo()) / page.zoomFactor), (int) (page.getSheetInfo()
				.getHeight(page.getTemplateInfo()) / page.zoomFactor));
		page.marginLeft = left;
		page.index = index;
		page.marginTop = top;
		params.setMargins(left, top, 0, 0);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		addView(page, params);
		page.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View arg0, boolean focus) {
				if (focus) { // Got focus - hide toolbar
					toolbar.setVisibility(GONE);
				}
			}
		});
		page.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i(TAG, "Click on page");
				onShowZoom();
				if (!page.isFocused()) { // Request focus
					page.requestFocus();
				}
			}
		});
		// requestLayout();
		decorate(page, page.getSheetInfo());
		page.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				float x = page.getLastDownX() * page.zoomFactor;
				float y = page.getLastDownY() * page.zoomFactor;
				// Log.i(TAG, "On long page click: " + x + "x" + y);
				NoteInfo info = new NoteInfo();
				info.x = adapter.getController().stickToGrid(x);
				info.y = adapter.getController().stickToGrid(y);
				info.sheetID = page.getSheetInfo().id;
				startEditor(info);
				return true;
			}
		});
	}

	private void loadNotes(final List<PageSurface> pages, final Runnable callback) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for (PageSurface page : pages) { //
					page.notes = adapter.getController().getNotes(page.getSheetInfo().id);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				callback.run();
			}
		};
		task.execute();

	}

	private void decorateBookmark(SheetInfo sheet, BookmarkInfo bmark, BookmarkSign sign) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			BookmarkDnDDecorator.decorate(sign, sheet, bmark);
		}
	}

	private void decorateNoteView(View view, NoteInfo info, PageSurface surface) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			NoteDnDDecorator.decorate(adapter.getController(), view, info, surface);
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed && forceLayout) { // Changed - re-layout
			width = parent.getWidth();
			height = parent.getHeight();
			forceLayout = false;
			render();
		}
	}

	void createLayout(boolean layoutChanged) {
		if (layoutChanged) { // (Re)calculate width, height
			forceLayout = true;
			return;
		}
		render();
	}

	private ImageButton addToolbarButton(int resID, OnClickListener listener) {
		ImageButton button = (ImageButton) inflater.inflate(R.layout.float_note_button, toolbar, false);
		button.setImageResource(resID);
		button.setOnClickListener(listener);
		toolbar.addView(button);
		return button;
	}

	private void decorate(PageSurface surface, SheetInfo info) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			PageDnDDecorator.decorate(this, surface, info);
		}

	}

	public void acceptDrop(PageSurface page, SheetInfo sheet, float x, float y, NoteDnDInfo dnDInfo) {
		// Log.i(TAG, "Accept drop: " + x + "x" + y + ", " + notes);

		// NoteInfo droppingNote = dnDInfo.notes.get(0);
		// // First check if we dropped on page
		// float dropX = page.marginLeft + x;
		// float dropY = page.marginTop + y;
		// for (NoteInfo note : page.notes) {
		// // Iterate and check coordinates
		// if (note.widget.getLeft() < dropX && dropX < note.widget.getRight()
		// && note.widget.getTop() < dropY
		// && dropY < note.widget.getBottom()) { // Within bounds
		// if (adapter.getController().createLink(droppingNote, note)) {
		// // Created link
		// adapter.getController().notifyNoteChanged(note);
		// return;
		// }
		// }
		// }
		//
		int noteX = adapter.getController().stickToGrid((x - dnDInfo.leftFix) * page.zoomFactor);
		int noteY = adapter.getController().stickToGrid((y - dnDInfo.topFix) * page.zoomFactor);

		for (NoteInfo note : dnDInfo.notes) {
			NoteInfo noteInfo = adapter.getController().getNote(note.id);
			if (null == noteInfo) {
				Log.w(TAG, "Note not found: " + note.id);
				continue;
			}
			noteInfo.sheetID = sheet.id;
			noteInfo.x = noteX;
			noteInfo.y = noteY;
			adapter.getController().saveNote(noteInfo);
		}
		adapter.getController().notifyNoteChanged(null, false);
	}

	private Bitmap decodeFile(File f, int width) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			o.inPurgeable = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			// Find the correct scale value. It should be the power of 2.
			if (o.outWidth <= 0 || width <= 0) { // Invalid width
				return null;
			}
			int scale = 1;
			while (o.outWidth / scale / 2 >= width) {
				scale *= 2;
			}
			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
		}
		return null;
	}

	private Point getBitmapSize(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			o.inPurgeable = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);
			return new Point(o.outWidth, o.outHeight);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void setImage(NoteInfo info, PageSurface page, String filePath, View parent, ImageView image) {
		final int margin = (int) (ICON_MARGIN / page.zoomFactor);
		boolean expanded = (Boolean) image.getTag(TAG_EXPANDED);
		final int width = expanded ? info.width - IMAGE_MARGIN : ICON_SIZE;
		int w = (int) (width / page.zoomFactor);
		image.setVisibility(VISIBLE);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, w);
		image.setLayoutParams(params);
		if (!"".equals(filePath)) { //
			File f = new File(filePath);
			Point size = getBitmapSize(f);
			if (null == size) {
				return;
			}
			float mul = (expanded ? size.x : Math.max(size.x, size.y)) / width;
			Bitmap b = decodeFile(f, (int) (width / page.zoomFactor));
			if (null == b) {
				return;
			}
			w = (int) (size.x / mul / page.zoomFactor);
			int h = (int) (size.y / mul / page.zoomFactor);
			params = new LinearLayout.LayoutParams(w, h);
			if (expanded) {
				params.bottomMargin = margin;
				params.gravity = Gravity.CENTER_HORIZONTAL;
			} else {
				params.rightMargin = margin;
				params.gravity = Gravity.LEFT;
			}
			image.setImageBitmap(b);
			image.setLayoutParams(params);
		}
	}

	private void createFiles(final PageSurface page, final NoteInfo info, final boolean expanded, ViewGroup root) {
		if (null == info.files || 0 == info.files.length()) {
			return;
		}
		final int margin = (int) (ICON_MARGIN / page.zoomFactor);
		final LinearLayout panel = (LinearLayout) root.findViewById(R.id.one_note_files);
		panel.setOrientation(expanded ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
		if (panel.getChildCount() == info.files.length()) {
			// Already created - change size
			for (int i = 0; i < info.files.length(); i++) {
				final String file = info.files.optString(i, "");
				View child = panel.getChildAt(i);
				if (file.endsWith(".jpg")) {
					String filePath = (String) child.getTag(TAG_FILE_PATH);
					// File - change size
					child.setTag(TAG_EXPANDED, expanded);
					if (null != filePath) { // Already loaded
						setImage(info, page, filePath, panel, (ImageView) child);
					}
				}
			}
			return;
		}

		panel.removeAllViews();
		for (int i = 0; i < info.files.length(); i++) {
			final String file = info.files.optString(i, "");
			final int index = i;
			View child = null;
			if (file.endsWith(".jpg")) {
				final int width = expanded ? info.width - IMAGE_MARGIN : ICON_SIZE;
				int w = (int) (width / page.zoomFactor);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, w);
				params.rightMargin = margin;
				final ImageView view = (ImageView) inflater.inflate(R.layout.one_note_image, panel, false);
				child = view;
				view.setTag(TAG_EXPANDED, expanded);
				AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

					@Override
					protected String doInBackground(Void... params) {
						return adapter.getController().getFile(file);
					}

					@Override
					protected void onPostExecute(String result) {
						if (null == result) { // Download failed
							result = "";
						}
						view.setTag(TAG_FILE_PATH, result);
						setImage(info, page, result, panel, view);
					}
				};
				view.setVisibility(GONE);
				panel.addView(view, params);
				task.execute();
			} else {
				final int width = ICON_SIZE;
				int w = (int) (width / page.zoomFactor);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, w);
				params.rightMargin = margin;
				TextView textView = (TextView) inflater.inflate(R.layout.one_note_file, panel, false);
				child = textView;
				String ext = "???";
				if (-1 != file.lastIndexOf('.')) {
					ext = file.substring(file.lastIndexOf('.'));
				}
				textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, FILE_TEXT_SIZE / page.zoomFactor);
				textView.setText(ext);
				panel.addView(textView, params);
			}
			child.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					new AlertDialog.Builder(getContext()).setIcon(android.R.drawable.ic_dialog_alert)
							.setTitle("Action").setMessage("What to do with file No  " + index + "?")
							.setPositiveButton("Open", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							}).setNeutralButton("Remove", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									NoteInfo current = adapter.getController().getNote(info.id);
									if (null == current || null == current.files) {
										SuperActivity.notifyUser(getContext(), "Invalid note");
										return;
									}
									JSONArray arr = new JSONArray();
									for (int j = 0; j < current.files.length(); j++) {
										if (!current.files.optString(j, "").equals(file)) {
											arr.put(current.files.optString(j, ""));
										}
									}
									current.files = arr;
									if (adapter.getController().saveNote(current)) {
										adapter.getController().removeFile(file);
										adapter.getController().notifyNoteChanged(current, false);
									} else {
										SuperActivity.notifyUser(getContext(), "Save failed");
										return;
									}
								}
							}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							}).show();
					return true;
				}
			});
		}
	}

	protected View createNoteTextItem(final PageSurface page, final NoteInfo info, final boolean leftToolbar) {
		final LinearLayout root = (LinearLayout) inflater.inflate(R.layout.one_note, this, false);
		final TextView text = (TextView) root.findViewById(R.id.one_note_text);
		root.setId((int) info.id);
		int width = (int) (info.width / page.zoomFactor);
		int height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		if (info.collapsible) { // Collapsible - collapse
			height = (int) (COLLPASED_HEIGHT / page.zoomFactor);
		}
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.leftMargin = (int) (info.x / page.zoomFactor + page.marginLeft);
		params.topMargin = (int) (info.y / page.zoomFactor + page.marginTop);
		root.setBackgroundResource(adapter.getController().getBackgroundDrawable(info.color));
		int textPadding = (int) (TEXT_PADDING / page.zoomFactor);
		root.setPadding(textPadding, textPadding, textPadding, textPadding);
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX, TEXT_SIZE / page.zoomFactor);
		root.setFocusable(true);
		root.setFocusableInTouchMode(true);
		SpannableStringBuilder _text = new SpannableStringBuilder(info.text);
		text.setText(_text);
		addView(root, params);
		root.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Log.i(TAG, "Click on note: " + info.collapsible);
				if (info.collapsible) { // Change state
					RelativeLayout.LayoutParams params = (LayoutParams) root.getLayoutParams();
					params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
					info.collapsed = false;
					root.setLayoutParams(params);
				}
				if (!root.hasFocus()) {
					root.requestFocus();
				}
			}
		});
		root.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// Log.i(TAG, "Focus changed: " + info.id + ", " + hasFocus);
				createFiles(page, info, hasFocus, root);
				if (null != info.linksToolbar) { // Have links toolbar
					if (hasFocus) { // Show and rais links toolbar
						info.linksToolbar.setVisibility(View.VISIBLE);
						info.linksToolbar.bringToFront();
					} else { // Hide links
						info.linksToolbar.setVisibility(View.GONE);
					}
				}
				if (hasFocus) { // Bring to front first
					root.bringToFront();
					RelativeLayout.LayoutParams toolbarParams = new LayoutParams(
							RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
					toolbarParams.addRule(RelativeLayout.ALIGN_TOP, root.getId());
					if (leftToolbar) { // Show toolbar to left
						// TODO: Fix position of toolbar for second page
						toolbarParams.addRule(RelativeLayout.RIGHT_OF, root.getId());
					} else {
						toolbarParams.addRule(RelativeLayout.RIGHT_OF, root.getId());
					}
					toolbar.setVisibility(VISIBLE);
					toolbar.setLayoutParams(toolbarParams);
					toolbar.bringToFront();
					currentNote = info;
				}
				if (info.collapsible && !hasFocus) { // Change state
					RelativeLayout.LayoutParams params = (LayoutParams) root.getLayoutParams();
					params.height = (int) (COLLPASED_HEIGHT / page.zoomFactor);
					info.collapsed = true;
					root.setLayoutParams(params);
				}
			}
		});
		createFiles(page, info, false, root);
		return root;
	}

	private void startEditor(NoteInfo info) {
		DialogFragment fragment = EditorDialogFragment.newInstance(info);
		fragment.show(activity.getSupportFragmentManager(), "editor");
	}

	public void acceptBookmarkDrop(SheetInfo sheet, BookmarkInfo bmark) {
		Log.i(TAG, "Drop bookmark: " + sheet.title + ", " + bmark.name);
		if (adapter.getController().moveBookmark(sheet, bmark)) {
			Log.i(TAG, "Saved OK");
			adapter.getController().notifyDataChanged();
		} else {
			Log.i(TAG, "Saved ERR");
		}
	}

	public void onShowZoom() {
		if (null != pageZoomListener) {
			pageZoomListener.onShow();
		}
	}

	public void setPageZoomListener(OnPageZoomListener pageZoomListener) {
		this.pageZoomListener = pageZoomListener;
	}
}
