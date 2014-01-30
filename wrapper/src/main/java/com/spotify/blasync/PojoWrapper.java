package com.spotify.blasync;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map.Entry;

public abstract class PojoWrapper<T> extends DefaultWrapper<T> implements
		Wrapper<T> {

	private final Constructor<T> constructor;
	private final Wrapper<?>[] args;
	private final HashMap<Method, Wrapper<?>> setters;

	public PojoWrapper(Constructor<T> constructor, Wrapper<?>... args) {
		super();
		this.setters = new HashMap<Method, Wrapper<?>>();
		this.constructor = constructor;
		this.args = args;
	}

	public PojoWrapper(HashMap<Method, Wrapper<?>> setters,
			Constructor<T> constructor, Wrapper<?>... args) {
		super();
		this.setters = setters;
		this.constructor = constructor;
		this.args = args;
	}

	public Constructor<T> getConstructor() {
		return constructor;
	}

	public Wrapper<?>[] getArgs() {
		return args;
	}

	public HashMap<Method, Wrapper<?>> getSetters() {
		return setters;
	}

	@Override
	protected T evaluate2(HashMap<Wrapper<?>, Object> context) {

		Object[] arguments = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			Wrapper<?> wrapper = args[i];
			arguments[i] = wrapper.evaluate(context);
		}

		try {

			T object = constructor.newInstance(arguments);
			for (Entry<Method, Wrapper<?>> entry : setters.entrySet()) {
				Method method = entry.getKey();
				Wrapper<?> wrapper = entry.getValue();
				Object argument = wrapper.evaluate(context);
				method.invoke(object, argument);
			}
			return object;

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	protected static <T> Constructor<T> getDefaultConstructor(Class<T> clazz) {
		try {
			return clazz.getConstructor();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
