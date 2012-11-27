package org.kvj.whiskey2.widgets.adapters;

import org.kvj.whiskey2.widgets.ListPageSelector;
import org.kvj.whiskey2.widgets.PagerItemFragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

public class PagesPagerAdapter extends FragmentPagerAdapter {

	private static final String TAG = "PagesAdapter";
	private ListPageSelector selector = null;
	private long notepadID = -1;

	public PagesPagerAdapter(FragmentManager fm, ViewPager pager) {
		super(fm);
		pager.setAdapter(this);
	}

	@Override
	public Fragment getItem(int index) {
		Log.i(TAG, "Creating page: " + index);
		return new PagerItemFragment(index, notepadID, selector.adapter);
	}

	@Override
	public int getCount() {
		int count = null == selector ? 0 : selector.adapter.getCount();
		return count;
	}

	public ListPageSelector getSelector() {
		return selector;
	}

	public void setSelector(ListPageSelector selector) {
		this.selector = selector;
	}

}
