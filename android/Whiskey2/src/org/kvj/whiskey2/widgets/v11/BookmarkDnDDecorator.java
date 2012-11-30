package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.BookmarkSign;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.os.Build;
import android.view.View;
import android.view.View.OnLongClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class BookmarkDnDDecorator {

	public static final String MIME_BMARK = "whiskey2/bmark";

	public static void decorate(final BookmarkSign view, SheetInfo sheet, final BookmarkInfo bmark) {
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {
				ClipData.Item item = new Item(bmark.name);
				ClipData data = new ClipData("Bookmark", new String[] { MIME_BMARK }, item);
				View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
				view.startDrag(data, shadow, bmark, 0);
				return true;
			}
		});
	}

}
