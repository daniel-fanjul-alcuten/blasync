package com.spotify.blasync;

import java.util.HashMap;

public abstract class DefaultWrapper<T> implements Wrapper<T> {

	@Override
	public T evaluate(HashMap<Wrapper<?>, Object> context) {

		if (context.containsKey(this)) {
			@SuppressWarnings("unchecked")
			T value = (T) context.get(this);
			return value;
		}

		T value = evaluate2(context);
		context.put(this, value);
		return value;
	}

	protected T evaluate2(HashMap<Wrapper<?>, Object> context) {
		throw new IllegalStateException("No value found for the Wrapper");
	}
}
