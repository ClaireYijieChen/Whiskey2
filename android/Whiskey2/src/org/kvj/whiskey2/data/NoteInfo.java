package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;

import android.widget.TextView;

public class NoteInfo {
	public long id = -1;
	public boolean collapsible = false;
	public String text;
	public int width = DataController.widths[1];
	public int x;
	public int y;
	public int color;
	public TextView widget = null;
	public boolean collapsed = true;
	public long sheetID = -1;
	private PJSONObject original = null;

	public static NoteInfo fromJSON(PJSONObject obj) throws JSONException {
		NoteInfo info = new NoteInfo();
		info.id = obj.getLong("id");
		info.color = obj.optInt("color");
		info.text = obj.optString("text", "");
		info.x = obj.optInt("x", 0);
		info.y = obj.optInt("y", 0);
		info.width = obj.optInt("width", info.width);
		info.collapsible = obj.optBoolean("collapsed", false);
		info.sheetID = obj.getLong("sheet_id");
		info.original = obj;
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
		return original;
	}
}
