package com.spotify.blasync;

import java.util.HashMap;

public abstract class CastWrapper<T> extends DefaultWrapper<T> implements
		Wrapper<T> {

	private final Wrapper<?> wrapper;

	public CastWrapper(Wrapper<?> wrapper) {
		this.wrapper = wrapper;
	}

	public Wrapper<?> getWrapper() {
		return wrapper;
	}

	@Override
	protected T evaluate2(HashMap<Wrapper<?>, Object> context) {

		Object object = wrapper.evaluate(context);

		@SuppressWarnings("unchecked")
		T value = (T) object;
		return value;
	}
}
