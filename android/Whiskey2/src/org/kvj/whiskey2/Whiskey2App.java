package org.kvj.whiskey2;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.whiskey2.data.DataController;

public class Whiskey2App extends ApplicationContext {

	@Override
	protected void init() {
		publishBean(new DataController(this));
	}

}
