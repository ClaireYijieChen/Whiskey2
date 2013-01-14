package org.kvj.whiskey2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.lima1.sync.QueryOperator;
import org.kvj.lima1.sync.SyncService;
import org.kvj.lima1.sync.SyncServiceInfo;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.template.DrawTemplate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

public class DataController {

	public interface DataControllerListener {

		public void dataChanged();

		public void noteChanged(NoteInfo info, boolean layoutChanged);
	}

	private static final String TAG = "DataController";
	private static final int REMOTE_WAIT_SECONDS = 30;
	protected static final String APP_NAME = "whiskey2";
	private Whiskey2App app = null;
	RemoteServiceConnector<SyncService> connector = null;
	private TemplateInfo defaultTemplate = new TemplateInfo();
	final Object connectorLock = new Object();
	private Integer[] colors = { R.drawable.note0, R.drawable.note1, R.drawable.note2, R.drawable.note3,
			R.drawable.note4, R.drawable.note5, R.drawable.note6, R.drawable.note7 };
	private int gridStep = 3;
	List<DataControllerListener> listeners = new ArrayList<DataController.DataControllerListener>();
	Map<Long, List<BookmarkInfo>> bookmarks = new HashMap<Long, List<BookmarkInfo>>();
	Map<Long, TemplateInfo> templates = new HashMap<Long, TemplateInfo>();
	DrawTemplate drawTemplate = null;
	protected String lastToken = null;

