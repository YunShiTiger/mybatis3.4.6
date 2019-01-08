package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * 属性检测处理类对象
 *   主要用于检测提供的方法名称是否是属性方法,同时获取对应的方法对应的属性名称
 */
public final class PropertyNamer {

	private PropertyNamer() {
		// Prevent Instantiation of Static Class
	}

	/*
	 * 将提供的方法名称转换成对应的属性对应的名称
	 */
	public static String methodToProperty(String name) {
		//首先将对应的is或者get或者set字符串进行截取操作处理--------->即获取后面的属性名称(注意此处的属性名称一般首字母是大写的---->所以后面要进行转换成对应的小写操作处理)
		if (name.startsWith("is")) {
			name = name.substring(2);
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			throw new ReflectionException("Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
		}
		//进行字符串拼接操作处理------->即将对应的首字母变成小写
		if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}
		//返回处理后的属性字符串
		return name;
	}

	/*
	 * 检测提供的name是否是操作属性方法
	 */
	public static boolean isProperty(String name) {
		return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
	}

	/*
	 * 检测提供的name是否是获取属性的方法
	 */
	public static boolean isGetter(String name) {
		return name.startsWith("get") || name.startsWith("is");
	}

	/*
	 * 检测提供的name是否是设置属性的方法
	 */
	public static boolean isSetter(String name) {
		return name.startsWith("set");
	}

}
