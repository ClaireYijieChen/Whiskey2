package org.kvj.whiskey2.widgets;

import java.util.ArrayList;
import java.util.List;

import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

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

	public ListPageSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		adapter = new SheetsAdapter(this);
		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				collapseExpand(true);
				notifyPageSelected(index, id);
			}
		});
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		density = getContext().getResources().getDisplayMetrics().density;
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		Log.i(TAG, "Focus changed: " + gainFocus);
		if (gainFocus) { // Got focus
			collapseExpand(false);
		}
	}

	public void collapseExpand(boolean collapse) {
		if (!collapsible) { // Not supported
			if (!collapse) { // Request focus
				requestFocus();
			}
			return;
		}
		collapsed = collapse;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
		if (collapse) { // Collapse
			params.width = (int) (collapsedWidth * density);
		} else {
			params.width = (int) (expandedWidth * density);
			requestFocus();
		}
		getParent().requestLayout();
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