	BroadcastReceiver syncDone = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String appName = intent.getStringExtra(SyncServiceInfo.KEY_APP);
			if (!APP_NAME.equals(appName)) { // Skip
				return;
			}
			String result = intent.getStringExtra(SyncServiceInfo.KEY_RESULT);
			String token = intent.getStringExtra(SyncServiceInfo.KEY_TOKEN);
			if (null == result) { // Done
				SuperActivity.notifyUser(context, "Sync done");
				afterSync();
				if (token.equals(lastToken)) { // Expected
					notifyDataChanged();
				}
			} else {
				SuperActivity.notifyUser(context, result);
			}
		}
	};

	public DataController(Whiskey2App whiskey2App) {
		this.app = whiskey2App;
		drawTemplate = new DrawTemplate(this);
		startConnector();
		app.registerReceiver(syncDone, new IntentFilter(SyncServiceInfo.SYNC_FINISH_INTENT));
	}

	private void startConnector() {
		connector = new RemoteServiceConnector<SyncService>(app, SyncServiceInfo.INTENT, null) {

			@Override
			protected void onBeforeConnect(Intent intent) {
				intent.putExtra("application", APP_NAME);
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
			try {
				PJSONObject[] list = svc.query("templates", new QueryOperator[0], "tag, name", null);
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

	private void afterSync() {
		refreshBookmarks();
		refreshTemplates();
	}

	public String sync() {
		if (null == getRemote()) { // No connection
			return "No connection";
		}
		try {
			String id = getRemote().sync();
			if (null == id) { // Error
				return "Sync error";
			}
			lastToken = id;
			return null;
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
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return new Integer[0];
		}
		try { // Remote errors
			PJSONObject data = svc.getData();
			if (null != data) { // Have data
				JSONArray widths = data.optJSONArray("widths");
				Integer[] result = new Integer[widths.length()];
				for (int i = 0; i < result.length; i++) { // Copy
					result[i] = widths.getInt(i);
				}
				return result;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting widths from data:", e);
		}
		return new Integer[0];
	}

	public void notifyNoteChanged(NoteInfo note, boolean layoutChanged) {
		synchronized (listeners) { // Lock for modifications
			for (DataControllerListener l : listeners) { // Notify
				l.noteChanged(note, layoutChanged);
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
			if (null != info.files) {
				for (int i = 0; i < info.files.length(); i++) {
					svc.removeFile(info.files.optString(i, ""));
				}
			}
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

	public boolean removeLink(NoteInfo note, int index) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject noteObj = findOne("notes", note.id);
			if (null == noteObj) {
				Log.e(TAG, "Note not found");
				return false;
			}
			JSONArray links = noteObj.optJSONArray("links");
			if (null == links) {
				return true;
			}
			boolean changed = false;
			JSONArray newLinksArr = new JSONArray();
			if (links.length() > index) { // Have to remove
				for (int i = 0; i < links.length(); i++) {
					if (i == index) {
						changed = true;
					} else {
						newLinksArr.put(links.getJSONObject(i));
					}
				}
			}
			if (changed) { // Need update
				noteObj.put("links", newLinksArr);
				svc.update("notes", noteObj);
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error creating link:", e);
		}
		return false;
	}

	public String getFile(String file) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return null;
		}
		try {
			return svc.getFile(file);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean removeFile(String file) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try {
			return svc.removeFile(file);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	public DrawTemplate getDrawTemplate() {
		return drawTemplate;
	}

	public List<TemplateGroup> getTemplates() {
		Map<String, TemplateGroup> map = new LinkedHashMap<String, TemplateGroup>();
		TemplateGroup empty = new TemplateGroup();
		empty.title = "No tag";
		for (TemplateInfo info : templates.values()) { // Collect tags
			if (TextUtils.isEmpty(info.tag)) { // Have Tag
				empty.templates.add(info);
			} else { // Not empty
				TemplateGroup group = map.get(info.tag.toLowerCase());
				if (null == group) { // Create
					group = new TemplateGroup();
					group.title = info.tag;
					map.put(info.tag.toLowerCase(), group);
				}
				group.templates.add(info);
			}
		}
		map.put(null, empty);
		empty.templates.add(defaultTemplate);
		List<TemplateGroup> result = new ArrayList<TemplateGroup>();
		result.addAll(map.values());
		return result;
	}

	/**
	 * Get sheets; create new sheet, update last sheet, set next_id to new sheet
	 * 
	 * @param title
	 * @param template
	 * @param notebook
	 * @return
	 */
	public SheetInfo newSheet(String title, long template, long notebook) {
		List<SheetInfo> sheets = getSheets(notebook);
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return null;
		}
		try {
			PJSONObject newSheet = new PJSONObject();
			newSheet.put("title", title);
			newSheet.put("notepad_id", notebook);
			if (template > 0) { // Have template
				newSheet.put("template_id", template);
			}
			newSheet = svc.create("sheets", newSheet);
			if (null == newSheet) { // Error
				Log.e(TAG, "Sheet not created");
				return null;
			}
			SheetInfo info = SheetInfo.fromPJSON(newSheet);
			if (sheets.size() > 0) { // Have sheets
				SheetInfo last = sheets.get(sheets.size() - 1);
				last.origin.put("next_id", info.id);
				svc.update("sheets", last.origin);
			}
			return info;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean updateSheet(long id, String title, long template) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try {
			PJSONObject sheet = findOne("sheets", id);
			if (null == sheet) { // Not found
				Log.w(TAG, "Sheet not found");
				return false;
			}
			sheet.put("title", title);
			if (template > 0) { // Have template
				sheet.put("template_id", template);
			} else {
				if (sheet.has("template_id")) { // Reset
					sheet.remove("template_id");
				}
			}
			sheet = svc.update("sheets", sheet);
			if (null == sheet) { // Error
				Log.e(TAG, "Sheet not updated");
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeSheet(long id) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try {
			PJSONObject sheet = findOne("sheets", id);
			if (null == sheet) { // Not found
				Log.w(TAG, "Sheet not found");
				return true;
			}
			sheet = svc.removeCascade("sheets", sheet);
			if (null == sheet) { // Error
				Log.e(TAG, "Sheet not removed");
				return false;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean createSpotLink(NoteInfo note, String id) {
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return false;
		}
		try { // Remote and JSON errors
			PJSONObject noteObj = findOne("notes", note.id);
			if (null == noteObj) {
				Log.e(TAG, "Note not found");
				return false;
			}
			JSONArray links = noteObj.optJSONArray("links");
			if (null == links) {
				links = new JSONArray();
				noteObj.put("links", links);
			}
			boolean needed = true;
			for (int i = 0; i < links.length(); i++) {
				if (links.getJSONObject(i).optString("spot", "").equals(id)) {
					needed = false;
					return false;
				}
			}
			if (needed) { // Need update
				JSONObject obj = new JSONObject();
				obj.put("spot", id);
				links.put(obj);
				svc.update("notes", noteObj);
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error creating link:", e);
		}
		return false;
	}
}
