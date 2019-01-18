package org.apache.ibatis.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/*
 * 方法参数解析处理器对象
 *   本类本质的功能是:解析参数,同时根据给定的真实参数与对应的解析结构向匹配,从而能够识别对应的在sql语句中对应的参数
 */
public class ParamNameResolver {

	private static final String GENERIC_NAME_PREFIX = "param";

	/*
	 * 用于存储对应方法的参数结构
	 * 例如下面对应方式的解析结构
	 *  aMethod(@Param("M") int a, @Param("N") int b) 等价于 {{0, "M"}, {1,"N"}}
	 *  aMethod(int a, int b) 等价于   {{0, "0"}, {1, "1"}}
	 *  aMethod(int a, RowBounds rb, int b) 等价于 {{0, "0"}, {2, "1"}}
	 *  
	 *对应的键为参数所处的位置,值为对应的参数的名称
	 */
	private final SortedMap<Integer, String> names;

	//用于标识对应的方法参数中是否有Param注解
	private boolean hasParamAnnotation;

	/*
	 * 解析给定方法的参数信息
	 */
	public ParamNameResolver(Configuration config, Method method) {
		//获取方法中所有的参数类型
		final Class<?>[] paramTypes = method.getParameterTypes();
		//获取方法上参数对应的注解信息
		//获得了一个注解的二维数组。第一个维度对应参数列表里参数的数目，第二个维度对应参数列表里对应的注解。如上图的示例方法有四个参数。
		final Annotation[][] paramAnnotations = method.getParameterAnnotations();
		//创建对应的存储参数解析的有序集合map
		final SortedMap<Integer, String> map = new TreeMap<Integer, String>();
		//获取第一个维度对应参数列表里参数的数目
		int paramCount = paramAnnotations.length;
		//循环遍历所有参数的注解信息   主要是分析Param注解
		for (int paramIndex = 0; paramIndex < paramCount; paramIndex++) {
			//检测当前参数是否是特殊类型的参数
			if (isSpecialParameter(paramTypes[paramIndex])) {
				//跳过特殊类型的参数解析----------->即本参数类型不进入对应的存储集合中
				continue;
			}
			String name = null;
			//循环遍历参数的所有注解
			for (Annotation annotation : paramAnnotations[paramIndex]) {
				//检测当前的注解是否是Param注解
				if (annotation instanceof Param) {
					//设置有Param注解的标识
					hasParamAnnotation = true;
					//获取设置的参数名称信息
					name = ((Param) annotation).value();
					break;
				}
			}
			//检测是否通过分析注解获取到参数名称信息
			if (name == null) {
				//没有通过Param注解设定参数名称,检测是否使用真实的参数名称 
				if (config.isUseActualParamName()) {
					//尝试获取对应的参数的真实名称
					name = getActualParamName(method, paramIndex);
				}
				//检测是否获取到了真实参数名称
				if (name == null) {
					//use the parameter index as the name ("0", "1", ...) gcode issue #71
					//使用参数的位置索引来对应的参数名称
					name = String.valueOf(map.size());
				}
			}
			//将对应位置和对应的参数名称放置到集合中
			map.put(paramIndex, name);
		}
		//获取参数信息不变的集合对象
		names = Collections.unmodifiableSortedMap(map);
	}

	/*
	 * 尝试获取方法参数的真实参数名称
	 */
	private String getActualParamName(Method method, int paramIndex) {
		//检测当前是否支持获取对应的真实参数的名称的处理
		if (Jdk.parameterExists) {
			//获取对应位置上参数的真实名称
			return ParamNameUtil.getParamNames(method).get(paramIndex);
		}
		return null;
	}

	/*
	 * 检测对应的参数类型是否是特殊的参数类型
	 * 即 派生自RowBounds或者ResultHandler类型
	 */
	private static boolean isSpecialParameter(Class<?> clazz) {
		return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
	}

	/**
	 * Returns parameter names referenced by SQL providers.
	 */
	public String[] getNames() {
		return names.values().toArray(new String[0]);
	}

	/**
	 * <p>
	 * A single non-special parameter is returned without a name.<br />
	 * Multiple parameters are named using the naming rule.<br />
	 * In addition to the default names, this method also adds the generic names (param1, param2, ...).
	 * </p>
	 */
	public Object getNamedParams(Object[] args) {
		//获取需要配置参数值的个数
		final int paramCount = names.size();
		//检测是否需要配置对应的参数值
		if (args == null || paramCount == 0) {
			return null;
		} else if (!hasParamAnnotation && paramCount == 1) {
			//在没有Param注解,同时参数个数为1的情况下,直接在真实参数数组中获取对应的值对象
			return args[names.firstKey()];
		} else {
			//在多个参数情况下,创建对应的map集合来进行存储对应的参数值和参数位置的对应关系
			//对应的键是方法参数的真实名称,对应的值是真实调用方法是所对应的真实参数对应的值对象
			//注意在本集合中可能会添加一些类似于paramX的键值对参数
			final Map<String, Object> param = new ParamMap<Object>();
			int i = 0;
			//循环处理配置参数信息
			for (Map.Entry<Integer, String> entry : names.entrySet()) {
				//将对应的参数真实名称和对应的参数的真实值添加到集合中
				param.put(entry.getValue(), args[entry.getKey()]);
				// add generic param names (param1, param2, ...)
				//创建对应的通用参数值
				final String genericParamName = GENERIC_NAME_PREFIX + String.valueOf(i + 1);
				// ensure not to overwrite parameter named with @Param
				//检测参数真实名称中是否有对应的通用参数值
				if (!names.containsValue(genericParamName)) {
					//将对应的通用参数值也放置到集合中
					param.put(genericParamName, args[entry.getKey()]);
				}
				i++;
			}
			//返回对应的参数配置信息
			//键是对应的参数真实名称  值是调用方法时传入的真实参数值
			return param;
		}
	}
}
