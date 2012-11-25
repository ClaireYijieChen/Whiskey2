package org.kvj.whiskey2.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.lima1.sync.QueryOperator;
import org.kvj.lima1.sync.SyncService;
import org.kvj.lima1.sync.SyncServiceInfo;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.Whiskey2App;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DataController {

	private static final String TAG = "DataController";
	private static final int REMOTE_WAIT_SECONDS = 30;
	private Whiskey2App app = null;
	RemoteServiceConnector<SyncService> connector = null;
	private TemplateInfo defaultTemplate = new TemplateInfo();
	final Object connectorLock = new Object();
	private Integer[] colors = { R.drawable.note0, R.drawable.note0, R.drawable.note0, R.drawable.note0,
			R.drawable.note0,
			R.drawable.note0, R.drawable.note0, R.drawable.note0 };
	private Integer[] widths = { 50, 75, 90, 125 };

	public DataController(Whiskey2App whiskey2App) {
		this.app = whiskey2App;
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

	public String sync() {
		if (null == getRemote()) { // No connection
			return "No connection";
		}
		try {
			return getRemote().sync();
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

	public List<PJSONObject> getSheets(long notepadID) {
		List<PJSONObject> result = new ArrayList<PJSONObject>();
		SyncService svc = getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return result;
		}
		try { // DB errors
			PJSONObject[] objects = svc.query("sheets",
					new QueryOperator[] { new QueryOperator("notepad_id", notepadID) }, null, null);
			for (PJSONObject obj : objects) { // Copy to list
				result.add(obj);
			}
			sortWithNext(result);
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

	public TemplateInfo getTemplate(long templateID) {
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
				NoteInfo info = new NoteInfo();
				info.id = obj.getLong("id");
				info.color = obj.optInt("color");
				info.text = obj.optString("text", "");
				info.x = obj.optInt("x", 0);
				info.y = obj.optInt("y", 0);
				info.width = obj.optInt("width", 50);
				info.collapsible = obj.optBoolean("collapsed", false);
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
}
