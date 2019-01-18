package org.apache.ibatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.ibatis.lang.UsesJava7;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.SqlSession;

/**
 * 进行mapper动态调用的控制器处理类
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

	private static final long serialVersionUID = -6424540398559729838L;
	//记录对应的回话对象
	private final SqlSession sqlSession;
	//记录对应的接口mapper
	private final Class<T> mapperInterface;
	//记录对应的方法和对应sql语句的映射关系的缓存集合
	private final Map<Method, MapperMethod> methodCache;

	public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
		this.sqlSession = sqlSession;
		this.mapperInterface = mapperInterface;
		this.methodCache = methodCache;
	}

	/*
	 * 此方法是对应的核心部分------------------->即对应的代理对象需要出发的方法一定会在此处进行触发处理
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			//检测对应的方法是否是Object中的方法
			if (Object.class.equals(method.getDeclaringClass())) {
				//直接进行触发操作处理
				return method.invoke(this, args);
			} else if (isDefaultMethod(method)) {
				return invokeDefaultMethod(proxy, method, args);
			}
		} catch (Throwable t) {
			throw ExceptionUtil.unwrapThrowable(t);
		}
		//通过缓存获取对应的MapperMethod对象
		final MapperMethod mapperMethod = cachedMapperMethod(method);
		//通过获取到的MapperMethod对象,进行执行对应的sql语句
		return mapperMethod.execute(sqlSession, args);
	}

	/*
	 * 根据提供的方法获取对应的sqlMapperMethod方法
	 */
	private MapperMethod cachedMapperMethod(Method method) {
		//首先哎集合中查找方法对应的MapperMethod对象
		MapperMethod mapperMethod = methodCache.get(method);
		//检测集合中是否存在对应的MapperMethod对象
		if (mapperMethod == null) {
			//创建对应的MapperMethod对象
			mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
			//将对应的MapperMethod对象添加到集合中
			methodCache.put(method, mapperMethod);
		}
		//返回对应的MapperMethod对象
		return mapperMethod;
	}

	@UsesJava7
	private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
		final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
		if (!constructor.isAccessible()) {
			constructor.setAccessible(true);
		}
		final Class<?> declaringClass = method.getDeclaringClass();
		return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC).unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
	}

	/**
	 * Backport of java.lang.reflect.Method#isDefault()
	 */
	private boolean isDefaultMethod(Method method) {
		return (method.getModifiers() & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC && method.getDeclaringClass().isInterface();
	}
	
}
