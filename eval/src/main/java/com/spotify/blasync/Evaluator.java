package com.spotify.blasync;

import java.util.HashMap;

public class Evaluator {

	public Evaluator() {
		super();
	}

	public <T> T evaluate(Wrapper<T> wrapper, HashMap<Wrapper<?>, ?> context) {
		
		if (context.containsKey(wrapper)) {
			return context.get(wrapper);
		}
	}
}
