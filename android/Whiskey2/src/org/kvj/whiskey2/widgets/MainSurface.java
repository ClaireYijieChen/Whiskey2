package org.kvj.whiskey2.widgets;

import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;
import org.kvj.whiskey2.widgets.v11.BookmarkDnDDecorator;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator;
import org.kvj.whiskey2.widgets.v11.PageDnDDecorator;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainSurface extends RelativeLayout {

	private static final String TAG = "MainSurface";
	public static final int PAGE_MARGIN = 15;
	public static final int PAGES_GAP = 10;
	public static final int TEXT_PADDING = 1;
	private static final float TEXT_SIZE = 5;
	private static final float COLLPASED_HEIGHT = (float) 8.5;
	private static final float BOOKMARK_WIDTH = 7;
	private static final float BOOKMARK_GAP = 2;

	private boolean layoutCreated = false;
	private float density = 1;
	private float pageMargin;
	private float pagesGap;
	ViewGroup parent = null;
	private SheetsAdapter adapter = null;
	private int index = -1;
	private FragmentActivity activity;
	private ViewGroup toolbar = null;
	RelativeLayout.LayoutParams toolbarParams = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT);
	private LayoutInflater inflater = null;
	protected NoteInfo currentNote = null;
	private int floatButtonSize;
	private int width = 0;
	private int height = 0;

	public MainSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		density = getContext().getResources().getDisplayMetrics().density;
		pageMargin = density * PAGE_MARGIN;
		pagesGap = density * PAGES_GAP;
		floatButtonSize = getContext().getResources().getDimensionPixelSize(R.dimen.float_note_button_size);
	}

	public void setController(int index, SheetsAdapter adapter, FragmentActivity fragmentActivity) {
		this.index = index;
		this.adapter = adapter;
		this.activity = fragmentActivity;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		// Log.i(TAG, "On measure: " + getMeasuredWidth() + "x" +
		// getMeasuredHeight() + ", " + layoutCreated + ", "
		// + getParent() + ", " + parent.getMeasuredWidth() + "x" +
		// parent.getMeasuredHeight());
		if (width <= 0 || height <= 0) {
			width = parent.getMeasuredWidth();
			height = parent.getMeasuredHeight();
		}
		if (!layoutCreated && width > 0 && height > 0) {
			createLayout();
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		// Log.i(TAG, "attached to window - : " + getWidth() + ", " +
		// getHeight());
		parent = (ViewGroup) getParent();
	}

	private void createViews(final PageSurface page, final SheetInfo sheet, TemplateInfo template) {
		int pagesDisplayed = 1;
		int contentWidth = template.width;
		for (int i = 0; i < page.notes.size(); i++) {
			// Check width of every note
			NoteInfo note = page.notes.get(i);
			int noteRight = note.x + note.width;
			if (noteRight > contentWidth) { // Wider
				contentWidth = noteRight;
			}
		}
		int contentHeight = template.height;
		float pageHeight = height - 2 * pageMargin;
		float pageWidth = pageHeight * contentWidth / contentHeight;
		// Log.i(TAG, "Calc page size pass 1: " + pageWidth + "x" + pageHeight +
		// ", " + width + "x" + height);
		if (pageWidth > width - 2 * pageMargin) { // Too wide
			pageWidth = width - 2 * pageMargin;
			// Recalculate height
			pageHeight = pageWidth * contentHeight / contentWidth;
		}
		// Log.i(TAG, "Calc page size pass 2: " + pageWidth + "x" + pageHeight +
		// ", " + density);
		float zoomFactor = contentWidth / pageWidth;
		if (zoomFactor > 0.1 * density) { //
			zoomFactor = (float) (0.1 * density);
			pageWidth = contentWidth / zoomFactor;
			pageHeight = contentHeight / zoomFactor;
		}
		int leftGap = 0;
		int left = (int) ((width - pageWidth) / 2);
		if (left < floatButtonSize) {
			left = (int) (width - floatButtonSize - pageWidth);
		}
		int top = (int) ((height - pageHeight) / 2);
		// Log.i(TAG, "Left x top: " + left + "x" + top);
		if (left < 0) { // Too big
			left = 0;
		}
		if (top < 0) { // Too big
			top = 0;
		}
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) pageWidth, (int) pageHeight);
		page.title = adapter.getItem(index).title;
		page.marginLeft = left;
		page.index = index;
		page.marginTop = top;
		page.zoomFactor = zoomFactor;
		params.setMargins(left, top, 0, 0);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		removeAllViews();
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
				page.requestFocus();
			}
		});
		// requestLayout();
		decorate(page, sheet);
		page.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				float x = page.getLastDownX() * page.zoomFactor;
				float y = page.getLastDownY() * page.zoomFactor;
				// Log.i(TAG, "On long page click: " + x + "x" + y);
				NoteInfo info = new NoteInfo();
				info.x = adapter.getController().stickToGrid(x);
				info.y = adapter.getController().stickToGrid(y);
				info.sheetID = sheet.id;
				startEditor(info);
				return true;
			}
		});
		for (NoteInfo info : page.notes) { // Create textview's
			TextView view = createNoteTextItem(page, info);
			info.widget = view;
			decorateNoteView(view, info);
		}
		List<BookmarkInfo> bmarks = adapter.getController().getBookmarks(sheet.id);
		if (null != bmarks) { // Have bookmarks -> create
			int bmarkWidth = (int) (BOOKMARK_WIDTH / page.zoomFactor);
			int bmarkGap = (int) (BOOKMARK_GAP / page.zoomFactor);
			int leftBMark = (int) (pageWidth - bmarkGap - bmarkWidth);
			for (final BookmarkInfo info : bmarks) { // Create bookmarks
				RelativeLayout.LayoutParams bmarkParams = new RelativeLayout.LayoutParams(
						RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				bmarkParams.topMargin = page.marginTop;
				bmarkParams.leftMargin = page.marginLeft + leftBMark;
				BookmarkSign sign = new BookmarkSign(getContext(), bmarkWidth, info.color);
				addView(sign, bmarkParams);
				leftBMark -= bmarkGap + bmarkWidth;
				decorateBookmark(sheet, info, sign);
				sign.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						SuperActivity.notifyUser(getContext(), info.name);
					}
				});
			}
		}
		toolbar = (ViewGroup) inflater.inflate(R.layout.float_note_toolbar, this, false);
		addToolbarButton(R.drawable.float_edit, new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null != currentNote) { // Edit note
					startEditor(currentNote);
				}
			}
		});
		toolbar.setVisibility(View.GONE);
		addView(toolbar, toolbarParams);
	}

	private void decorateBookmark(SheetInfo sheet, BookmarkInfo bmark, BookmarkSign sign) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			BookmarkDnDDecorator.decorate(sign, sheet, bmark);
		}
	}

	private void decorateNoteView(TextView view, NoteInfo info) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			new NoteDnDDecorator(view, info);
		}
	}

	void createLayout() {
		layoutCreated = true;
		currentNote = null;
		final SheetInfo sheet = adapter.getItem(index);
		if (null == sheet) {
			return;
		}
		final PageSurface page = new PageSurface(getContext());
		final TemplateInfo template = adapter.getController().getTemplate(0);
		AsyncTask<Void, Void, List<NoteInfo>> task = new AsyncTask<Void, Void, List<NoteInfo>>() {

			@Override
			protected List<NoteInfo> doInBackground(Void... params) {
				List<NoteInfo> notes = adapter.getController().getNotes(sheet.id);
				return notes;
			}

			@Override
			protected void onPostExecute(List<NoteInfo> result) {
				page.notes.addAll(result);
				createViews(page, sheet, template);
			}
		};
		task.execute();
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

	public void acceptDrop(PageSurface page, SheetInfo sheet, float x, float y, List<NoteInfo> notes) {
		// Log.i(TAG, "Accept drop: " + x + "x" + y + ", " + notes);
		int noteX = adapter.getController().stickToGrid(x * page.zoomFactor);
		int noteY = adapter.getController().stickToGrid(y * page.zoomFactor);

		for (NoteInfo note : notes) {
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
		adapter.getController().notifyNoteChanged(null);
	}

	protected TextView createNoteTextItem(final PageSurface page, final NoteInfo info) {
		final TextView textView = new TextView(getContext());
		textView.setId((int) info.id);
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
		textView.setBackgroundResource(adapter.getController().getBackgroundDrawable(info.color));
		int textPadding = (int) (TEXT_PADDING / page.zoomFactor);
		textView.setPadding(textPadding, textPadding, textPadding, textPadding);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, TEXT_SIZE / page.zoomFactor);
		textView.setFocusable(true);
		textView.setFocusableInTouchMode(true);
		textView.setText(info.text);
		addView(textView, params);
		textView.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// Log.i(TAG, "Focus changed: " + info.id + ", " + hasFocus);
				if (hasFocus) { // Bring to front first
					textView.bringToFront();
					toolbarParams.addRule(RelativeLayout.ALIGN_TOP, textView.getId());
					toolbarParams.addRule(RelativeLayout.RIGHT_OF, textView.getId());
					toolbar.setVisibility(VISIBLE);
					toolbar.bringToFront();
					toolbar.setLayoutParams(toolbarParams);
					currentNote = info;
				}
				if (info.collapsible) { // Change state
					RelativeLayout.LayoutParams params = (LayoutParams) textView.getLayoutParams();
					if (hasFocus) { // Expand
						params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
					} else {
						params.height = (int) (COLLPASED_HEIGHT / page.zoomFactor);
					}
					info.collapsed = !info.collapsed;
					textView.requestLayout();
				}
			}
		});
		return textView;
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
}
