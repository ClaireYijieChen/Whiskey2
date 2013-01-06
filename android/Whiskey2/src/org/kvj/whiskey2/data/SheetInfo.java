package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.lima1.sync.PJSONObject;

public class SheetInfo {
	public long id = -1;
	public String title;
	public long templateID = -1;
	public JSONObject config = null;
	public PJSONObject origin = null;

	public static SheetInfo fromPJSON(PJSONObject obj) throws JSONException {
		SheetInfo info = new SheetInfo();
		info.id = obj.getLong("id");
		info.title = obj.getString("title");
		info.templateID = obj.optLong("template_id", -1);
		info.config = obj.optJSONObject("config");
		info.origin = obj;
		return info;
	}
}
