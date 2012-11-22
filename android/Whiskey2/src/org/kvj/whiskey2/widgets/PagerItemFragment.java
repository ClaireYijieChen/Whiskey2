package org.kvj.whiskey2.widgets;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.widgets.adapters.PagesPagerAdapter;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class PagerItemFragment extends SherlockFragment {

	private static final String TAG = "PagerFragment";
	private int index;
	MainSurface surface = null;
	private ListPageSelector selector = null;

	public PagerItemFragment() {
		Log.i(TAG, "Fragment created from empty constructor");
	}

	public PagerItemFragment(int index) {
		super();
		this.index = index;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (null != savedInstanceState && savedInstanceState.containsKey("index")) {
			// Restore index
			index = savedInstanceState.getInt("index");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", index);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		selector = ((PagesPagerAdapter) ((ViewPager) container).getAdapter()).getSelector();
		Log.i(TAG, "Creating page: " + container + ", " + selector);
		if (null == selector || index >= selector.getCount()) { // Out of bounds
			TextView view = new TextView(getActivity());
			view.setText("Loading...");
			return view;
		}
		View v = inflater.inflate(R.layout.notepad_pager_item, container, false);
		surface = (MainSurface) v.findViewById(R.id.notepad_main_surface);
		surface.setController(index, selector);
		return v;
	}
}
