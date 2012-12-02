package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.kvj.lima1.sync.PJSONObject;

public class SheetInfo {
	public long id;
	public String title;
	public long templateID = -1;

	public static SheetInfo fromPJSON(PJSONObject obj) throws JSONException {
		SheetInfo info = new SheetInfo();
		info.id = obj.getLong("id");
		info.title = obj.getString("title");
		info.templateID = obj.optLong("template_id", -1);
		return info;
	}
}
