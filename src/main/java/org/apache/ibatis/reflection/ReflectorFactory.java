package org.apache.ibatis.reflection;

/**
 * 反射类处理工厂
 *   主要用于完成通过本工厂来完成获取对应类所对应的反射操作处理类对象
 */
public interface ReflectorFactory {

	//检测是否具有缓存反射处理类功能
	boolean isClassCacheEnabled();

	//设置是否开启对应的缓存反射处理类功能
	void setClassCacheEnabled(boolean classCacheEnabled);

	//获取对应的类所对应的反射处理类对象
	Reflector findForClass(Class<?> type);
	
}