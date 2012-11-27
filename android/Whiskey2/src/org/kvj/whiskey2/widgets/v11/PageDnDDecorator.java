package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.MainSurface;
import org.kvj.whiskey2.widgets.PageSurface;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.NoteDnDInfo;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PageDnDDecorator {

	protected static final String TAG = "DnD";

	public PageDnDDecorator(final MainSurface main, final PageSurface surface, final SheetInfo info) {
		surface.setOnDragListener(new OnDragListener() {

			@SuppressWarnings("unchecked")
			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DROP:
					NoteDnDInfo dndInfo = (NoteDnDInfo) event.getLocalState();
					main.acceptDrop(surface, info, event.getX() - dndInfo.leftFix, event.getY() - dndInfo.topFix,
							dndInfo.notes);
					break;
				}
				return false;
			}
		});
	}
}
