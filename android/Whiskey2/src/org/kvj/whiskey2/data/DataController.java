package org.kvj.whiskey2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.lima1.sync.QueryOperator;
import org.kvj.lima1.sync.SyncService;
import org.kvj.lima1.sync.SyncServiceInfo;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.template.DrawTemplate;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DataController {

	public interface DataControllerListener {

		public void dataChanged();

		public void noteChanged(NoteInfo info);
	}

	private static final String TAG = "DataController";
	private static final int REMOTE_WAIT_SECONDS = 30;
	private Whiskey2App app = null;
	RemoteServiceConnector<SyncService> connector = null;
	private TemplateInfo defaultTemplate = new TemplateInfo();
	final Object connectorLock = new Object();
	private Integer[] colors = { R.drawable.note0, R.drawable.note1, R.drawable.note2, R.drawable.note3,
			R.drawable.note4, R.drawable.note5, R.drawable.note6, R.drawable.note7 };
	public static Integer[] widths = { 50, 75, 90, 125 };
	private int gridStep = 6;
	List<DataControllerListener> listeners = new ArrayList<DataController.DataControllerListener>();
	Map<Long, List<BookmarkInfo>> bookmarks = new HashMap<Long, List<BookmarkInfo>>();
	Map<Long, TemplateInfo> templates = new HashMap<Long, TemplateInfo>();
	Map<String, DrawTemplate> templateConfigs = new HashMap<String, DrawTemplate>();

	public DataController(Whiskey2App whiskey2App) {
		this.app = whiskey2App;
		templateConfigs.put("draw", new DrawTemplate(this));
		startConnector();
	}

	private void startConnector() {
		connector = new RemoteServiceConnector<SyncService>(app, SyncServiceInfo.INTENT, null) {

			@Override
			protected void onBeforeConnect(Intent intent) {
				intent.putExtra("application", "whiskey2");
			}

			@Override
			public SyncService castAIDL(IBinder binder) {
				return SyncService.Stub.asInterface(binder);
			}

			@Override
			public void onConnect() {
				Log.i(TAG, "Remote Service connected");
				refreshBookmarks();
				refreshTemplates();
				synchronized (connectorLock) { // Notify
					connectorLock.notifyAll();
				}
			}

			@Override
			public void onDisconnect() {
				Log.i(TAG, "Remote Service disconnected");
				super.onDisconnect();
			}
		};
		Log.i(TAG, "Connecting to remote service");
	}

	SyncService getRemote() {
		SyncService svc = connector.getRemote();
		if (null != svc) { // Connected
			return svc;
		}
		synchronized (connectorLock) { // Wait for startup
			try {
				Log.i(TAG, "Waiting for remoteService");
				connectorLock.wait(1000 * REMOTE_WAIT_SECONDS);
				Log.i(TAG, "Waiting for remoteService done");
			} catch (InterruptedException e) {
			}
		}
		return connector.getRemote();
	}

	public void refreshBookmarks() {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return;
		}
		synchronized (bookmarks) { // Lock bookmarks
			try { //
				PJSONObject[] list = svc.query("bookmarks", new QueryOperator[0], null, null);
				bookmarks.clear();
				for (PJSONObject obj : list) { // Fill structure
					BookmarkInfo info = BookmarkInfo.fromJSON(obj);
					List<BookmarkInfo> infos = bookmarks.get(info.sheetID);
					if (null == infos) { // New - create
						infos = new ArrayList<BookmarkInfo>();
						bookmarks.put(info.sheetID, infos);
					}
					infos.add(info);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error updating bookmarks:", e);
			}
		}
	}

	public void refreshTemplates() {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return;
		}
		synchronized (templates) { // Lock templates
			try { //
				PJSONObject[] list = svc.query("templates", new QueryOperator[0], null, null);
				templates.clear();
				for (PJSONObject obj : list) { // Fill structure
					TemplateInfo info = TemplateInfo.fromJSON(obj);
					templates.put(info.id, info);
				}
			} catch (Exception e) {
				Log.e(TAG, "Error getting templates:", e);
			}
		}
	}

	public String sync() {
		if (null == getRemote()) { // No connection
			return "No connection";
		}
		try {
			String result = getRemote().sync();
			if (null == result) { // Refresh bookmarks
				refreshBookmarks();
				refreshTemplates();
			}
			return result;
		} catch (RemoteException e) {
			Log.e(TAG, "Error syncing:", e);
			return "Application error";
		}
	}

	private int findWith(List<PJSONObject> list, String attr, long value) {
		if (value == -1) { // Invalid value
			return -1;
		}
		for (int i = 0; i < list.size(); i++) {
			PJSONObject obj = list.get(i);
			if (obj.has(attr) && obj.optLong(attr, -1) == value) { // Found
				return i;
			}
		}
		return -1;
	}

	private void sortWithNext(List<PJSONObject> list) {
		int i = 0;
		while (i < list.size()) {
			PJSONObject item = list.get(i);
			int index = findWith(list, "id", item.optLong("next_id", -1));
			if (index != -1) { // Found
				PJSONObject found = list.get(index);
				if (index < i) { // Left
					list.add(i + 1, found);
					list.remove(index);
					i--;
				} else {
					if (index > i + 1) { // Right too far
						list.remove(index);
						list.add(i + 1, found);
					}
				}
			}
			i++;
		}
	}

	public List<PJSONObject> getNotebooks() {
		List<PJSONObject> result = new ArrayList<PJSONObject>();
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return result;
		}
		try { // DB errors
			PJSONObject[] objects = svc.query("notepads", null, null, null);
			for (PJSONObject obj : objects) { // Copy to list
				result.add(obj);
			}
			sortWithNext(result);
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

	public List<SheetInfo> getSheets(long notepadID) {
		List<SheetInfo> result = new ArrayList<SheetInfo>();
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return result;
		}
		try { // DB errors
			PJSONObject[] objects = svc.query("sheets",
					new QueryOperator[] { new QueryOperator("notepad_id", notepadID) }, null, null);
			List<PJSONObject> toSort = new ArrayList<PJSONObject>();
			for (PJSONObject obj : objects) { // Copy to list
				toSort.add(obj);
			}
			sortWithNext(toSort);
			for (PJSONObject pjsonObject : toSort) { // Convert to SheetInfo
				result.add(SheetInfo.fromPJSON(pjsonObject));
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

	private PJSONObject findOne(String stream, long id) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return null;
		}
		try { // Remote and DB error
			PJSONObject[] objects = svc.query(stream, new QueryOperator[] { new QueryOperator("id", id) }, null, null);
			if (objects.length > 0) { // Found
				return objects[0];
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting object of type " + stream, e);
		}
		return null;
	}

	public NoteInfo getNote(long id) {
		PJSONObject obj = findOne("notes", id);
		try { // JSON errors
			if (null != obj) { // Not found
				return NoteInfo.fromJSON(obj);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public SheetInfo getSheet(long id) {
		PJSONObject obj = findOne("sheets", id);
		try { // JSON errors
			if (null != obj) { // Not found
				return SheetInfo.fromPJSON(obj);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public TemplateInfo getTemplate(long templateID) {
		synchronized (templates) {
			TemplateInfo info = templates.get(templateID);
			if (null != info) {
				return info;
			}
		}
		return defaultTemplate;
	}

	public List<NoteInfo> getNotes(long id) {
		List<NoteInfo> result = new ArrayList<NoteInfo>();
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return result;
		}
		try { // DB errors
			PJSONObject[] objects = svc.query("notes", new QueryOperator[] { new QueryOperator("sheet_id", id) }, null,
					null);
			for (PJSONObject obj : objects) { // Copy to list
				NoteInfo info = NoteInfo.fromJSON(obj);
				result.add(info);
			}
			Collections.sort(result, new Comparator<NoteInfo>() {

				@Override
				public int compare(NoteInfo lhs, NoteInfo rhs) {
					return lhs.y - rhs.y;
				}
			});
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

	public Integer[] getColors() {
		return colors;
	}

	public Integer[] getWidths() {
		return widths;
	}

	public void notifyNoteChanged(NoteInfo note) {
		synchronized (listeners) { // Lock for modifications
			for (DataControllerListener l : listeners) { // Notify
				l.noteChanged(note);
			}
		}

	}

	public void notifyDataChanged() {
		synchronized (listeners) { // Lock for modifications
			for (DataControllerListener l : listeners) { // Notify
				l.dataChanged();
			}
		}

	}

	public boolean saveNote(NoteInfo info) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject obj = info.toPJSON();
			svc.update("notes", obj);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error saving note:", e);
		}
		return false;
	}

	public boolean removeNote(NoteInfo info) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject obj = info.toPJSON();
			svc.removeCascade("notes", obj);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error removing note:", e);
		}
		return false;
	}

	public void addControllerListener(DataControllerListener listener) {
		synchronized (listeners) { // Lock for modifications
			if (!listeners.contains(listener)) { // Add
				listeners.add(listener);
			}
		}
	}

	public void removeControllerListener(DataControllerListener listener) {
		synchronized (listeners) { // Lock for modifications
			listeners.remove(listener);
		}
	}

	public int getBackgroundDrawable(int color) {
		if (color < colors.length) { // Within range
			return colors[color];
		}
		return colors[0];
	}

	public int stickToGrid(float value) {
		return Math.round(value / gridStep) * gridStep;
	}

	public List<BookmarkInfo> getBookmarks(long sheetID) {
		synchronized (bookmarks) { // Lock
			return bookmarks.get(sheetID);
		}
	}

	public boolean moveBookmark(SheetInfo sheet, BookmarkInfo bmark) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject sheetObject = findOne("sheets", sheet.id);
			PJSONObject bmarkObject = findOne("bookmarks", bmark.id);
			if (null == bmarkObject || null == sheetObject) { // Invalid data
				Log.e(TAG, "Object not found: " + sheetObject + ", " + bmarkObject);
				return false;
			}
			bmarkObject.put("sheet_id", sheet.id);
			svc.update("bookmarks", bmarkObject);
			refreshBookmarks();
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error saving note:", e);
		}
		return false;
	}

	public DrawTemplate getTemplateConfig(TemplateInfo info) {
		DrawTemplate tmplConfig = templateConfigs.get(info.type);
		return tmplConfig;
	}

	public boolean createLink(NoteInfo note, NoteInfo other) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		if (note.id == other.id) { // Same note
			Log.w(TAG, "Same note");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject noteObj = findOne("notes", note.id);
			PJSONObject otherObj = findOne("notes", other.id);
			if (null == noteObj || null == otherObj) {
				Log.e(TAG, "Note not found");
				return false;
			}
			if (noteObj.optLong("sheet_id", 0) != otherObj.optLong("sheet_id", 0)) {
				Log.e(TAG, "Different sheets");
				return false;
			}
			JSONArray links = noteObj.optJSONArray("links");
			if (null == links) {
				links = new JSONArray();
			}
			for (int i = 0; i < links.length(); i++) {
				if (links.getJSONObject(i).optLong("id", -1) == other.id) {
					// Already created
					Log.e(TAG, "Already created");
					return false;
				}
			}
			JSONObject obj = new JSONObject();
			obj.put("id", other.id);
			links.put(obj);
			noteObj.put("links", links);
			svc.update("notes", noteObj);
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error creating link:", e);
		}
		return false;
	}
}
