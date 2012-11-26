package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;

public class SheetInfo {
	public long id;
	public String title;
	public Long templateID;

	public static SheetInfo fromPJSON(PJSONObject obj) throws JSONException {
		SheetInfo info = new SheetInfo();
		info.id = obj.getLong("id");
		info.title = obj.getString("title");
		return info;
	}
}
