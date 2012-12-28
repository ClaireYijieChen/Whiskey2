package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.MainSurface;
import org.kvj.whiskey2.widgets.PageSurface;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.DragType;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.NoteDnDInfo;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PageDnDDecorator {

	protected static final String TAG = "DnD";

	public static void decorate(final MainSurface main, final PageSurface surface, final SheetInfo info) {
		surface.setOnDragListener(new OnDragListener() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
				case DragEvent.ACTION_DRAG_ENTERED:
					// Log.i(TAG, "Drag start on page: " + info.title + ", " +
					// event.getAction());
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dndInfo = (NoteDnDInfo) event.getLocalState();
						if (dndInfo.dragType == DragType.Move) {
							// Single finger drag
							return true;
						}
					}
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DROP:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dndInfo = (NoteDnDInfo) event.getLocalState();
						main.acceptDrop(surface, info, event.getX(), event.getY(), dndInfo);
						return true;
					}
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						main.acceptBookmarkDrop(info, (BookmarkInfo) event.getLocalState());
						return true;
					}
					break;
				}
				return false;
			}
		});
	}
}
