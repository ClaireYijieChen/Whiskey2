package org.kvj.whiskey2.widgets.adapters;

import org.kvj.bravo7.adapter.AnotherArrayAdapter;
import org.kvj.whiskey2.R;

import android.view.View;
import android.widget.TextView;

public class WidthsSpinnerAdapter extends AnotherArrayAdapter<Integer> {

	public WidthsSpinnerAdapter(Integer[] widths) {
		super(widths, R.layout.spinner_item);
	}

	@Override
	public void customize(View view, int position) {
		TextView textView = (TextView) view;
		textView.setText(String.format("%d%%", getItem(position)));
	}

}
