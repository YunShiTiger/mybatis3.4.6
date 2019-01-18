package org.apache.ibatis.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.lang.UsesJava8;

/*
 * 用于解析方法参数的真实名称的处理类
 */
@UsesJava8
public class ParamNameUtil {
	
	/*
	 * 执行获取方法的真实参数名称的列表
	 */
	public static List<String> getParamNames(Method method) {
		return getParameterNames(method);
	}

	/*
	 * 执行获取构造方法的真实参数名称的列表
	 */
	public static List<String> getParamNames(Constructor<?> constructor) {
		return getParameterNames(constructor);
	}

	/*
	 * 获取给定实体对应的参数名称的列表信息
	 */
	private static List<String> getParameterNames(Executable executable) {
		//创建存储参数真实名称的列表对象
		final List<String> names = new ArrayList<String>();
		//获取给定实体对应的所有参数数组
		final Parameter[] params = executable.getParameters();
		//循环遍历对应的参数数组
		for (Parameter param : params) {
			//获取对应参数的真实名称
			names.add(param.getName());
		}
		//返回获取到的真实参数的名称列表
		return names;
	}

	private ParamNameUtil() {
		super();
	}
}
