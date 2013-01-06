package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.adapters.BookmarksAdapter;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.DragType;
import org.kvj.whiskey2.widgets.v11.NoteDnDDecorator.NoteDnDInfo;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.widget.ListView;

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
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					selector.collapseExpand(true);
					return true;
				}
				return false;
			}
		});
	}

	@SuppressLint("NewApi")
	public static void decorateItem(final SheetsAdapter sheetsAdapter, final View view, final int position) {
		final Drawable normalBackground = view.getContext().getResources()
				.getDrawable(android.R.drawable.list_selector_background);
		view.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_ENTERED:
					view.setBackgroundResource(R.drawable.sheet_item_dnd_target);
					break;
				case DragEvent.ACTION_DRAG_EXITED:
					view.setBackground(normalBackground);
					break;
				case DragEvent.ACTION_DRAG_STARTED:
					// Log.i(TAG, "Drag start: " + position);
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DROP:
					SheetInfo sheet = sheetsAdapter.getItem(position);
					sheetsAdapter.getSelector().notifyPageSelected(position, sheet.id);
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dnDInfo = (NoteDnDInfo) event.getLocalState();
						if (dnDInfo.dragType != DragType.Move) { // Not move
							return false;
						}
						for (NoteInfo note : dnDInfo.notes) {
							NoteInfo noteInfo = sheetsAdapter.getController().getNote(note.id);
							if (null == noteInfo) {
								Log.w(TAG, "Note not found: " + note.id);
								continue;
							}
							noteInfo.sheetID = sheet.id;
							sheetsAdapter.getController().saveNote(noteInfo);
						}
						sheetsAdapter.getSelector().collapseExpand(true);
						sheetsAdapter.getController().notifyNoteChanged(null, false);
						return true;
					}
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						if (sheetsAdapter.getController().moveBookmark(sheet, (BookmarkInfo) event.getLocalState())) {
							// Bookmark moved
							sheetsAdapter.getSelector().collapseExpand(true);
							sheetsAdapter.getController().notifyDataChanged();
						}
						return true;
					}
					break;
				}
				return false;
			}
		});
	}

	public static void decorateBookmarkSelector(ListView bookmarkSelector, BookmarksAdapter bookmarkAdapter,
			final ListPageSelector sheetSelector) {
		bookmarkSelector.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					if (event.getClipDescription().hasMimeType(BookmarkDnDDecorator.MIME_BMARK)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dnDInfo = (NoteDnDInfo) event.getLocalState();
						if (dnDInfo.dragType != DragType.Move) { // Not move
							return true;
						}
					}
					sheetSelector.collapseExpand(false);
					return true;
				}
				return false;
			}
		});
	}
}
