package org.kvj.whiskey2;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.svc.DataService;
import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.ListPageSelector.PagesSelectorListener;
import org.kvj.whiskey2.widgets.adapters.NotebookListAdapter;
import org.kvj.whiskey2.widgets.adapters.NotebookListAdapter.NotebookInfo;
import org.kvj.whiskey2.widgets.adapters.PagesPagerAdapter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class NotepadActivity extends SherlockFragmentActivity implements ControllerReceiver<DataController> {

	protected static final String TAG = "NotepadActivity";
	ControllerConnector<Whiskey2App, DataController, DataService> conn = null;
	private DataController controller = null;
	NotebookListAdapter notebookListAdapter = null;
	ListPageSelector sheetSelector = null;
	ViewPager pager = null;
	PagesPagerAdapter pagerAdapter = null;
	private long selectedNotepadID = -1;
	private Long sheetID = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_notepad, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_sync: // Run sync
			sync();
			break;
		case R.id.menu_reload: // Reload selected notepad
			reloadNotepad(selectedNotepadID);
			break;
		}

		return true;
	}

	private void reloadNotepad(long id) {
		for (int i = 0; i < notebookListAdapter.getCount(); i++) { // Search
			NotebookInfo info = notebookListAdapter.getItem(i);
			if (id == info.id) { // Found
				notepadSelected(info);
				return;
			}
		}
		SuperActivity.notifyUser(this, "Not selected");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		startService(new Intent(this, DataService.class));
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notepad);
		Bundle data = SuperActivity.getData(this, savedInstanceState);
		if (data.containsKey("sheet")) { // Found sheet
			sheetID = data.getLong("sheet");
		}
		pager = (ViewPager) findViewById(R.id.notepad_pager);
		sheetSelector = (ListPageSelector) findViewById(R.id.notepad_sheets);
		pagerAdapter = new PagesPagerAdapter(getSupportFragmentManager(), pager);
		sheetSelector.addListener(new PagesSelectorListener() {

			@Override
			public void onPagesChanged() {
			}

			@Override
			public void onPageSelected(int position, long id) {
				pager.requestFocus();
			}
		});
		conn = new ControllerConnector<Whiskey2App, DataController, DataService>(this, this);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		notebookListAdapter = new NotebookListAdapter(this);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setListNavigationCallbacks(notebookListAdapter, new OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				notepadSelected(notebookListAdapter.getItem(itemPosition));
				return true;
			}
		});
	}

	private void notepadSelected(NotebookInfo info) {
		Log.i(TAG, "Notepad selected: " + info);
		selectedNotepadID = info.id;
		sheetSelector.update(controller, info.id, sheetID);
	}

	private void sync() {
		final ProgressDialog progress = SuperActivity.showProgressDialog(this, "Sync...");
		AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {

			@Override
			protected String doInBackground(Void... params) {
				return controller.sync();
			}

			@Override
			protected void onPostExecute(String result) {
				if (null != result) { // Error
					SuperActivity.notifyUser(getApplicationContext(), result);
				}
				progress.dismiss();
				reloadNotepad(selectedNotepadID);
			}

		};
		task.execute();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (pager.getCurrentItem() != -1) { // Have item
			outState.putLong("sheet", sheetSelector.adapter.getItem(pager.getCurrentItem()).id);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		conn.connectController(DataService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		conn.disconnectController();
	}

	@Override
	public void onController(DataController controller) {
		if (null == this.controller) { // Just connected
			this.controller = controller;
			pagerAdapter.setSelector(sheetSelector);
			notebookListAdapter.update(controller);
		}
	}

	@Override
	public void onBackPressed() {
		if (sheetSelector.collapsed) { // Expand
			if (sheetSelector.requestFocus()) { // Focused
				return;
			}
		}
		super.onBackPressed();
	}

}
