package org.kvj.whiskey2.widgets.adapters;

import java.util.ArrayList;

import org.kvj.bravo7.adapter.AnotherListAdapter;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.BookmarkInfo;

import android.graphics.Color;
import android.view.View;

public class BookmarksAdapter extends AnotherListAdapter<BookmarkInfo> {

	public BookmarksAdapter() {
		super(new ArrayList<BookmarkInfo>(), R.layout.bookmark_list_item);
	}

	@Override
	public void customize(View view, int position) {
		BookmarkInfo info = getItem(position);
		view.setBackgroundColor(Color.parseColor(info.color));
	}

}
