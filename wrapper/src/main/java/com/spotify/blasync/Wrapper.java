package com.spotify.blasync;

import java.util.HashMap;

public interface Wrapper<T> {

	Class<T> getBaseClass();

	T evaluate(HashMap<Wrapper<?>, Object> context);

	@Override
	int hashCode();

	@Override
	boolean equals(Object obj);
}
