package org.kvj.whiskey2.widgets.v11;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
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
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NoteDnDDecorator {

	public enum DragType {
		Move, Link, Merge
	};

	public static class NoteDnDInfo {
		public static DragType defaultType = DragType.Move;
		public List<NoteInfo> notes = new ArrayList<NoteInfo>();
		public int leftFix, topFix;
		public DragType dragType = defaultType;
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
							controller.notifyNoteChanged(note, false);
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
				dndinfo.notes.add(note);
				dndinfo.leftFix = view.getWidth() / 2;
				dndinfo.topFix = view.getHeight() / 2;
				if (dndinfo.dragType == DragType.Link) { // Link
					try {
						surface.createHotspots();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				view.startDrag(data, shadow, dndinfo, 0);
				Log.i(TAG, "Note drag started");
				return true;
			}
		});

	}

	public static void decorateDndTargets(View root, final LinearLayout dndTargets) {
		root.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_ENTERED:
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						dndTargets.setVisibility(View.VISIBLE);
						return false;
					}
					break;
				case DragEvent.ACTION_DROP:
				case DragEvent.ACTION_DRAG_ENDED:
					dndTargets.setVisibility(View.INVISIBLE);
					return true;
				}
				return false;
			}
		});
		dndTargets.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DROP:
				case DragEvent.ACTION_DRAG_ENDED:
					dndTargets.setVisibility(View.INVISIBLE);
					return true;
				}
				return false;
			}
		});
		final int[] toggles = { R.id.dnd_move_target, R.id.dnd_link_target, R.id.dnd_merge_target };
		initToggleDndTarget(dndTargets, toggles[0], toggles, DragType.Move);
		initToggleDndTarget(dndTargets, toggles[1], toggles, DragType.Link);
		initToggleDndTarget(dndTargets, toggles[2], toggles, DragType.Merge);
		final ImageButton trash = (ImageButton) dndTargets.findViewById(R.id.dnd_trash_target);
		trash.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						trash.setBackgroundResource(R.drawable.float_button_checked);
						return true;
					}
					break;
				case DragEvent.ACTION_DROP:
					Log.i(TAG, "Drop on trash");
					return true;
				case DragEvent.ACTION_DRAG_EXITED:
				case DragEvent.ACTION_DRAG_ENDED:
					trash.setBackgroundResource(R.drawable.dnd_target_bg);
					return true;
				}
				return false;
			}
		});

	}

	private static void initToggleDndTarget(final LinearLayout dndTargets, int id, final int[] toggles,
			final DragType type) {
		final ImageButton button = (ImageButton) dndTargets.findViewById(id);
		button.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						NoteDnDInfo dndinfo = (NoteDnDInfo) event.getLocalState();
						if (dndinfo.dragType == type) { // Same
							button.setBackgroundResource(R.drawable.float_button_checked);
						}
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENTERED:
					if (event.getClipDescription().hasMimeType(NoteDnDDecorator.MIME_NOTE)) {
						for (int other : toggles) {
							dndTargets.findViewById(other).setBackgroundResource(R.drawable.dnd_target_bg);
						}
						button.setBackgroundResource(R.drawable.float_button_checked);
						NoteDnDInfo dndinfo = (NoteDnDInfo) event.getLocalState();
						NoteDnDInfo.defaultType = type;
						dndinfo.dragType = type;
						return true;
					}
					break;
				case DragEvent.ACTION_DRAG_ENDED:
					button.setBackgroundResource(R.drawable.dnd_target_bg);
					return true;
				}
				return false;
			}
		});
	}
}
