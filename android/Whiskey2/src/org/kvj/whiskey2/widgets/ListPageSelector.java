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
		public void onPagesChanged(long notepadID);

		public void onPageSelected(int position, long id);
	}

	private List<PagesSelectorListener> listeners = new ArrayList<ListPageSelector.PagesSelectorListener>();

	private static final String TAG = "PageSelector";
	public SheetsAdapter adapter = null;
	public boolean collapsed = false;
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

	public void update(DataController controller, long notepadID, Long sheetID) {
		this.controller = controller;
		adapter.update(controller, notepadID, sheetID);
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		Log.i(TAG, "Focus changed: " + gainFocus);
		// if (gainFocus) { // Got focus
		collapseExpand(!gainFocus);
		// }
	}

	public void collapseExpand(boolean collapse) {
		if (!collapsible) { // Not supported
			return;
		}
		collapsed = collapse;
		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();
		if (collapse) { // Collapse
			params.width = (int) (collapsedWidth * density);
		} else {
			params.width = (int) (expandedWidth * density);
		}
		getParent().requestLayout();
	}

	public void addListener(PagesSelectorListener listener) {
		if (!listeners.contains(listener)) { // New
			listeners.add(listener);
		}
	}

	public void notifyPagesChanged(long notepadID) {
		// Log.i(TAG, "Notify pages changed: " + adapter.getCount() + ", " +
		// listeners.size());
		for (PagesSelectorListener l : listeners) { // Iterate and notify
			l.onPagesChanged(notepadID);
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
