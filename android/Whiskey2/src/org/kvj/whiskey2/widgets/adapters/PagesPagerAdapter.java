package org.kvj.whiskey2.widgets.adapters;

import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.ListPageSelector.PagesSelectorListener;
import org.kvj.whiskey2.widgets.PagerItemFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class PagesPagerAdapter extends FragmentStatePagerAdapter implements PagesSelectorListener {

	private static final String TAG = "PagesAdapter";
	private ListPageSelector selector = null;
	private ViewPager pager = null;
	private long notepadID = -1;

	public PagesPagerAdapter(FragmentManager fm, ViewPager pager) {
		super(fm);
		this.pager = pager;
		pager.setAdapter(this);
	}

	@Override
	public Fragment getItem(int index) {
		Log.i(TAG, "Creating page: " + index);
		return new PagerItemFragment(index, notepadID, selector);
	}

	@Override
	public int getCount() {
		int count = null == selector ? 0 : selector.adapter.getCount();
		return count;
	}

	@Override
	public void onPagesChanged(long notepadID) {
		Log.i(TAG, "Pages changed");
		this.notepadID = notepadID;
		notifyDataSetChanged();
	}

	@Override
	public void onPageSelected(int position, long id) {
		pager.setCurrentItem(position, false);
	}

	public ListPageSelector getSelector() {
		return selector;
	}

	public void setSelector(ListPageSelector selector) {
		this.selector = selector;
		selector.addListener(this);
	}

}
