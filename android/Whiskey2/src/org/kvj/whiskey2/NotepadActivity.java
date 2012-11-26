package org.kvj.whiskey2;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.impl.bundle.LongBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.DataController.DataControllerListener;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.data.SheetInfo;
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

public class NotepadActivity extends SherlockFragmentActivity implements ControllerReceiver<DataController>,
		DataControllerListener {

	public static final String KEY_NOTEPAD = "notepad_id";
	public static final String KEY_SHEET = "sheet_id";

	protected static final String TAG = "NotepadActivity";
	ControllerConnector<Whiskey2App, DataController, DataService> conn = null;
	private DataController controller = null;
	NotebookListAdapter notebookListAdapter = null;
	ListPageSelector sheetSelector = null;
	ViewPager pager = null;
	FormController form = null;
	TransientAdapter<Long> notepadID = null;
	TransientAdapter<Long> sheetID = null;
	protected int notepadPosition = -1;
	protected PagesPagerAdapter pagerAdapter = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_notepad, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home: // Run sync
			sheetSelector.collapseExpand(false);
			break;
		case R.id.menu_sync: // Run sync
			sync();
			break;
		case R.id.menu_reload: // Reload selected notepad
			refresh();
			break;
		}

		return true;
	}

	private void selectPage(int position, boolean animate) {
		pager.setCurrentItem(position, animate);
		pager.requestFocus();
		sheetSelector.collapseExpand(true);
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

		sheetSelector.addListener(new PagesSelectorListener() {

			@Override
			public void onPageSelected(int position, long id) {
				selectPage(position, true);
			}
		});
		conn = new ControllerConnector<Whiskey2App, DataController, DataService>(this, this);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		notebookListAdapter = new NotebookListAdapter(this);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setListNavigationCallbacks(notebookListAdapter, new OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				notepadSelected(itemPosition);
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

	protected void notepadSelected(int position) {
		NotebookInfo info = notebookListAdapter.getItem(position);
		if (notepadID.getWidgetValue() != info.id) { // Another item
			startNewActivity(info);
			// Restore selection
			getSupportActionBar().setSelectedNavigationItem(notepadPosition);
		}
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
				refresh();
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
		controller.removeControllerListener(this);
		conn.disconnectController();
	}

	@Override
	public void onController(DataController controller) {
		controller.addControllerListener(this);
		if (null == this.controller) { // Just connected
			this.controller = controller;
			refresh();
		}
	}

	private void refresh() {
		notebookListAdapter.update(controller, new Runnable() {

			@Override
			public void run() {
				int position = -1;
				for (int i = 0; i < notebookListAdapter.getCount(); i++) {
					// Search selected notepad
					NotebookInfo info = notebookListAdapter.getItem(i);
					if (info.id == notepadID.getWidgetValue()) { // Found
						position = i;
						break;
					}
				}
				if (-1 == position && notebookListAdapter.getCount() > 0) {
					// Select first item
					position = 0;
					notepadID.setWidgetValue(notebookListAdapter.getItem(position).id);
				}
				if (-1 != position) { // Select notepad
					notepadPosition = position;
					getSupportActionBar().setSelectedNavigationItem(position);
					refreshNotepad();
				}
			}

		});
	}

	private void refreshSheets() {
		pagerAdapter = new PagesPagerAdapter(getSupportFragmentManager(), pager);
		pagerAdapter.setSelector(sheetSelector);
		// Search selected sheetID
		int position = -1;
		for (int i = 0; i < sheetSelector.adapter.getCount(); i++) { //
			SheetInfo info = sheetSelector.adapter.getItem(i);
			if (sheetID.getWidgetValue() == info.id) { // Found
				position = i;
				break;
			}
		}
		if (-1 == position && sheetSelector.adapter.getCount() > 0) {
			// Select first item
			position = 0;
		}
		if (-1 != position) { // Select page
			selectPage(position, false);
		}
	}

	private void refreshNotepad() {
		sheetSelector.adapter.update(controller, notepadID.getWidgetValue(), new Runnable() {

			@Override
			public void run() {
				// New pagerAdapter
				refreshSheets();
			}
		});
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "Collapsed: " + sheetSelector.collapsed);
		if (sheetSelector.collapsed) { // Expand
			sheetSelector.collapseExpand(false);
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		form.save(outState);
	}

	@Override
	public void dataChanged() {
		refresh();
	}

	@Override
	public void noteChanged(NoteInfo info) {
		refreshSheets();
	}

}
