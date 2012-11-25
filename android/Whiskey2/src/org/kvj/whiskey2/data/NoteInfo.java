package org.kvj.whiskey2.data;

import android.widget.TextView;

public class NoteInfo {
	public long id = -1;
	public boolean collapsible = false;
	public String text;
	public int width;
	public int x;
	public int y;
	public int color;
	public TextView widget = null;
	public boolean collapsed = true;
	public long sheetID = -1;
}
