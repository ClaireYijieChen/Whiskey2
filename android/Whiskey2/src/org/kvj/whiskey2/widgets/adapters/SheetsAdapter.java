package org.kvj.whiskey2.widgets.adapters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kvj.whiskey2.R;
import org.kvj.whiskey2.data.BookmarkInfo;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.BookmarkSign;
import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.PagerItemFragment;
import org.kvj.whiskey2.widgets.v11.SheetListDecorator;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class SheetsAdapter implements ListAdapter {

	protected static final String TAG = "Sheets";
	private ListPageSelector selector = null;
	private float bookmarkWidth = 1;
	private int textPadding;
	private int bmarkGap;

	public SheetsAdapter(ListPageSelector selector) {
		this.selector = selector;
		selector.setAdapter(this);
		bookmarkWidth = selector.getResources().getDimensionPixelSize(R.dimen.list_item_height) / 2;
		textPadding = selector.getResources().getDimensionPixelSize(R.dimen.list_item_padding);
		bmarkGap = selector.getResources().getDimensionPixelSize(R.dimen.list_item_padding);
	}

	List<SheetInfo> data = new ArrayList<SheetInfo>();
	private DataSetObserver observer = null;
	private DataController controller = null;
	Set<PagerItemFragment> activeFragments = new HashSet<PagerItemFragment>();

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public SheetInfo getItem(int index) {
		if (index < getCount()) {
			return data.get(index);
		}
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return getItem(arg0).id;
	}

	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}

	@Override
	public View getView(int index, View view, ViewGroup group) {
		SheetInfo info = getItem(index);
		if (null == view) { // Inflate
			LayoutInflater inflater = (LayoutInflater) group.getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.sheet_item, group, false);
		}
		RelativeLayout layout = (RelativeLayout) view;
		decorate(this, view, index);
		TextView textView = (TextView) view.findViewById(R.id.sheet_item_title);
		textView.setText(info.title);
		RelativeLayout.LayoutParams textParams = (LayoutParams) textView.getLayoutParams();
		List<BookmarkInfo> bmarks = controller.getBookmarks(info.id);
		int rightMargin = textPadding;
		layout.removeViews(1, layout.getChildCount() - 1);
		if (null != bmarks) { // Add bmarks
			for (BookmarkInfo bmark : bmarks) { //
				RelativeLayout.LayoutParams params = new LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
						RelativeLayout.LayoutParams.WRAP_CONTENT);
				BookmarkSign sign = new BookmarkSign(group.getContext(), bookmarkWidth, bmark.color);
				params.rightMargin = rightMargin;
				params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				rightMargin += bookmarkWidth + bmarkGap;
				layout.addView(sign, params);
			}
		}
		textParams.rightMargin = rightMargin;
		return view;
	}

	private void decorate(SheetsAdapter sheetsAdapter, View view, int position) {
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			SheetListDecorator.decorateItem(sheetsAdapter, view, position);
		}
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		this.observer = null;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void update(final DataController controller, final long notepadID, final BookmarksAdapter bookmarkAdapter,
			final Runnable finishCallback) {
		this.controller = controller;
		final List<SheetInfo> newData = new ArrayList<SheetInfo>();
		final List<BookmarkInfo> bookmarks = new ArrayList<BookmarkInfo>();
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				try { //
					List<SheetInfo> sheets = controller.getSheets(notepadID);
					newData.addAll(sheets);
					for (SheetInfo sheet : newData) { // Create bookmarks
						List<BookmarkInfo> list = controller.getBookmarks(sheet.id);
						if (null != list) { // Have bookmarks
							bookmarks.addAll(list);
						}
					}
					Log.i(TAG, "After sheets load: " + newData.size() + ", " + bookmarks.size());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				data.clear();
				data.addAll(newData);
				if (null != observer) { // Have observer
					observer.onChanged();
				}
				bookmarkAdapter.setData(bookmarks);
				finishCallback.run();
			}
		};
		task.execute();
	}

	public DataController getController() {
		return controller;
	}

	public ListPageSelector getSelector() {
		return selector;
	}

	public void setFragmentActive(PagerItemFragment fragment, boolean active) {
		synchronized (activeFragments) { // Lock
			if (active) { // Add
				activeFragments.add(fragment);
			} else { // Remove
				activeFragments.remove(fragment);
			}
		}
	}

	public void refreshVisiblePages(boolean layoutChanged) {
		synchronized (activeFragments) { // Lock
			for (PagerItemFragment fragment : activeFragments) { // Refresh
				fragment.refresh(layoutChanged);
			}
		}
	}

}
