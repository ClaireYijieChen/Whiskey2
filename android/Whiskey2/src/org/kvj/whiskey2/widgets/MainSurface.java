package org.kvj.whiskey2.widgets;

import java.util.List;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter.SheetInfo;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainSurface extends RelativeLayout {

	private static final String TAG = "MainSurface";
	public static final int PAGE_MARGIN = 10;
	public static final int PAGES_GAP = 10;
	public static final int TEXT_PADDING = 1;
	private static final float TEXT_SIZE = 5;
	private static final float COLLPASED_HEIGHT = (float) 8.5;

	private boolean layoutCreated = false;
	private float density = 1;
	private float pageMargin;
	private float pagesGap;
	ViewGroup parent = null;
	private ListPageSelector selector = null;
	private int index = -1;
	private FragmentActivity activity;

	public MainSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
		density = getContext().getResources().getDisplayMetrics().density;
		pageMargin = density * PAGE_MARGIN;
		pagesGap = density * PAGES_GAP;
	}

	public void setController(int index, ListPageSelector selector, FragmentActivity fragmentActivity) {
		this.index = index;
		this.selector = selector;
		this.activity = fragmentActivity;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = parent.getMeasuredWidth();
		int height = parent.getMeasuredHeight();
		// Log.i(TAG, "On measure: " + getMeasuredWidth() + "x" +
		// getMeasuredHeight() + ", " + layoutCreated + ", "
		// + getParent() + ", " + parent.getMeasuredWidth() + "x" +
		// parent.getMeasuredHeight());
		if (!layoutCreated && width > 0 && height > 0) {
			createLayout(width, height);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		// Log.i(TAG, "attached to window - : " + getWidth() + ", " +
		// getHeight());
		parent = (ViewGroup) getParent();
	}

	private void createLayout(int width, int height) {
		layoutCreated = true;
		TemplateInfo template = selector.getController().getTemplate(0);
		// Log.i(TAG, "create layout: " + width + "x" + height);
		int pagesDisplayed = 1;
		float pageHeight = height - 2 * pageMargin;
		float pageWidth = pageHeight * template.width / template.height;
		// Log.i(TAG, "Calc page size pass 1: " + pageWidth + "x" + pageHeight +
		// ", " + width + "x" + height);
		if (pageWidth > width - 2 * pageMargin) { // Too wide
			pageWidth = width - 2 * pageMargin;
			// Recalculate height
			pageHeight = pageWidth * template.height / template.width;
		}
		// Log.i(TAG, "Calc page size pass 2: " + pageWidth + "x" + pageHeight +
		// ", " + density);
		float zoomFactor = template.width / pageWidth;
		if (zoomFactor > 0.1 * density) { //
			zoomFactor = (float) (0.1 * density);
			pageWidth = template.width / zoomFactor;
			pageHeight = template.height / zoomFactor;
		}
		int leftGap = 0;
		int left = (int) ((width - pageWidth) / 2);
		int top = (int) ((height - pageHeight) / 2);
		// Log.i(TAG, "Left x top: " + left + "x" + top);
		if (left < 0) { // Too big
			left = 0;
		}
		if (top < 0) { // Too big
			top = 0;
		}
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) pageWidth, (int) pageHeight);
		final PageSurface page = new PageSurface(getContext());
		page.title = selector.adapter.getItem(index).title;
		page.marginLeft = left;
		page.index = index;
		page.marginTop = top;
		page.zoomFactor = zoomFactor;
		params.setMargins(left, top, 0, 0);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		addView(page, params);
		page.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Log.i(TAG, "Click on page:");
				page.requestFocus();
			}
		});
		page.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Log.i(TAG, "On long page click: " + v.getLeft() + "x" + v.getTop());
				return false;
			}
		});
		// requestLayout();
		refresh(page);
	}

	private void refresh(final PageSurface page) {
		final SheetInfo sheet = selector.adapter.getItem(page.index);
		if (null == sheet) {
			return;
		}
		AsyncTask<Void, Void, List<NoteInfo>> task = new AsyncTask<Void, Void, List<NoteInfo>>() {

			@Override
			protected List<NoteInfo> doInBackground(Void... params) {
				List<NoteInfo> notes = selector.getController().getNotes(sheet.id);
				return notes;
			}

			@Override
			protected void onPostExecute(List<NoteInfo> result) {
				for (NoteInfo info : result) { // Create textview's
					TextView view = createNoteTextItem(page, info);
					info.widget = view;
				}
				page.notes.clear();
				page.notes.addAll(result);
				requestLayout();
			}
		};
		task.execute();
		setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.i(TAG, "Key event: " + keyCode);
				return false;
			}
		});
	}

	//
	// @Override
	// protected void onLayout(boolean changed, int l, int t, int r, int b) {
	// Log.i(TAG, "onLayout: " + toString() + ", " + l + "x" + t + " - " + r +
	// "x" + b);
	// super.onLayout(changed, l, t, r, b);
	// }

	protected TextView createNoteTextItem(final PageSurface page, final NoteInfo info) {
		final TextView textView = new TextView(getContext());
		int width = (int) (info.width / page.zoomFactor);
		int height = RelativeLayout.LayoutParams.WRAP_CONTENT;
		if (info.collapsible) { // Collapsible - collapse
			height = (int) (COLLPASED_HEIGHT / page.zoomFactor);
		}
		// Log.i(TAG, "Render text view: " + info.x + ": " + page.zoomFactor +
		// ", " + width);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.leftMargin = (int) (info.x / page.zoomFactor + page.marginLeft);
		params.topMargin = (int) (info.y / page.zoomFactor + page.marginTop);
		textView.setBackgroundResource(R.drawable.note0);
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
		textView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				Log.i(TAG, "Text long click");
				DialogFragment fragment = EditorDialogFragment.newInstance(info);
				fragment.show(activity.getSupportFragmentManager(), "editor");
				return true;
			}
		});
		textView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i(TAG, "Clicked: " + info.id + ", " + v + ", " + info.collapsible);
				// textView.bringToFront();
				// if (info.collapsible) { // Change state
				// RelativeLayout.LayoutParams params = (LayoutParams)
				// textView.getLayoutParams();
				// if (info.collapsed) { // Expand
				// params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
				// } else {
				// params.height = (int) (COLLPASED_HEIGHT / page.zoomFactor);
				// }
				// info.collapsed = !info.collapsed;
				// textView.requestLayout();
				// }
			}
		});
		return textView;
	}
}
