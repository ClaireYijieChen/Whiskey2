package org.kvj.whiskey2.widgets.v11;

import java.util.ArrayList;
import java.util.List;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.widgets.PageSurface;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Intent;
import android.os.Build;
import android.text.SpannableString;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NoteDnDDecorator {

	public enum DragType {
		Move, Link, Merge
	};

	public static class NoteDnDInfo {
		public List<NoteInfo> notes = new ArrayList<NoteInfo>();
		public int leftFix, topFix;
		public DragType dragType = DragType.Move;
	}

	static final String MIME_NOTE = "custom/note";
	protected static final String TAG = "NoteDnD";

	public static void decorate(final DataController controller, final View view, final NoteInfo note,
			final PageSurface surface) {
		final int originalBackground = controller.getBackgroundDrawable(note.color);
		view.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dndinfo = (NoteDnDInfo) event.getLocalState();
						if (dndinfo.dragType != DragType.Move) {
							// Linking or merging
							return true;
						}
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					NoteDnDInfo dndinfo = (NoteDnDInfo) event.getLocalState();
					switch (dndinfo.dragType) {
					case Link:
						view.setBackgroundResource(R.drawable.note_link_target);
						break;
					case Merge:
						view.setBackgroundResource(R.drawable.note_merge_target);
						break;
					default:
						break;
					}
					break;
				case DragEvent.ACTION_DRAG_ENDED:
				case DragEvent.ACTION_DRAG_EXITED:
					view.setBackgroundResource(originalBackground);
					break;
				case DragEvent.ACTION_DROP:
					dndinfo = (NoteDnDInfo) event.getLocalState();
					if (dndinfo.dragType == DragType.Link) { // Making link
						NoteInfo droppingNote = dndinfo.notes.get(0);
						if (controller.createLink(droppingNote, note)) { // Changed
							controller.notifyNoteChanged(note);
						}
					}
					break;
				}
				return false;
			}
		});
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				ClipData.Item item = new Item(new Intent());
				ClipData data = new ClipData(new SpannableString("Note"), new String[] { MIME_NOTE }, item);
				View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
				NoteDnDInfo dndinfo = new NoteDnDInfo();
				switch (note.touchedPoints) {
				case 2: // Make link
					dndinfo.dragType = DragType.Link;
					break;
				case 3: // Make merge
					dndinfo.dragType = DragType.Merge;
					break;
				}
				dndinfo.notes.add(note);
				dndinfo.leftFix = view.getWidth() / 2;
				dndinfo.topFix = view.getHeight() / 2;
				view.startDrag(data, shadow, dndinfo, 0);
				return true;
			}
		});

	}
}
