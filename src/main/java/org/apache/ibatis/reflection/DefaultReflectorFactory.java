package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认的处理反射对象的工厂------->主要完成了缓存操作处理
 */
public class DefaultReflectorFactory implements ReflectorFactory {
	
	//用于标识是否开启对应的缓存操作处理
	private boolean classCacheEnabled = true;
	//用于记录对应的已经解析的类的反射处理类------------>注意这个地方使用的具有线程同步操作处理的map集合对象
	private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<Class<?>, Reflector>();

	public DefaultReflectorFactory() {
		
	}

	@Override
	public boolean isClassCacheEnabled() {
		return classCacheEnabled;
	}

	@Override
	public void setClassCacheEnabled(boolean classCacheEnabled) {
		this.classCacheEnabled = classCacheEnabled;
	}

	@Override
	public Reflector findForClass(Class<?> type) {
		//检测是否开启了缓存功能
		if (classCacheEnabled) {
			// synchronized (type) removed see issue #461
			//首先在对应的缓存集合中查询是否有对应的解析完成的反射处理类
			Reflector cached = reflectorMap.get(type);
			//检测是否在缓存对象中找到对应的反射处理类
			if (cached == null) {
				//创建并解析对应的反射处理类
				cached = new Reflector(type);
				//将对应的解析完成的反射处理类放置到缓存集合中进行存储处理
				reflectorMap.put(type, cached);
			}
			//返回对应的反射处理类对象
			return cached;
		} else {
			//没有开始缓存功能,就直接创建对应的反射处理类对象
			return new Reflector(type);
		}
	}

}
