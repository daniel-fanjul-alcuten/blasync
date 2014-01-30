package com.spotify.blasync;

import java.lang.reflect.Method;
import java.util.HashMap;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public abstract class DelegatedWrapper<T> extends DefaultWrapper<T> implements
		Wrapper<T> {

	private final Wrapper<?> wrapper;
	private final Method method;
	private final Wrapper<?>[] args;

	public DelegatedWrapper(Wrapper<?> wrapper, Method method,
			Wrapper<?>... args) {
		super();
		this.wrapper = wrapper;
		this.method = method;
		this.args = args;
	}

	public Wrapper<?> getWrapper() {
		return wrapper;
	}

	public Method getMethod() {
		return method;
	}

	public Wrapper<?>[] getArgs() {
		return args;
	}

	@Override
	public T evaluate2(HashMap<Wrapper<?>, Object> context) {

		Object object = wrapper.evaluate(context);
		if (object != null) {
			if (ListenableFuture.class.isAssignableFrom(object.getClass())) {
				ListenableFuture<?> future = (ListenableFuture<?>) object;
				object = Futures.getUnchecked(future);
			}
		}

		Object[] arguments = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			Wrapper<?> wrapper = args[i];
			arguments[i] = wrapper.evaluate(context);
		}

		try {
			@SuppressWarnings("unchecked")
			T value = (T) method.invoke(object, arguments);
			return value;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
