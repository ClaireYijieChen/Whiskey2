package org.kvj.whiskey2.widgets;

import org.kvj.bravo7.util.DelayedTask;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.widgets.MainSurface.OnPageZoomListener;
import org.kvj.whiskey2.widgets.adapters.SheetsAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.actionbarsherlock.app.SherlockFragment;

public class PagerItemFragment extends SherlockFragment implements OnPageZoomListener {

	private static final String TAG = "PagerFragment";
	private static final long ZOOM_HIDE_MSEC = 3000;
	private int index = -1;
	MainSurface surface = null;
	private SheetsAdapter adapter = null;
	private long notepadID = -1;
	LockableHorizontalScrollView horScroll = null;
	DelayedTask hideZoomTask = new DelayedTask(ZOOM_HIDE_MSEC, new Runnable() {

		@Override
		public void run() {
			if (null != getActivity()) { // Have Activity
				getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						zoomButtons.setVisibility(View.GONE);
					}
				});
			}
		}
	});

	private ViewGroup zoomButtons = null;

	public PagerItemFragment() {
		Log.i(TAG, "Fragment created from empty constructor");
	}

	public PagerItemFragment(int index, long notepadID, SheetsAdapter adapter) {
		super();
		this.notepadID = notepadID;
		this.index = index;
		this.adapter = adapter;
		if (null != adapter) { // Attach
			adapter.setFragmentActive(this, true);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (null != adapter) { // Attach
			adapter.setFragmentActive(this, false);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", index);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Log.i(TAG, "Creating page: " + container + ", " + adapter);
		if (null == adapter) {
			return null;
		}
		ViewGroup v = (ViewGroup) inflater.inflate(R.layout.notepad_pager_item, container, false);
		horScroll = (LockableHorizontalScrollView) v.findViewById(R.id.notepad_hor_scroll);
		surface = (MainSurface) v.findViewById(R.id.notepad_main_surface);
		surface.setPageZoomListener(this);
		surface.setController(index, adapter, getActivity());
		zoomButtons = (ViewGroup) v.findViewById(R.id.notepad_main_zoom);
		v.findViewById(R.id.notepad_main_zoom_in).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null != surface && surface.zoom > MainSurface.ZOOM_STEP) {
					// Zoom out
					surface.zoom -= MainSurface.ZOOM_STEP;
					surface.createLayout();
					hideZoomTask.schedule();
				}
			}
		});
		v.findViewById(R.id.notepad_main_zoom_out).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null != surface) {
					surface.zoom += MainSurface.ZOOM_STEP;
					surface.createLayout();
					hideZoomTask.schedule();
				}
			}
		});
		CompoundButton lockButton = (CompoundButton) v.findViewById(R.id.notepad_main_zoom_lock);
		lockButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (null != surface) {
					hideZoomTask.schedule();
				}
				horScroll.setLocked(!isChecked);
			}
		});
		zoomButtons.setVisibility(View.GONE);
		return v;
	}

	public long getNotepadID() {
		return notepadID;
	}

	public void update() {
		surface.createLayout();
	}

	public void refresh() {
		if (null != surface) { // Have surface
			surface.createLayout();
		}
	}

	@Override
	public void onShow() {
		zoomButtons.setVisibility(View.VISIBLE);
		hideZoomTask.schedule();
	}
}
