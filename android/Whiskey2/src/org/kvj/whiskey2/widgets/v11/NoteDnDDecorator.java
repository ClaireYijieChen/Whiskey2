package org.kvj.whiskey2.widgets.v11;

import java.util.ArrayList;
import java.util.List;

import org.kvj.whiskey2.data.NoteInfo;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.os.Build;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NoteDnDDecorator {

	public static class NoteDnDInfo {
		List<NoteInfo> notes = new ArrayList<NoteInfo>();
		int leftFix, topFix;
	}

	static final String MIME_NOTE = "custom/note";

	public NoteDnDDecorator(final TextView view, final NoteInfo note) {
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				ClipData.Item item = new Item(note.text);
				ClipData data = new ClipData("Note", new String[] { MIME_NOTE }, item);
				View.DragShadowBuilder shadow = new View.DragShadowBuilder(view);
				NoteDnDInfo dndinfo = new NoteDnDInfo();
				dndinfo.notes.add(note);
				dndinfo.leftFix = view.getWidth() / 2;
				dndinfo.topFix = view.getHeight() / 2;
				view.startDrag(data, shadow, dndinfo, 0);
				return true;
			}
		});
	}
}
