package com.spotify.blasync;

import java.util.HashMap;

public class EqualsWrapper extends BooleanDefaultWrapper {

	private final Wrapper<?> wrapper1;
	private final Wrapper<?> wrapper2;

	public <T> EqualsWrapper(Wrapper<T> wrapper1, Wrapper<T> wrapper2) {
		super();
		this.wrapper1 = wrapper1;
		this.wrapper2 = wrapper2;
	}

	public Wrapper<?> getWrapper1() {
		return wrapper1;
	}

	public Wrapper<?> getWrapper2() {
		return wrapper2;
	}

	@Override
	protected Boolean evaluate2(HashMap<Wrapper<?>, Object> context) {

		Object evaluate1 = wrapper1.evaluate(context);
		Object evaluate2 = wrapper2.evaluate(context);

		if (evaluate1 == null) {
			return evaluate2 == null;
		}
		return evaluate1.equals(evaluate2);
	}
}
