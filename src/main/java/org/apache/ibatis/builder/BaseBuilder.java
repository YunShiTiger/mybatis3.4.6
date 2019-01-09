package org.apache.ibatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * 
 */
public abstract class BaseBuilder {
	
	//记录对应的配置信息类对象
	protected final Configuration configuration;
	//记录对应的别名注册器对象
	protected final TypeAliasRegistry typeAliasRegistry;
	//记录对应的类型处理器注册对象
	protected final TypeHandlerRegistry typeHandlerRegistry;

	public BaseBuilder(Configuration configuration) {
		this.configuration = configuration;
		this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
		this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	protected Pattern parseExpression(String regex, String defaultValue) {
		return Pattern.compile(regex == null ? defaultValue : regex);
	}

	protected Boolean booleanValueOf(String value, Boolean defaultValue) {
		return value == null ? defaultValue : Boolean.valueOf(value);
	}

	protected Integer integerValueOf(String value, Integer defaultValue) {
		return value == null ? defaultValue : Integer.valueOf(value);
	}

	protected Set<String> stringSetValueOf(String value, String defaultValue) {
		value = (value == null ? defaultValue : value);
		return new HashSet<String>(Arrays.asList(value.split(",")));
	}

	protected JdbcType resolveJdbcType(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return JdbcType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
		}
	}

	protected ResultSetType resolveResultSetType(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return ResultSetType.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
		}
	}

	protected ParameterMode resolveParameterMode(String alias) {
		if (alias == null) {
			return null;
		}
		try {
			return ParameterMode.valueOf(alias);
		} catch (IllegalArgumentException e) {
			throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
		}
	}

	protected Object createInstance(String alias) {
		Class<?> clazz = resolveClass(alias);
		if (clazz == null) {
			return null;
		}
		try {
			return resolveClass(alias).newInstance();
		} catch (Exception e) {
			throw new BuilderException("Error creating instance. Cause: " + e, e);
		}
	}

	/*
	 * 通过给定的别名来获取对应的类对象
	 */
	protected Class<?> resolveClass(String alias) {
		//检测给定的别名参数是否为空
		if (alias == null) {
			//返回对应的空对象
			return null;
		}
		try {
			//通过别名注册器来解析对应的别名获取对应的类对象
			return resolveAlias(alias);
		} catch (Exception e) {
			throw new BuilderException("Error resolving class. Cause: " + e, e);
		}
	}

	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
		if (typeHandlerAlias == null) {
			return null;
		}
		Class<?> type = resolveClass(typeHandlerAlias);
		if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
			throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
		}
		@SuppressWarnings("unchecked") // already verified it is a TypeHandler
		Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
		return resolveTypeHandler(javaType, typeHandlerType);
	}

	protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
		if (typeHandlerType == null) {
			return null;
		}
		// javaType ignored for injected handlers see issue #746 for full detail
		TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
		if (handler == null) {
			// not in registry, create a new one
			handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
		}
		return handler;
	}

	/*
	 * 通过别名注册器解析别名对应的类对象
	 * 
	 *   通过这个地方也可以发现  别名注册器中不仅需要完成 匹配已经存在的别名对应关系,同时也也要完成不存在的情况下,需要使用资源进行加载处理
	 */
	protected Class<?> resolveAlias(String alias) {
		return typeAliasRegistry.resolveAlias(alias);
	}
}
