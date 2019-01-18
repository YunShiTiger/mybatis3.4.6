package org.apache.ibatis.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

/**
 * 本类主要完成创建mapper对应代理对象的工厂处理类
 *   此处需要明确一点  每次根据方法获取的代理对象都是不一样的,
 *     即每次都是新创建一个对应的代理对象,但是这个地方创建的缓存对象确实对所有创建的代理对象公用的,所以定义在此处最为合适 
 */
public class MapperProxyFactory<T> {

	//用于记录需要代理的mapper类型
	private final Class<T> mapperInterface;
	//用于记录对应mapper中方法和对应的执行sql的对应关系--------------------------->即本接口方法对应的真实需要执行的sql语句---------------->注意本类是具有线程同步的集合
	private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();

	/*
	 * 根据提供的mapper类型构建对应代理工厂的构造函数
	 */
	public MapperProxyFactory(Class<T> mapperInterface) {
		this.mapperInterface = mapperInterface;
	}

	public Class<T> getMapperInterface() {
		return mapperInterface;
	}

	public Map<Method, MapperMethod> getMethodCache() {
		return methodCache;
	}

	/*
	 * 真实用于创建对应接口的代理对象的处理----------------------->此处使用jdk提供的动态代理机制
	 */
	@SuppressWarnings("unchecked")
	protected T newInstance(MapperProxy<T> mapperProxy) {
		return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
	}

	/*
	 * 对外提供获取mapper对应的代理对象的处理
	 */
	public T newInstance(SqlSession sqlSession) {
		//定义一个动态调用的控制器对象
		final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
		//创建真实的代理对象的处理
		return newInstance(mapperProxy);
	}

}
