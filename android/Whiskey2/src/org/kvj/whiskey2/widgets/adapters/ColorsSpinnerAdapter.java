package org.kvj.whiskey2.widgets.adapters;

import org.kvj.bravo7.adapter.AnotherArrayAdapter;
import org.kvj.whiskey2.R;

import android.view.View;

public class ColorsSpinnerAdapter extends AnotherArrayAdapter<Integer> {

	public ColorsSpinnerAdapter(Integer[] colors) {
		super(colors, R.layout.spinner_item);
	}

	@Override
	public void customize(View view, int position) {
		view.setBackgroundResource(getItem(position));
	}

}
