package org.apache.ibatis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;

/**
 * 别名注册器对象处理类
 *   本类主要完成对别名的注册处理,以及对应的查询操作
 */
public class TypeAliasRegistry {

	//用于存储别名和对应类的对应关系的集合
	private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();

	public TypeAliasRegistry() {
		//在创建对象的同时将一些常用的别名注册到系统中----------->即后期需要注册的别名都是为完成特定功能的实体对象的别名
		registerAlias("string", String.class);

		registerAlias("byte", Byte.class);
		registerAlias("long", Long.class);
		registerAlias("short", Short.class);
		registerAlias("int", Integer.class);
		registerAlias("integer", Integer.class);
		registerAlias("double", Double.class);
		registerAlias("float", Float.class);
		registerAlias("boolean", Boolean.class);

		registerAlias("byte[]", Byte[].class);
		registerAlias("long[]", Long[].class);
		registerAlias("short[]", Short[].class);
		registerAlias("int[]", Integer[].class);
		registerAlias("integer[]", Integer[].class);
		registerAlias("double[]", Double[].class);
		registerAlias("float[]", Float[].class);
		registerAlias("boolean[]", Boolean[].class);

		registerAlias("_byte", byte.class);
		registerAlias("_long", long.class);
		registerAlias("_short", short.class);
		registerAlias("_int", int.class);
		registerAlias("_integer", int.class);
		registerAlias("_double", double.class);
		registerAlias("_float", float.class);
		registerAlias("_boolean", boolean.class);

		registerAlias("_byte[]", byte[].class);
		registerAlias("_long[]", long[].class);
		registerAlias("_short[]", short[].class);
		registerAlias("_int[]", int[].class);
		registerAlias("_integer[]", int[].class);
		registerAlias("_double[]", double[].class);
		registerAlias("_float[]", float[].class);
		registerAlias("_boolean[]", boolean[].class);

		registerAlias("date", Date.class);
		registerAlias("decimal", BigDecimal.class);
		registerAlias("bigdecimal", BigDecimal.class);
		registerAlias("biginteger", BigInteger.class);
		registerAlias("object", Object.class);

		registerAlias("date[]", Date[].class);
		registerAlias("decimal[]", BigDecimal[].class);
		registerAlias("bigdecimal[]", BigDecimal[].class);
		registerAlias("biginteger[]", BigInteger[].class);
		registerAlias("object[]", Object[].class);

		registerAlias("map", Map.class);
		registerAlias("hashmap", HashMap.class);
		registerAlias("list", List.class);
		registerAlias("arraylist", ArrayList.class);
		registerAlias("collection", Collection.class);
		registerAlias("iterator", Iterator.class);

		registerAlias("ResultSet", ResultSet.class);
	}

	/*
	 * 根据提供的别名参数来查询对应的类对象
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<T> resolveAlias(String string) {
		try {
			//检测给定的别名参数是否存在
			if (string == null) {
				return null;
			}
			//转换成对应的小写形式
			String key = string.toLowerCase(Locale.ENGLISH);
			Class<T> value;
			//检测对应的别名是否存在
			if (TYPE_ALIASES.containsKey(key)) {
				//在集合中获取对应的映射类对象
				value = (Class<T>) TYPE_ALIASES.get(key);
			} else {
				//尝试根据给定参数进行加载对应的类对象
				value = (Class<T>) Resources.classForName(string);
			}
			//返回别名对应的类对象
			return value;
		} catch (ClassNotFoundException e) {
			throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
		}
	}

	/*
	 * 通过提供包名的方式进行批量注册别名的处理
	 */
	public void registerAliases(String packageName) {
		//设置默认继承的类为Object
		registerAliases(packageName, Object.class);
	}

	/*
	 * 通过给定的包名和需要继承的类来进行批量注册别名操作处理
	 */
	public void registerAliases(String packageName, Class<?> superType) {
		//创建对应的解析器对象
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		//解析满足在指定包下是继承自指定类的所有资源类
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		//获取满足资源的所有类对象
		Set<Class<? extends Class<?>>> typeSet = resolverUtil.getClasses();
		//循环遍历所有的类对象,进行注册别名操作处理
		for (Class<?> type : typeSet) {
			// Ignore inner classes and interfaces (including package-info.java) Skip also inner classes. See issue #6
			//对匿名内部类   接口类型  成员内部类  进行过滤操作处理 
			if (!type.isAnonymousClass() && !type.isInterface() && !type.isMemberClass()) {
				//最终完成对应的注册操作处理
				registerAlias(type);
			}
		}
	}

	/*
	 * 根据提供的类进行别名注册操作处理
	 */
	public void registerAlias(Class<?> type) {
		//获取类对象对应的简单类名
		String alias = type.getSimpleName();
		//获取类对象上设置的Alias别名注解
		Alias aliasAnnotation = type.getAnnotation(Alias.class);
		//检测是否设置了别名注解
		if (aliasAnnotation != null) {
			//获取对应的别名
			alias = aliasAnnotation.value();
		}
		//进行别名注册操作处理
		registerAlias(alias, type);
	}
	
	/*
	 * 通过给定别名和类名进行注册操作处理
	 */
	public void registerAlias(String alias, String value) {
		try {
			//加载对应的类并进行注册别名操作处理
			registerAlias(alias, Resources.classForName(value));
		} catch (ClassNotFoundException e) {
			throw new TypeException("Error registering type alias " + alias + " for " + value + ". Cause: " + e, e);
		}
	}

	/*
	 * 进行别名和对应类的注册处理
	 *    最终的注册必须要要走的处理方法
	 */
	public void registerAlias(String alias, Class<?> value) {
		//首先检测给定的别名是否存在
		if (alias == null) {
			throw new TypeException("The parameter alias cannot be null");
		}
		//获取别名对应的小写形式
		String key = alias.toLowerCase(Locale.ENGLISH);
		//检测设置的别名是否有冲突的情况,即一个别名对应了对个类对象的处理
		if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
			throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
		}
		//将对应的别名和类添加到注册器集合中
		TYPE_ALIASES.put(key, value);
	}

	/**
	 * @since 3.2.2
	 */
	public Map<String, Class<?>> getTypeAliases() {
		return Collections.unmodifiableMap(TYPE_ALIASES);
	}

}
