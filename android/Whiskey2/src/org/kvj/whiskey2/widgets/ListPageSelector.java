package org.kvj.whiskey2.widgets;

import java.util.ArrayList;
import java.util.List;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;
import org.kvj.whiskey2.widgets.v11.SheetListDecorator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ListView;

public class ListPageSelector extends ListView {

	public static interface PagesSelectorListener {
		public void onPageSelected(int position, long id);
	}

	private List<PagesSelectorListener> listeners = new ArrayList<ListPageSelector.PagesSelectorListener>();

	private static final String TAG = "PageSelector";
	public SheetsAdapter adapter = null;
	public boolean collapsed = true;
	boolean collapsible = true;
	static int collapsedWidth = 30;
	static int expandedWidth = 150;
	float density = 1;

	private DataController controller = null;

	private int listWidth = 0;
	View pager = null;

	private AnimationSet listShowAnimation = null;

	public ListPageSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		listShowAnimation = new AnimationSet(true);
		Animation transform = new TranslateAnimation(-context.getResources().getDimensionPixelSize(
				R.dimen.sheets_list_width), 0, 0, 0);
		listShowAnimation.setDuration(150);
		listShowAnimation.setInterpolator(new AccelerateInterpolator(1f));
		AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
		listShowAnimation.addAnimation(transform);
		listShowAnimation.addAnimation(alpha);
		adapter = new SheetsAdapter(this);
		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				collapseExpand(true);
				notifyPageSelected(index, id);
			}
		});
		decorate(this);
	}

	public void setPager(View pager) {
		this.pager = pager;
	}

	private void decorate(ListPageSelector listPageSelector) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			SheetListDecorator.decorate(listPageSelector);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		density = getContext().getResources().getDisplayMetrics().density;
		listWidth = getContext().getResources().getDimensionPixelSize(R.dimen.sheets_list_width);
	}

	// @Override
	// protected void onFocusChanged(boolean gainFocus, int direction, Rect
	// previouslyFocusedRect) {
	// super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	// Log.i(TAG, "Focus changed: " + gainFocus);
	// if (gainFocus) { // Got focus
	// collapseExpand(false);
	// }
	// }
	//
	public void collapseExpand(boolean collapse) {
		if (!collapsible) { // Not supported
			if (!collapse) { // Request focus
				requestFocus();
			}
			return;
		}
		collapsed = collapse;
		// RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)
		// pager.getLayoutParams();
		if (collapse) { // Collapse
			// params.leftMargin = 0;
			clearAnimation();
			pager.bringToFront();
		} else {
			// params.leftMargin = listWidth;
			startAnimation(listShowAnimation);
			requestFocus();
			bringToFront();
		}
		// pager.setLayoutParams(params);
	}

	public void addListener(PagesSelectorListener listener) {
		if (!listeners.contains(listener)) { // New
			listeners.add(listener);
		}
	}

	public void notifyPageSelected(int position, long id) {
		for (PagesSelectorListener l : listeners) { // Iterate and notify
			l.onPageSelected(position, id);
		}
	}

	public DataController getController() {
		return controller;
	}

}
