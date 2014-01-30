package com.spotify.blasync;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;

public class WrapperGenerator {

	public void generate(JCodeModel model, List<Class<?>> classes)
			throws JClassAlreadyExistsException {

		LinkedList<Class<?>> allowedClasses = new LinkedList<Class<?>>(classes);
		allowedClasses.add(boolean.class);
		allowedClasses.add(byte.class);
		allowedClasses.add(short.class);
		allowedClasses.add(char.class);
		allowedClasses.add(int.class);
		allowedClasses.add(long.class);
		allowedClasses.add(float.class);
		allowedClasses.add(double.class);
		allowedClasses.add(Boolean.class);
		allowedClasses.add(Byte.class);
		allowedClasses.add(Short.class);
		allowedClasses.add(Character.class);
		allowedClasses.add(Integer.class);
		allowedClasses.add(Long.class);
		allowedClasses.add(Float.class);
		allowedClasses.add(Double.class);
		allowedClasses.add(String.class);
		allowedClasses.add(Object.class);

		for (Class<?> clazz : classes) {

			// Wrapper interface
			{
				JDefinedClass jclazz = model._class(wrapperClassName(clazz),
						ClassType.INTERFACE);
				jclazz._implements(model.ref(Wrapper.class).narrow(clazz));

				// wrapper methods
				generateWrapperMethods(model, jclazz, clazz, allowedClasses,
						false);
			}

			// DefaultWrapper class
			{
				JDefinedClass jclazz = model
						._class(defaultWrapperClassName(clazz));
				jclazz._extends(model.ref(DefaultWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// wrapper methods
				generateWrapperMethods(model, jclazz, clazz, allowedClasses,
						true);
			}

			// Delegated class
			{
				JDefinedClass jwclazz = model
						._class(delegatedWrapperClassName(clazz));
				jwclazz._extends(
						model.ref(DelegatedWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// constructor
				JMethod jconstr = jwclazz.constructor(JMod.PUBLIC);
				JVar jwrapper = jconstr.param(
						model.ref(Wrapper.class).narrow(model.ref("?")),
						"wrapper");
				JVar jmethod = jconstr.param(Method.class, "method");
				JVar jargs = jconstr
						.varParam(
								model.ref(Wrapper.class).narrow(model.ref("?")),
								"args");
				jconstr.body().invoke("super").arg(jwrapper).arg(jmethod)
						.arg(jargs);

				// wrapper methods
				generateWrapperMethods(model, jwclazz, clazz, allowedClasses,
						true);
			}

			// Literal class
			{
				JDefinedClass jclazzz = model._class(literalClassName(clazz));
				jclazzz._extends(model.ref(LiteralWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// constructor
				JMethod jconstr = jclazzz.constructor(JMod.PUBLIC);
				JVar jvalue = jconstr.param(clazz, "value");
				jconstr.body().invoke("super").arg(jvalue);

				// methods
				generateWrapperMethods(model, jclazzz, clazz, allowedClasses,
						true);
			}

			// Pojo class
			{
				JDefinedClass jclazz = model._class(pojoClassName(clazz));
				jclazz._extends(model.ref(PojoWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// constructor 1
				{
					JMethod jconstr = jclazz.constructor(JMod.PUBLIC);
					JVar jargs = jconstr.varParam(model.ref(Wrapper.class)
							.narrow(model.ref("?")), "args");
					jconstr.body()
							.invoke("super")
							.arg(model.ref(pojoClassName(clazz))
									.staticInvoke("getDefaultConstructor")
									.arg(model.ref(clazz).dotclass()))
							.arg(jargs);
				}

				// constructor 2
				{
					JMethod jconstr = jclazz.constructor(JMod.PUBLIC);
					JVar jconstructor = jconstr.param(
							model.ref(Constructor.class).narrow(clazz),
							"constructor");
					JVar jargs = jconstr.varParam(model.ref(Wrapper.class)
							.narrow(model.ref("?")), "args");
					jconstr.body().invoke("super").arg(jconstructor).arg(jargs);
				}

				// pojo methods
				generatePojoMethods(model, jclazz, clazz, allowedClasses);

				// wrapper methods
				generateWrapperMethods(model, jclazz, clazz, allowedClasses,
						true);
			}

			// Cast class
			{
				JDefinedClass jclazz = model
						._class(castWrapperClassName(clazz));
				jclazz._extends(model.ref(CastWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// constructor
				JMethod jconstr = jclazz.constructor(JMod.PUBLIC);
				JVar jwrapper = jconstr.param(
						model.ref(Wrapper.class).narrow(model.ref("?")),
						"wrapper");
				jconstr.body().invoke("super").arg(jwrapper);

				// wrapper methods
				generateWrapperMethods(model, jclazz, clazz, allowedClasses,
						true);
			}

			// If class
			{
				JDefinedClass jclazz = model._class(ifWrapperClassName(clazz));
				jclazz._extends(model.ref(IfWrapper.class).narrow(clazz))
						._implements(model.ref(wrapperClassName(clazz)));

				// constructor
				JMethod jconstr = jclazz.constructor(JMod.PUBLIC);
				JVar jiwcond = jconstr.param(
						model.ref(Wrapper.class).narrow(Boolean.class), "cond");
				JVar jiwwrap1 = jconstr.param(
						model.ref(Wrapper.class).narrow(clazz), "wrapper1");
				JVar jiwwrap2 = jconstr.param(
						model.ref(Wrapper.class).narrow(clazz), "wrapper2");
				jconstr.body().invoke("super").arg(jiwcond).arg(jiwwrap1)
						.arg(jiwwrap2);

				// wrapper methods
				generateWrapperMethods(model, jclazz, clazz, allowedClasses,
						true);
			}
		}
	}

	private void generateWrapperMethods(JCodeModel model,
			JDefinedClass jwclazz, Class<?> clazz,
			Collection<Class<?>> allowedClasses, boolean withBody) {

		// method getBaseClass
		if (withBody) {
			JMethod getBaseClass = jwclazz.method(JMod.PUBLIC,
					model.ref(Class.class).narrow(clazz), "getBaseClass");
			getBaseClass.body()._return(model.ref(clazz).dotclass());
		}

		HashSet<List<String>> processedMethods = new HashSet<List<String>>();
		for (Method method : getWrapperMethods(clazz, allowedClasses)) {

			// check method is already processed, there could be methods for
			// both boxed and unboxed types
			ArrayList<String> methodId = new ArrayList<String>();
			methodId.add(method.getName());
			Class<?>[] parameterTypes = method.getParameterTypes();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> ptype = parameterTypes[i];
				methodId.add(wrapperClassName(ptype));
			}
			if (processedMethods.contains(methodId)) {
				continue;
			}
			processedMethods.add(methodId);

			// method
			int mod = 0;
			if (Modifier.isPublic(method.getModifiers())) {
				mod = JMod.PUBLIC;
			} else if (Modifier.isProtected(method.getModifiers())) {
				mod = JMod.PROTECTED;
			} else {
				mod = JMod.PRIVATE;
			}
			JMethod jmethod = jwclazz.method(mod,
					model.ref(wrapperClassName(method.getGenericReturnType())),
					method.getName());

			// params
			ArrayList<JVar> jparams = new ArrayList<JVar>();
			for (int i = 0; i < parameterTypes.length; i++) {
				Class<?> ptype = parameterTypes[i];
				jparams.add(jmethod.param(model.ref(wrapperClassName(ptype)),
						"p" + i));
			}

			// body
			if (withBody) {
				JVar jvar = generateGetMethod(model, jmethod, clazz, method);
				JInvocation newWrapper = JExpr
						._new(model.ref(delegatedWrapperClassName(method
								.getGenericReturnType()))).arg(JExpr._this())
						.arg(jvar);
				for (int i = 0; i < parameterTypes.length; i++) {
					newWrapper.arg(jparams.get(i));
				}
				jmethod.body()._return(newWrapper);
			}
		}
	}

	private void generatePojoMethods(JCodeModel model, JDefinedClass jnwclazz,
			Class<?> clazz, Collection<Class<?>> allowedClasses) {

		HashSet<List<String>> processedMethods = new HashSet<List<String>>();
		for (Method method : getPojoMethods(clazz)) {

			ArrayList<String> methodId = new ArrayList<String>();
			methodId.add(method.getName());
			Class<?>[] parameterClasses = method.getParameterTypes();
			for (int i = 0; i < parameterClasses.length; i++) {
				methodId.add(wrapperClassName(parameterClasses[i]));
			}
			if (processedMethods.contains(methodId)) {
				continue;
			}
			processedMethods.add(methodId);

			// Pojo class: method
			int mod = 0;
			if (Modifier.isPublic(method.getModifiers())) {
				mod = JMod.PUBLIC;
			} else if (Modifier.isProtected(method.getModifiers())) {
				mod = JMod.PROTECTED;
			} else {
				mod = JMod.PRIVATE;
			}
			JMethod jmethod = jnwclazz.method(mod, jnwclazz, method.getName());

			// Pojo class: params
			Type parameterType = method.getGenericParameterTypes()[0];
			Type listType = getListType(parameterType);
			JClass jparameterType;
			if (listType == null) {
				jparameterType = model.ref(List.class).narrow(
						model.ref(wrapperClassName(parameterType)));
			} else {
				jparameterType = model.ref(wrapperClassName(listType));
			}
			JVar jvalue = jmethod.param(jparameterType, "value");

			// Pojo class: body
			JVar jgetMethod = generateGetMethod(model, jmethod, clazz, method);
			JVar jnext = jmethod.body().decl(
					jnwclazz,
					"next",
					JExpr._new(jnwclazz)
							.arg(JExpr._this().invoke("getConstructor"))
							.arg(JExpr._this().invoke("getArgs")));
			jmethod.body().invoke(jnext.invoke("getSetters"), "putAll")
					.arg(JExpr._this().invoke("getSetters"));
			jmethod.body().invoke(jnext.invoke("getSetters"), "put")
					.arg(jgetMethod).arg(jvalue);
			jmethod.body()._return(jnext);
		}
	}

	private JVar generateGetMethod(JCodeModel model, JMethod jmethod,
			Class<?> clazz, Method method) {

		JInvocation jgetMethod = model.ref(clazz).dotclass()
				.invoke("getMethod").arg(method.getName());
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			jgetMethod.arg(model.ref(unboxClass(parameterTypes[i])).dotclass());
		}

		JVar jvar = jmethod.body().decl(model.ref(Method.class), "method");
		JTryBlock jtry = jmethod.body()._try();
		jtry.body().assign(jvar, jgetMethod);
		JCatchBlock jcatch = jtry
				._catch(model.ref(NoSuchMethodException.class));
		JVar jexception = jcatch.param("e");
		jcatch.body()._throw(
				JExpr._new(model.ref(RuntimeException.class)).arg(jexception));
		return jvar;
	}

	private String wrapperClassName(Type type) {
		return unlangClassName(type) + "Wrapper";
	}

	private String defaultWrapperClassName(Type type) {
		return unlangClassName(type) + "DefaultWrapper";
	}

	private String delegatedWrapperClassName(Type type) {
		return unlangClassName(type) + "DelegateWrapper";
	}

	private String literalClassName(Type type) {
		return unlangClassName(type) + "LiteralWrapper";
	}

	private String castWrapperClassName(Type type) {
		return unlangClassName(type) + "CastWrapper";
	}

	private String ifWrapperClassName(Type type) {
		return unlangClassName(type) + "IfWrapper";
	}

	private String pojoClassName(Type type) {
		return unlangClassName(type) + "PojoWrapper";
	}

	private String unlangClassName(Type type) {
		return unfutureClass(type).getName().replaceFirst("^java.lang.",
				"com.spotify.blasync.");
	}

	private Class<?> unfutureClass(Type type) {
		Type futuredType = getFuturedType(type);
		if (futuredType != null) {
			type = futuredType;
		}
		return unboxClass((Class<?>) type);
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

	private LinkedList<Method> getWrapperMethods(Class<?> clazz,
			Collection<Class<?>> allowedClasses) {

		LinkedList<Method> list = new LinkedList<Method>();
		for (Method method : getAllMethods(clazz)) {

			String name = method.getName();
			int modifiers = method.getModifiers();
			Type[] parameterTypes = method.getGenericParameterTypes();
			Type returnType = method.getGenericReturnType();

			// no static methods
			if (Modifier.isStatic(modifiers)) {
				continue;
			}

			// exclude some methods
			if ("toString".equals(name) && parameterTypes.length == 0
					&& returnType == String.class) {
				continue;
			} else if ("equals".equals(name) && parameterTypes.length == 1
					&& parameterTypes[0] == Object.class
					&& returnType == boolean.class) {
				continue;
			} else if ("hashCode".equals(name) && parameterTypes.length == 0
					&& returnType == int.class) {
				continue;
			}

			// check return type
			if (returnType == void.class) {
				continue;
			}
			if (!isAllowedWrapperType(returnType, allowedClasses)) {
				continue;
			}

			// check parameter types
			boolean ok = true;
			for (Type paramType : method.getGenericParameterTypes()) {
				if (!isAllowedWrapperType(paramType, allowedClasses)) {
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

	private boolean isAllowedWrapperType(Type type,
			Collection<Class<?>> allowedClasses) {

		Type futuredType = getFuturedType(type);
		if (futuredType != null) {
			type = futuredType;
		}
		Type listType = getListType(type);
		if (listType != null) {
			type = listType;
		}
		return allowedClasses.contains(type);
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

	private Type getListType(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType ptype = (ParameterizedType) type;
			if (ptype.getRawType() == List.class) {
				return ptype.getActualTypeArguments()[0];
			}
		}
		return null;
	}

	private LinkedList<Method> getPojoMethods(Class<?> clazz) {

		LinkedList<Method> list = new LinkedList<Method>();
		for (Method method : getAllMethods(clazz)) {

			if (!method.getName().startsWith("set")) {
				continue;
			}
			if (method.getReturnType() != void.class) {
				continue;
			}
			if (method.getParameterTypes().length != 1) {
				continue;
			}

			list.add(method);
		}
		return list;
	}

	private Method[] getAllMethods(Class<?> clazz) {
		// TODO(dfanjul): use clazz.getDeclaredMethods() and traverse hierarchy
		return clazz.getMethods();
	}
}
