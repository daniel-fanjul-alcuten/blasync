package com.spotify.blasync;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.spotify.blasync.Graph.GroupInfo;
import com.spotify.blasync.Graph.WrapperInfo;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;

public class AsyncGenerator {

	public void generate(JCodeModel model, Map<Class<?>, String> classes)
			throws JClassAlreadyExistsException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			InstantiationException, ClassNotFoundException {

		for (Entry<Class<?>, String> entry : classes.entrySet()) {
			Class<?> clazz = entry.getKey();

			LinkedList<Method> methods = getAsyncMethods(clazz);
			if (methods.isEmpty()) {
				continue;
			}

			// class
			JDefinedClass jclass = model._class(entry.getValue());

			for (Method method : methods) {

				// root
				Class<?>[] parameterTypes = method.getParameterTypes();
				Object[] args = new Wrapper<?>[parameterTypes.length];
				for (int i = 0; i < parameterTypes.length; i++) {
					Class<?> parameterType = parameterTypes[i];
					args[i] = Class
							.forName(
									defaultWrapperClassName(getWrapperActualClass(parameterType)))
							.newInstance();
				}
				Wrapper<?> root = (Wrapper<?>) method.invoke(
						clazz.newInstance(), args);

				// graph
				Graph graph = new Graph();
				graph.add(root);
				graph.wrappers.get(root).root = true;
				for (WrapperInfo info : graph.wrappers.values()) {
					if (isFutureWrapper(info.wrapper)) {
						info.future = true;
					}
				}
				HashMap<Wrapper<?>, GroupInfo> groups = new HashMap<Wrapper<?>, GroupInfo>();
				groups.putAll(graph.calculateGroups());

				// method
				int modifiers = method.getModifiers();
				int mod = 0;
				if (Modifier.isPublic(modifiers)) {
					mod = JMod.PUBLIC;
				} else if (Modifier.isProtected(modifiers)) {
					mod = JMod.PROTECTED;
				} else {
					mod = JMod.PRIVATE;
				}
				Class<?> returnType = method.getReturnType();
				JMethod jmethod = jclass.method(
						mod,
						model.ref(ListenableFuture.class).narrow(
								getWrapperActualClass(returnType)),
						method.getName());

				// params
				Context context = new Context();
				for (int i = 0; i < parameterTypes.length; i++) {
					Class<?> parameterType = parameterTypes[i];
					context.exprs.put((Wrapper<?>) args[i], jmethod.param(
							JMod.FINAL, getWrapperActualClass(parameterType),
							"p" + i));
				}

				// body
				while (!groups.isEmpty()) {
					Iterator<GroupInfo> iterator = groups.values().iterator();
					while (iterator.hasNext()) {
						GroupInfo group = iterator.next();
						if (group.deps.isEmpty()) {
							context.exprs.put(
									group.root.wrapper,
									generateFuture(model, jmethod.body(),
											group, context));
							iterator.remove();
						} else {
							boolean generate = true;
							for (Wrapper<?> wrapper : group.deps) {
								if (!context.exprs.containsKey(wrapper)) {
									generate = false;
								}
							}
							if (generate) {
								context.exprs
										.put(group.root.wrapper,
												generateTransformFuture(model,
														jmethod.body(), group,
														context));
								iterator.remove();
							}
						}
					}
				}

				jmethod.body()._return(context.exprs.get(root));
			}
		}
	}

	private boolean isFutureWrapper(Wrapper<?> wrapper) {

		if (wrapper instanceof DelegatedWrapper) {
			DelegatedWrapper<?> dwrapper = (DelegatedWrapper<?>) wrapper;

			Method method = dwrapper.getMethod();
			return getFuturedType(method.getGenericReturnType()) != null;
		}
		return false;
	}

	private class Context {

		Context root;
		HashMap<Wrapper<?>, JExpression> exprs;

		Context() {
			super();
			this.root = this;
			this.exprs = new HashMap<Wrapper<?>, JExpression>();
		}

		Context(Context context) {
			this();
			this.root = context.root;
			this.exprs.putAll(context.exprs);
		}

		private int id;

		int id() {
			return this.root.id++;
		}
	}

	private JExpression generateFuture(JCodeModel model, JBlock body,
			GroupInfo group, Context context) {

		if (group.root.future) {
			JExpression jexpr = generateExpressionForWrapper1(model, body,
					group.root.wrapper, context);
			return body.decl(JMod.FINAL, model.ref(ListenableFuture.class)
					.narrow(group.root.wrapper.getBaseClass()),
					"v" + context.id(), jexpr);
		}

		JDefinedClass jcallable = model.anonymousClass(model
				.ref(Callable.class).narrow(group.root.wrapper.getBaseClass()));
		JMethod jcall = jcallable.method(JMod.PUBLIC,
				group.root.wrapper.getBaseClass(), "call");
		jcall.body()._return(
				generateExpressionForWrapper1(model, jcall.body(),
						group.root.wrapper, new Context(context)));
		JInvocation jfuture = model.ref(ListenableFutureTask.class)
				.staticInvoke("create").arg(JExpr._new(jcallable));
		return body.decl(
				JMod.FINAL,
				model.ref(ListenableFuture.class).narrow(
						group.root.wrapper.getBaseClass()), "v" + context.id(),
				jfuture);

	}

