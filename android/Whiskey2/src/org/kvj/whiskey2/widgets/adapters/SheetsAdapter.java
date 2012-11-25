package org.kvj.whiskey2.widgets.adapters;

import java.util.ArrayList;
import java.util.List;

import org.kvj.lima1.sync.PJSONObject;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.widgets.ListPageSelector;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class SheetsAdapter implements ListAdapter {

	private ListPageSelector selector = null;

	public SheetsAdapter(ListPageSelector selector) {
		this.selector = selector;
		selector.setAdapter(this);
	}

	public static class SheetInfo {
		public long id;
		public String title;
		public Long templateID;
	}

	List<SheetInfo> data = new ArrayList<SheetsAdapter.SheetInfo>();
	private DataSetObserver observer = null;

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public SheetInfo getItem(int index) {
		if (index < getCount()) {
			return data.get(index);
		}
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return getItem(arg0).id;
	}

	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}

	@Override
	public View getView(int index, View view, ViewGroup group) {
		SheetInfo info = getItem(index);
		if (null == view) { // Inflate
			LayoutInflater inflater = (LayoutInflater) group.getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.sheet_item, group, false);
		}
		TextView textView = (TextView) view.findViewById(R.id.sheet_item_title);
		textView.setText(info.title);
		return view;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		this.observer = null;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void update(final DataController controller, final long notepadID, final Long sheetID) {
		final List<SheetInfo> newData = new ArrayList<SheetsAdapter.SheetInfo>();
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try { //
					List<PJSONObject> sheets = controller.getSheets(notepadID);
					for (PJSONObject obj : sheets) { // Create info
						SheetInfo info = new SheetInfo();
						info.id = obj.getLong("id");
						info.title = obj.getString("title");
						newData.add(info);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				data.clear();
				data.addAll(newData);
				if (null != observer) { // Have observer
					observer.onChanged();
					selector.notifyPagesChanged(notepadID);
				}
				for (int i = 0; i < data.size(); i++) {
					SheetInfo info = data.get(i);
					if (info.id == sheetID) {
						selector.notifyPageSelected(i, info.id);
					}
				}
			}
		};
		task.execute();
	}

}
