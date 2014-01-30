package com.spotify.blasync;

import java.util.HashMap;

public abstract class IfWrapper<T> extends DefaultWrapper<T> implements
		Wrapper<T> {

	private final Wrapper<Boolean> condWrapper;
	private final Wrapper<T> thenWrapper;
	private final Wrapper<T> elseWrapper;

	public IfWrapper(Wrapper<Boolean> condWrapper, Wrapper<T> thenWrapper,
			Wrapper<T> elseWrapper) {
		this.condWrapper = condWrapper;
		this.thenWrapper = thenWrapper;
		this.elseWrapper = elseWrapper;
	}

	public Wrapper<Boolean> getCondWrapper() {
		return condWrapper;
	}

	public Wrapper<T> getThenWrapper() {
		return thenWrapper;
	}

	public Wrapper<T> getElseWrapper() {
		return elseWrapper;
	}

	@Override
	protected T evaluate2(HashMap<Wrapper<?>, Object> context) {
		if (condWrapper.evaluate(context)) {
			return thenWrapper.evaluate(context);
		} else {
			return elseWrapper.evaluate(context);
		}
	}
}
