package org.kvj.whiskey2.widgets;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

public class PagerItemFragment extends SherlockFragment {

	private static final String TAG = "PagerFragment";
	private int index = -1;
	MainSurface surface = null;
	private SheetsAdapter adapter = null;
	private long notepadID = -1;

	public PagerItemFragment() {
		Log.i(TAG, "Fragment created from empty constructor");
	}

	public PagerItemFragment(int index, long notepadID, SheetsAdapter adapter) {
		super();
		this.notepadID = notepadID;
		this.index = index;
		this.adapter = adapter;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", index);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// selector = ((PagesPagerAdapter) ((ViewPager)
		// container).getAdapter()).getSelector();
		Log.i(TAG, "Creating page: " + container + ", " + adapter);
		if (null == adapter) {
			return null;
		}
		// if (null == selector || index >= selector.getCount()) { // Out of
		// bounds
		// TextView view = new TextView(getActivity());
		// view.setText("Loading...");
		// return view;
		// }
		View v = inflater.inflate(R.layout.notepad_pager_item, container, false);
		surface = (MainSurface) v.findViewById(R.id.notepad_main_surface);
		surface.setController(index, adapter, getActivity());
		return v;
	}

	public long getNotepadID() {
		return notepadID;
	}

	public void update() {
		surface.createLayout();
	}
}