	private JExpression generateTransformFuture(JCodeModel model, JBlock body,
			GroupInfo group, Context context) {

		JInvocation jAllAsListInvocation = model.ref(Futures.class)
				.staticInvoke("<Object>allAsList");
		for (Wrapper<?> dep : group.deps) {
			jAllAsListInvocation.arg(context.exprs.get(dep));
		}
		JVar jAllAsList = body.decl(
				model.ref(ListenableFuture.class).narrow(
						model.ref(List.class).narrow(Object.class)), "v"
						+ context.id(), jAllAsListInvocation);
		jAllAsList.annotate(SuppressWarnings.class).param("value", "unchecked");
		JDefinedClass asyncFunction = model.anonymousClass(model.ref(
				AsyncFunction.class).narrow(Object.class,
				group.root.wrapper.getBaseClass()));
		JMethod japply = asyncFunction.method(
				JMod.PUBLIC,
				model.ref(ListenableFuture.class).narrow(
						group.root.wrapper.getBaseClass()), "apply")._throws(
				Exception.class);
		japply.param(Object.class, "_");
		Context context2 = new Context(context);
		for (Wrapper<?> dep : group.deps) {
			context2.exprs.put(
					dep,
					japply.body().decl(
							model.ref(dep.getBaseClass()),
							"v" + context.id(),
							model.ref(Futures.class)
									.staticInvoke("getUnchecked")
									.arg(context.exprs.get(dep))));
		}
		JExpression jexpr = generateExpressionForWrapper1(model, japply.body(),
				group.root.wrapper, context2);
		if (group.root.future) {
			japply.body()._return(jexpr);
		} else {
			japply.body()._return(
					model.ref(Futures.class).staticInvoke("immediateFuture")
							.arg(jexpr));
		}

		JInvocation jtransform = model.ref(Futures.class)
				.staticInvoke("transform").arg(jAllAsList)
				.arg(JExpr._new(asyncFunction));
		return body.decl(
				model.ref(ListenableFuture.class).narrow(
						group.root.wrapper.getBaseClass()), "v" + context.id(),
				jtransform);

	}

	private JExpression generateExpressionForWrapper1(JCodeModel model,
			JBlock body, Wrapper<?> wrapper, Context context) {

		if (context.exprs.containsKey(wrapper)) {
			return context.exprs.get(wrapper);
		}

		JExpression jexpr = generateExpressionForWrapper2(model, body, wrapper,
				context);
		context.exprs.put(wrapper, jexpr);
		return jexpr;
	}

