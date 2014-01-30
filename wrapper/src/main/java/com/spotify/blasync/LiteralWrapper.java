package com.spotify.blasync;

import java.util.HashMap;

public abstract class LiteralWrapper<T> extends DefaultWrapper<T> implements
		Wrapper<T> {

	private final T value;

	public LiteralWrapper(T value) {
		super();
		this.value = value;
	}

	public T getValue() {
		return value;
	}

	@Override
	public T evaluate2(HashMap<Wrapper<?>, Object> context) {
		return value;
	}
}
