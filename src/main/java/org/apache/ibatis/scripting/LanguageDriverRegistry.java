package org.apache.ibatis.scripting;

import java.util.HashMap;
import java.util.Map;

/**
 * 进行sql语句操作处理的语句操作驱动对象注册管理处理类
 *   即注册所有使用到的进行sql语句生成处理的驱动对象
 */
public class LanguageDriverRegistry {

	//用于存储对应的sql语句处理驱动对象的集合
	private final Map<Class<?>, LanguageDriver> LANGUAGE_DRIVER_MAP = new HashMap<Class<?>, LanguageDriver>();

	//记录默认的语句处理驱动对象
	private Class<?> defaultDriverClass;

	/*
	 * 根据类型将对应的语言处理驱动对象注册到集合中
	 */
	public void register(Class<?> cls) {
		//检测对应的类型是否传入
		if (cls == null) {
			throw new IllegalArgumentException("null is not a valid Language Driver");
		}
		//检测对应的类型是否已经在集合中
		if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
			try {
				//注册对应的语言驱动对象到集合中
				LANGUAGE_DRIVER_MAP.put(cls, (LanguageDriver) cls.newInstance());
			} catch (Exception ex) {
				throw new ScriptingException("Failed to load language driver for " + cls.getName(), ex);
			}
		}
	}

	/*
	 * 直接将对应的语句驱动对象注册到集合中
	 */
	public void register(LanguageDriver instance) {
		//检测到对应的对象是否为空
		if (instance == null) {
			throw new IllegalArgumentException("null is not a valid Language Driver");
		}
		//获取对象对应的类型信息
		Class<?> cls = instance.getClass();
		//检测本类型是否已经注册到集合中
		if (!LANGUAGE_DRIVER_MAP.containsKey(cls)) {
			//将对应的类型注册到集合中
			LANGUAGE_DRIVER_MAP.put(cls, instance);
		}
	}

	/*
	 * 根据给定的类型获取对应的语句处理驱动对象
	 */
	public LanguageDriver getDriver(Class<?> cls) {
		return LANGUAGE_DRIVER_MAP.get(cls);
	}

	/*
	 * 获取默认的语句处理驱动对象
	 */
	public LanguageDriver getDefaultDriver() {
		return getDriver(getDefaultDriverClass());
	}

	public Class<?> getDefaultDriverClass() {
		return defaultDriverClass;
	}

	/*
	 * 将默认的驱动对象注册到集合中,同时设置对应的默认语言驱动器对象
	 */
	public void setDefaultDriverClass(Class<?> defaultDriverClass) {
		register(defaultDriverClass);
		this.defaultDriverClass = defaultDriverClass;
	}

}
