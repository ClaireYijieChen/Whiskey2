package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;

public class BookmarkInfo {

	long id;
	public long sheetID;
	public String color;
	public String name;
	PJSONObject original = null;

	public static BookmarkInfo fromJSON(PJSONObject obj) throws JSONException {
		BookmarkInfo info = new BookmarkInfo();
		info.id = obj.getLong("id");
		info.sheetID = obj.getLong("sheet_id");
		info.color = obj.optString("color", "#ff0000");
		info.name = obj.optString("name", "");
		info.original = obj;
		return info;
	}

	public PJSONObject toPJSON() throws JSONException {
		if (null == original) { // Create
			original = new PJSONObject();
		}
		original.put("sheet_id", sheetID);
		original.put("color", color);
		original.put("name", name);
		return original;
	}
}
