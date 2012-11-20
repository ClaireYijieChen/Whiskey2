package org.kvj.whiskey2.data;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.ipc.RemoteServiceConnector;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.lima1.sync.QueryOperator;
import org.kvj.lima1.sync.SyncService;
import org.kvj.lima1.sync.SyncServiceInfo;
import org.kvj.whiskey2.Whiskey2App;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DataController {

	private static final String TAG = "DataController";
	private Whiskey2App app = null;
	RemoteServiceConnector<SyncService> connector = null;

	public DataController(Whiskey2App whiskey2App) {
		this.app = whiskey2App;
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
			}

			@Override
			public void onDisconnect() {
				Log.i(TAG, "Remote Service disconnected");
				super.onDisconnect();
			}
		};
		Log.i(TAG, "Connecting to remote service");
	}

	public String sync() {
		if (null == connector.getRemote()) { // No connection
			return "No connection";
		}
		try {
			return connector.getRemote().sync();
		} catch (RemoteException e) {
			Log.e(TAG, "Error syncing:", e);
			return "Application error";
		}
	}

	public List<PJSONObject> getNotebooks() {
		List<PJSONObject> result = new ArrayList<PJSONObject>();
		SyncService svc = connector.getRemote();
		if (null == svc) { // No connection
			Log.w(TAG, "No service");
			return result;
		}
		try { // DB errors
			PJSONObject[] objects = svc.query("notepads", null, null, null);
			for (PJSONObject obj : objects) { // Copy to list
				result.add(obj);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

	public List<PJSONObject> getSheets(long notepadID) {
		List<PJSONObject> result = new ArrayList<PJSONObject>();
		SyncService svc = connector.getRemote();
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
		} catch (Exception e) {
			Log.e(TAG, "Error getting notebooks:", e);
		}
		return result;
	}

}
