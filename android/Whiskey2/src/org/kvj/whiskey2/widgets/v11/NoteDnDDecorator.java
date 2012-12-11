package org.kvj.whiskey2.widgets.v11;

import java.util.ArrayList;
import java.util.List;

import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.widgets.PageSurface;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Intent;
import android.os.Build;
import android.text.SpannableString;
import android.view.View;
import android.view.View.OnLongClickListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NoteDnDDecorator {

	public static class NoteDnDInfo {
		public List<NoteInfo> notes = new ArrayList<NoteInfo>();
		public int leftFix, topFix;
	}

	static final String MIME_NOTE = "custom/note";
	protected static final String TAG = "NoteDnD";

	public static void decorate(final DataController controller, final View view, final NoteInfo note,
			final PageSurface surface) {
		view.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				ClipData.Item item = new Item(new Intent());
				ClipData data = new ClipData(new SpannableString("Note"), new String[] { MIME_NOTE }, item);
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
