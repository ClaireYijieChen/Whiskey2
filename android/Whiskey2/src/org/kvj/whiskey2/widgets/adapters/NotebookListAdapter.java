package org.kvj.whiskey2.widgets.adapters;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.widgets.adapters.NotebookListAdapter.NotebookInfo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NotebookListAdapter extends ArrayAdapter<NotebookInfo> {

	private static final String TAG = "Notebooks";

	public NotebookListAdapter(Context context) {
		super(context, android.R.layout.simple_spinner_item);
	}

	public void onLoaded() {

	}

	public void update(final DataController controller) {
		clear();
		new AsyncTask<Void, Void, List<NotebookInfo>>() {

			@Override
			protected List<NotebookInfo> doInBackground(Void... params) {
				List<NotebookInfo> result = new ArrayList<NotebookListAdapter.NotebookInfo>();
				List<PJSONObject> notebooks = controller.getNotebooks();
				Log.i(TAG, "Notebooks: " + notebooks);
				for (PJSONObject obj : notebooks) { // Create and put notebooks
					NotebookInfo info = new NotebookInfo();
					try {
						info.id = obj.getLong("id");
						info.title = obj.getString("name");
						result.add(info);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return result;
			}

			@Override
			protected void onPostExecute(java.util.List<NotebookInfo> result) {
				addAll(result);
				onLoaded();
			};
		}.execute();
	}

	public static class NotebookInfo {
		public long id;
		public String title;

		@Override
		public String toString() {
			return title;
		}

	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.spinner_item, parent, false);
		TextView textView = (TextView) convertView;
		textView.setText(getItem(position).title);
		return convertView;
	}

}
