package org.kvj.whiskey2.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.kvj.lima1.sync.PJSONObject;

public class TemplateInfo {

	public long id = -1;
	public String tag;
	public String type;
	public String name = "No template";
	public JSONObject config;
	public int width = 102;
	public int height = 144;
	PJSONObject original = null;

	public static TemplateInfo fromJSON(PJSONObject obj) throws JSONException {
		TemplateInfo info = new TemplateInfo();
		info.id = obj.getLong("id");
		info.width = obj.optInt("width", info.width);
		info.height = obj.optInt("height", info.height);
		info.name = obj.optString("name", "");
		info.tag = obj.optString("tag", "");
		info.type = obj.optString("type", "");
		info.config = obj.optJSONObject("config");
		info.original = obj;
		return info;
	}

}
