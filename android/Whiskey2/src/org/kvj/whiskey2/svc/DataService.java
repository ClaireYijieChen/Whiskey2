package org.kvj.whiskey2.svc;

import org.kvj.bravo7.SuperService;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.DataController;

import android.app.Service;
import android.content.Intent;

public class DataService extends SuperService<DataController, Whiskey2App> {

	public DataService() {
		super(DataController.class, "Whiskey2");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return Service.START_STICKY;
	}
}
