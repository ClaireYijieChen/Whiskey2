package org.kvj.whiskey2.data;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;

import android.view.View;
import android.widget.LinearLayout;

public class NoteInfo {
	public long id = -1;
	public boolean collapsible = false;
	public String text;
	public int width = 0;
	public int x;
	public int y;
	public int color;
	public JSONArray links = null;
	public JSONArray files = null;
	public View widget = null;
	public boolean collapsed = true;
	public long sheetID = -1;
	private PJSONObject original = null;
	public LinearLayout linksToolbar = null;
	public Map<String, String> fileCache = new HashMap<String, String>();
	public int touchedPoints = 1;

	public NoteInfo(int width) {
		this.width = width;
	}

	public static NoteInfo fromJSON(PJSONObject obj) throws JSONException {
		NoteInfo info = new NoteInfo(0);
		info.id = obj.getLong("id");
		info.color = obj.optInt("color");
		info.text = obj.optString("text", "");
		info.x = obj.optInt("x", 0);
		info.y = obj.optInt("y", 0);
		info.width = obj.optInt("width", info.width);
		info.collapsible = obj.optBoolean("collapsed", false);
		info.sheetID = obj.getLong("sheet_id");
		info.original = obj;
		info.links = obj.optJSONArray("links");
		info.files = obj.optJSONArray("files");
		return info;
	}

	public PJSONObject toPJSON() throws JSONException {
		if (null == original) { // Create
			original = new PJSONObject();
		}
		original.put("collapsed", collapsible);
		original.put("color", color);
		original.put("text", text);
		original.put("x", x);
		original.put("y", y);
		original.put("width", width);
		original.put("sheet_id", sheetID);
		if (null != links) { // Add links
			original.put("links", links);
		}
		if (null != files) { // Add files
			original.put("files", files);
		}
		return original;
	}
}
