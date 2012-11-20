package org.kvj.whiskey2.widgets.adapters;

import java.util.List;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.widgets.adapters.NotebookListAdapter.NotebookInfo;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

public class NotebookListAdapter extends ArrayAdapter<NotebookInfo> {

	private static final String TAG = "Notebooks";

	public NotebookListAdapter(Context context) {
		super(context, android.R.layout.simple_spinner_item);
	}

	public void update(DataController controller) {
		clear();
		List<PJSONObject> notebooks = controller.getNotebooks();
		Log.i(TAG, "Notebooks: " + notebooks);
		for (PJSONObject obj : notebooks) { // Create and put notebooks
			NotebookInfo info = new NotebookInfo();
			try {
				info.id = obj.getLong("id");
				info.title = obj.getString("name");
				add(info);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static class NotebookInfo {
		public long id;
		public String title;

		@Override
		public String toString() {
			return title;
		}

	}

}