	private JExpression generateExpressionForWrapper2(JCodeModel model,
			JBlock body, Wrapper<?> wrapper, Context context) {

		if (wrapper instanceof IfWrapper) {
			IfWrapper<?> iwrapper = (IfWrapper<?>) wrapper;

			JExpression jcond = generateExpressionForWrapper1(model, body,
					iwrapper.getCondWrapper(), context);

			JVar jvar = body.decl(model.ref(wrapper.getBaseClass()), "v"
					+ context.id());
			JConditional jif = body._if(jcond);

			JExpression jthen = generateExpressionForWrapper1(model,
					jif._then(), iwrapper.getThenWrapper(), context);
			jif._then().assign(jvar, jthen);

			JExpression jelse = generateExpressionForWrapper1(model,
					jif._else(), iwrapper.getElseWrapper(), context);
			jif._else().assign(jvar, jelse);
			return jvar;

		} else if (wrapper instanceof DelegatedWrapper) {
			DelegatedWrapper<?> dwrapper = (DelegatedWrapper<?>) wrapper;

			JInvocation jcall = JExpr.invoke(
					generateExpressionForWrapper1(model, body,
							dwrapper.getWrapper(), context), dwrapper
							.getMethod().getName());
			for (Wrapper<?> arg : dwrapper.getArgs()) {
				JExpression jarg = generateExpressionForWrapper1(model, body,
						arg, context);
				jcall.arg(jarg);
			}
			JClass jtype;
			if (getFuturedType(dwrapper.getMethod().getGenericReturnType()) == null) {
				jtype = model.ref(wrapper.getBaseClass());
			} else {
				jtype = model.ref(ListenableFuture.class).narrow(
						wrapper.getBaseClass());
			}
			return body.decl(jtype, "v" + context.id(), jcall);

		} else if (wrapper instanceof LiteralWrapper) {
			LiteralWrapper<?> lwrapper = (LiteralWrapper<?>) wrapper;

			Object value = lwrapper.getValue();
			if (value == null) {
				return JExpr._null();
			} else if (value instanceof String) {
				return JExpr.lit((String) value);
			} else if (value instanceof Integer) {
				return JExpr.lit((Integer) value);
			} else {
				throw new UnsupportedOperationException("Unknown literal "
						+ value);
			}

		} else if (wrapper instanceof PojoWrapper) {
			PojoWrapper<?> pwrapper = (PojoWrapper<?>) wrapper;

			JInvocation jnew = JExpr._new(model.ref(wrapper.getBaseClass()));
			for (Wrapper<?> arg : pwrapper.getArgs()) {
				JExpression jarg = generateExpressionForWrapper1(model, body,
						arg, context);
				jnew.arg(jarg);
			}
			JVar jvar = body.decl(model.ref(wrapper.getBaseClass()), "v"
					+ context.id(), jnew);

			for (Entry<Method, Wrapper<?>> entry : pwrapper.getSetters()
					.entrySet()) {
				JExpression jarg = generateExpressionForWrapper1(model, body,
						entry.getValue(), context);
				body.invoke(jvar, entry.getKey().getName()).arg(jarg);
			}

			return jvar;

		} else if (wrapper instanceof CastWrapper) {
			CastWrapper<?> cwrapper = (CastWrapper<?>) wrapper;

			JExpression jwrapper = generateExpressionForWrapper1(model, body,
					cwrapper.getWrapper(), context);
			return body.decl(model.ref(wrapper.getBaseClass()),
					"v" + context.id(),
					JExpr.cast(model.ref(wrapper.getBaseClass()), jwrapper));

		} else if (wrapper instanceof EqualsWrapper) {
			EqualsWrapper ewrapper = (EqualsWrapper) wrapper;

			JExpression jw1 = generateExpressionForWrapper1(model, body,
					ewrapper.getWrapper1(), context);
			JExpression jw2 = generateExpressionForWrapper1(model, body,
					ewrapper.getWrapper2(), context);

			return body.decl(model.ref(wrapper.getBaseClass()),
					"v" + context.id(), jw1.invoke("equals").arg(jw2));
		}

		throw new UnsupportedOperationException("Unknown Wrapper class "
				+ wrapper.getClass().getName());
	}

	private LinkedList<Method> getAsyncMethods(Class<?> clazz) {

		LinkedList<Method> list = new LinkedList<Method>();
		for (Method method : getAllMethods(clazz)) {

			// return type
			Class<?> returnType = method.getReturnType();
			if (getWrapperActualClass(returnType) == null) {
				continue;
			}

			// parameter types
			boolean ok = true;
			for (Class<?> parameterType : method.getParameterTypes()) {
				if (getWrapperActualClass(parameterType) == null) {
					ok = false;
					break;
				}
			}
			if (!ok) {
				continue;
			}

			list.add(method);
		}
		return list;
	}

	private Class<?> getWrapperActualClass(Class<?> clazz) {
		try {
			for (Type type : clazz.getGenericInterfaces()) {
				if (type instanceof ParameterizedType) {
					ParameterizedType ptype = (ParameterizedType) type;
					if (ptype.getRawType() == Wrapper.class) {
						return (Class<?>) ptype.getActualTypeArguments()[0];
					}
				}
			}
			return null;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// DUPLICATED IN WRAPPERGENERATOR

	private String defaultWrapperClassName(Class<?> clazz) {
		return unlangClassName(clazz) + "DefaultWrapper";
	}

	private String unlangClassName(Class<?> clazz) {
		return unboxClass(clazz).getName().replaceFirst("^java.lang.",
				"com.spotify.blasync.");
	}

	private Class<?> unboxClass(Class<?> clazz) {
		if (!clazz.isPrimitive()) {
			return clazz;
		} else if (clazz == boolean.class) {
			return Boolean.class;
		} else if (clazz == byte.class) {
			return Byte.class;
		} else if (clazz == short.class) {
			return Short.class;
		} else if (clazz == char.class) {
			return Character.class;
		} else if (clazz == int.class) {
			return Integer.class;
		} else if (clazz == long.class) {
			return Long.class;
		} else if (clazz == float.class) {
			return Float.class;
		} else if (clazz == double.class) {
			return Double.class;
		}
		throw new UnsupportedOperationException("Primitive type "
				+ clazz.getName() + " not supported");
	}

	private Method[] getAllMethods(Class<?> clazz) {
		// TODO(dfanjul): use clazz.getDeclaredMethods() and traverse hierarchy
		return clazz.getMethods();
	}

	private Type getFuturedType(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			if (ptype.getRawType() == ListenableFuture.class) {
				return ptype.getActualTypeArguments()[0];
			}
		}
		return null;
	}
}
