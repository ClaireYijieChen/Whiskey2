package org.kvj.whiskey2.widgets;

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
import android.widget.ZoomButtonsController.OnZoomListener;
import android.widget.ZoomControls;

import com.actionbarsherlock.app.SherlockFragment;

public class PagerItemFragment extends SherlockFragment implements OnPageZoomListener {

	private static final String TAG = "PagerFragment";
	private int index = -1;
	MainSurface surface = null;
	private SheetsAdapter adapter = null;
	private long notepadID = -1;

	private OnZoomListener zoomListener = new OnZoomListener() {

		@Override
		public void onZoom(boolean zoomIn) {
			if (null == surface) {
				return;
			}
			Log.i(TAG, "Zoom controller: " + zoomIn + ", " + surface.zoom);
			if (zoomIn) { // Zoom in
			} else {
				surface.zoom += MainSurface.ZOOM_STEP;
				surface.createLayout();
			}
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			Log.i(TAG, "Zoom visibility: " + visible);
		}
	};
	private ZoomControls zoomButtons = null;

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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (null != adapter) { // Attach
			adapter.setFragmentActive(this, true);
		}
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
		surface = (MainSurface) v.findViewById(R.id.notepad_main_surface);
		surface.setPageZoomListener(this);
		surface.setController(index, adapter, getActivity());
		zoomButtons = (ZoomControls) v.findViewById(R.id.notepad_main_zoom);
		zoomButtons.setOnZoomInClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null != surface && surface.zoom > MainSurface.ZOOM_STEP) {
					// Zoom out
					surface.zoom -= MainSurface.ZOOM_STEP;
					surface.createLayout();
				}
			}
		});
		zoomButtons.setOnZoomOutClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (null != surface) {
					surface.zoom += MainSurface.ZOOM_STEP;
					surface.createLayout();
				}
			}
		});
		zoomButtons.hide();
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
		zoomButtons.show();
	}
}
