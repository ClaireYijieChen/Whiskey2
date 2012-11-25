package org.kvj.whiskey2;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.bundle.LongBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
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
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class NotepadActivity extends SherlockFragmentActivity implements ControllerReceiver<DataController> {

	public static final String KEY_NOTEPAD = "notepad_id";
	public static final String KEY_SHEET = "sheet_id";

	protected static final String TAG = "NotepadActivity";
	ControllerConnector<Whiskey2App, DataController, DataService> conn = null;
	private DataController controller = null;
	NotebookListAdapter notebookListAdapter = null;
	ListPageSelector sheetSelector = null;
	ViewPager pager = null;
	PagesPagerAdapter pagerAdapter = null;
	FormController form = null;
	TransientAdapter<Long> notepadID = null;
	TransientAdapter<Long> sheetID = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_notepad, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Options: " + item.getItemId());
		switch (item.getItemId()) {
		case android.R.id.home: // Run sync
			sheetSelector.collapseExpand(false);
			break;
		case R.id.menu_sync: // Run sync
			sync();
			break;
		case R.id.menu_reload: // Reload selected notepad
			reloadNotepad(form.getValue(KEY_NOTEPAD, Long.class));
			break;
		}

		return true;
	}

	private void reloadNotepad(long id) {
		for (int i = 0; i < notebookListAdapter.getCount(); i++) { // Search
			NotebookInfo info = notebookListAdapter.getItem(i);
			if (id == info.id) { // Found
				notepadSelected(i, info);
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
		pager = (ViewPager) findViewById(R.id.notepad_pager);
		form = new FormController(pager);
		notepadID = new TransientAdapter<Long>(new LongBundleAdapter(), -1L);
		form.add(notepadID, KEY_NOTEPAD);
		sheetID = new TransientAdapter<Long>(new LongBundleAdapter(), -1L);
		form.add(sheetID, KEY_SHEET);
		form.load(this, savedInstanceState);
		pager.setSaveEnabled(false);
		sheetSelector = (ListPageSelector) findViewById(R.id.notepad_sheets);
		// pagerAdapter = new PagesPagerAdapter(getSupportFragmentManager(),
		// pager);
		sheetSelector.addListener(new PagesSelectorListener() {

			@Override
			public void onPagesChanged(long notepadID) {
			}

			@Override
			public void onPageSelected(int position, long id) {
				pager.requestFocus();
			}
		});
		conn = new ControllerConnector<Whiskey2App, DataController, DataService>(this, this);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		notebookListAdapter = new NotebookListAdapter(this) {
			@Override
			public void onLoaded() {
				onNotepadsLoaded();
			}
		};
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setListNavigationCallbacks(notebookListAdapter, new OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				// notepadSelected(itemPosition,
				// notebookListAdapter.getItem(itemPosition));
				return true;
			}
		});
		getSupportActionBar().setHomeButtonEnabled(true);
		pager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int index) {
				if (index < sheetSelector.adapter.getCount()) {
					// Save sheetID
					sheetID.setWidgetValue(sheetSelector.adapter.getItemId(index));
				}
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	private void onNotepadsLoaded() {
		int index = 0;
		long id = -1;
		for (int i = 0; i < notebookListAdapter.getCount(); i++) {
			NotebookInfo info = notebookListAdapter.getItem(i);
			if (info.id == notepadID.getWidgetValue()) {
				index = i;
				id = info.id;
				break;
			}
		}
		notepadID.setWidgetValue(id);
		getSupportActionBar().setSelectedNavigationItem(index);
	}

	private void notepadSelected(int index, NotebookInfo info) {
		Log.i(TAG, "Notepad selected: " + info.id + ", " + notepadID.getWidgetValue());
		if (info.id != notepadID.getWidgetValue()) {
			startNewActivity(info);
			return;
		}
		// notepadID.setWidgetValue(info.id);
		sheetSelector.update(controller, info.id, sheetID.getWidgetValue());
	}

	private void startNewActivity(NotebookInfo info) {
		Intent intent = new Intent(this, NotepadActivity.class);
		intent.putExtra(KEY_NOTEPAD, info.id);
		startActivityForResult(intent, 0);
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
				reloadNotepad(notepadID.getWidgetValue());
			}

		};
		task.execute();
	}

	//
	// @Override
	// protected void onSaveInstanceState(Bundle outState) {
	// super.onSaveInstanceState(outState);
	// if (pager.getCurrentItem() != -1) { // Have item
	// outState.putLong("sheet",
	// sheetSelector.adapter.getItem(pager.getCurrentItem()).id);
	// }
	// }

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
		Log.i(TAG, "Collapsed: " + sheetSelector.collapsed);
		if (sheetSelector.collapsed) { // Expand
			if (sheetSelector.requestFocus()) { // Focused
			}
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		form.save(outState);
	}

}
