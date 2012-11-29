package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SheetListDecorator {

	protected static final String TAG = "SheetDecorator";

	public static void decorate(final ListPageSelector selector) {
		selector.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					selector.collapseExpand(false);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					selector.collapseExpand(true);
					break;
				}
				return false;
			}
		});
	}

	public static void decorateItem(final SheetsAdapter sheetsAdapter, View view, final int position) {
		view.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					// Log.i(TAG, "Drag start: " + position);
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					// Select me
					// Log.i(TAG, "Drag enter: " + position);
					sheetsAdapter.getSelector().notifyPageSelected(position, sheetsAdapter.getItemId(position));
					break;
				}
				return false;
			}
		});
	}
}
