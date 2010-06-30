package com.foo.client.dispatch.base;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public abstract class BaseAction<R extends Result> implements Action<R> {

	public static final String NAME = "com.foo.client.dispatch.base.BaseAction";
	private static final long serialVersionUID = 1L;

	public void inBaseAction() {
	}

}
