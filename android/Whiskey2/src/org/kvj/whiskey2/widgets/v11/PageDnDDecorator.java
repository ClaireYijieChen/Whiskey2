package org.kvj.whiskey2.widgets.v11;

import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.widgets.PageSurface;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PageDnDDecorator {

	public PageDnDDecorator(PageSurface surface, SheetInfo info) {
		surface.setOnDragListener(new OnDragListener() {

			@Override
			public boolean onDrag(View v, DragEvent event) {
				// TODO Auto-generated method stub
				return false;
			}
		});
	}
}
