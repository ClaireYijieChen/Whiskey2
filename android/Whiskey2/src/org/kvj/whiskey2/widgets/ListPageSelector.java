package org.kvj.whiskey2.widgets;

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

	private static final String TAG = "PageSelector";
	SheetsAdapter adapter = null;
	boolean collapsed = false;
	boolean collapsible = true;
	static int collapsedWidth = 50;
	static int expandedWidth = 150;
	float density = 1;

	public ListPageSelector(Context context, AttributeSet attrs) {
		super(context, attrs);
		adapter = new SheetsAdapter();
		setAdapter(adapter);
		setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				collapseExpand(true);
			}
		});
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		density = getContext().getResources().getDisplayMetrics().density;
	}

	public void update(DataController controller, long notepadID) {
		adapter.update(controller, notepadID);
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		Log.i(TAG, "Focus changed: " + gainFocus);
		if (gainFocus) { // Got focus
			collapseExpand(false);
		}
	}

	private void collapseExpand(boolean collapse) {
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

}
