package org.apache.ibatis.type;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.Jdk;

/**
 * 
 */
public final class TypeHandlerRegistry {

	private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<JdbcType, TypeHandler<?>>(JdbcType.class);
	private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new ConcurrentHashMap<Type, Map<JdbcType, TypeHandler<?>>>();
	private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknownTypeHandler(this);
	private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<Class<?>, TypeHandler<?>>();

	private static final Map<JdbcType, TypeHandler<?>> NULL_TYPE_HANDLER_MAP = Collections.emptyMap();

	private Class<? extends TypeHandler> defaultEnumTypeHandler = EnumTypeHandler.class;

	public TypeHandlerRegistry() {
		register(Boolean.class, new BooleanTypeHandler());
		register(boolean.class, new BooleanTypeHandler());
		register(JdbcType.BOOLEAN, new BooleanTypeHandler());
		register(JdbcType.BIT, new BooleanTypeHandler());

		register(Byte.class, new ByteTypeHandler());
		register(byte.class, new ByteTypeHandler());
		register(JdbcType.TINYINT, new ByteTypeHandler());

		register(Short.class, new ShortTypeHandler());
		register(short.class, new ShortTypeHandler());
		register(JdbcType.SMALLINT, new ShortTypeHandler());

		register(Integer.class, new IntegerTypeHandler());
		register(int.class, new IntegerTypeHandler());
		register(JdbcType.INTEGER, new IntegerTypeHandler());

		register(Long.class, new LongTypeHandler());
		register(long.class, new LongTypeHandler());

		register(Float.class, new FloatTypeHandler());
		register(float.class, new FloatTypeHandler());
		register(JdbcType.FLOAT, new FloatTypeHandler());

		register(Double.class, new DoubleTypeHandler());
		register(double.class, new DoubleTypeHandler());
		register(JdbcType.DOUBLE, new DoubleTypeHandler());

		register(Reader.class, new ClobReaderTypeHandler());
		register(String.class, new StringTypeHandler());
		register(String.class, JdbcType.CHAR, new StringTypeHandler());
		register(String.class, JdbcType.CLOB, new ClobTypeHandler());
		register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
		register(String.class, JdbcType.LONGVARCHAR, new ClobTypeHandler());
		register(String.class, JdbcType.NVARCHAR, new NStringTypeHandler());
		register(String.class, JdbcType.NCHAR, new NStringTypeHandler());
		register(String.class, JdbcType.NCLOB, new NClobTypeHandler());
		register(JdbcType.CHAR, new StringTypeHandler());
		register(JdbcType.VARCHAR, new StringTypeHandler());
		register(JdbcType.CLOB, new ClobTypeHandler());
		register(JdbcType.LONGVARCHAR, new ClobTypeHandler());
		register(JdbcType.NVARCHAR, new NStringTypeHandler());
		register(JdbcType.NCHAR, new NStringTypeHandler());
		register(JdbcType.NCLOB, new NClobTypeHandler());

		register(Object.class, JdbcType.ARRAY, new ArrayTypeHandler());
		register(JdbcType.ARRAY, new ArrayTypeHandler());

		register(BigInteger.class, new BigIntegerTypeHandler());
		register(JdbcType.BIGINT, new LongTypeHandler());

		register(BigDecimal.class, new BigDecimalTypeHandler());
		register(JdbcType.REAL, new BigDecimalTypeHandler());
		register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
		register(JdbcType.NUMERIC, new BigDecimalTypeHandler());

		register(InputStream.class, new BlobInputStreamTypeHandler());
		register(Byte[].class, new ByteObjectArrayTypeHandler());
		register(Byte[].class, JdbcType.BLOB, new BlobByteObjectArrayTypeHandler());
		register(Byte[].class, JdbcType.LONGVARBINARY, new BlobByteObjectArrayTypeHandler());
		register(byte[].class, new ByteArrayTypeHandler());
		register(byte[].class, JdbcType.BLOB, new BlobTypeHandler());
		register(byte[].class, JdbcType.LONGVARBINARY, new BlobTypeHandler());
		register(JdbcType.LONGVARBINARY, new BlobTypeHandler());
		register(JdbcType.BLOB, new BlobTypeHandler());

		register(Object.class, UNKNOWN_TYPE_HANDLER);
		register(Object.class, JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);
		register(JdbcType.OTHER, UNKNOWN_TYPE_HANDLER);

		register(Date.class, new DateTypeHandler());
		register(Date.class, JdbcType.DATE, new DateOnlyTypeHandler());
		register(Date.class, JdbcType.TIME, new TimeOnlyTypeHandler());
		register(JdbcType.TIMESTAMP, new DateTypeHandler());
		register(JdbcType.DATE, new DateOnlyTypeHandler());
		register(JdbcType.TIME, new TimeOnlyTypeHandler());

		register(java.sql.Date.class, new SqlDateTypeHandler());
		register(java.sql.Time.class, new SqlTimeTypeHandler());
		register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());

		// mybatis-typehandlers-jsr310
		if (Jdk.dateAndTimeApiExists) {
			this.register(Instant.class, InstantTypeHandler.class);
			this.register(LocalDateTime.class, LocalDateTimeTypeHandler.class);
			this.register(LocalDate.class, LocalDateTypeHandler.class);
			this.register(LocalTime.class, LocalTimeTypeHandler.class);
			this.register(OffsetDateTime.class, OffsetDateTimeTypeHandler.class);
			this.register(OffsetTime.class, OffsetTimeTypeHandler.class);
			this.register(ZonedDateTime.class, ZonedDateTimeTypeHandler.class);
			this.register(Month.class, MonthTypeHandler.class);
			this.register(Year.class, YearTypeHandler.class);
			this.register(YearMonth.class, YearMonthTypeHandler.class);
			this.register(JapaneseDate.class, JapaneseDateTypeHandler.class);
		}

		// issue #273
		register(Character.class, new CharacterTypeHandler());
		register(char.class, new CharacterTypeHandler());
	}

	/**
	 * Set a default {@link TypeHandler} class for {@link Enum}. A default
	 * {@link TypeHandler} is {@link org.apache.ibatis.type.EnumTypeHandler}.
	 * 
	 * @param typeHandler a type handler class for {@link Enum}
	 * @since 3.4.5
	 */
	public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler) {
		this.defaultEnumTypeHandler = typeHandler;
	}

	public boolean hasTypeHandler(Class<?> javaType) {
		return hasTypeHandler(javaType, null);
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference) {
		return hasTypeHandler(javaTypeReference, null);
	}

	public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
		return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
	}

	public boolean hasTypeHandler(TypeReference<?> javaTypeReference, JdbcType jdbcType) {
		return javaTypeReference != null && getTypeHandler(javaTypeReference, jdbcType) != null;
	}

	public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
		return ALL_TYPE_HANDLERS_MAP.get(handlerType);
	}

	public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
		return getTypeHandler((Type) type, null);
	}

	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference) {
		return getTypeHandler(javaTypeReference, null);
	}

	public TypeHandler<?> getTypeHandler(JdbcType jdbcType) {
		return JDBC_TYPE_HANDLER_MAP.get(jdbcType);
	}

	public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
		return getTypeHandler((Type) type, jdbcType);
	}

	public <T> TypeHandler<T> getTypeHandler(TypeReference<T> javaTypeReference, JdbcType jdbcType) {
		return getTypeHandler(javaTypeReference.getRawType(), jdbcType);
	}

	@SuppressWarnings("unchecked")
	private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
		if (ParamMap.class.equals(type)) {
			return null;
		}
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = getJdbcHandlerMap(type);
		TypeHandler<?> handler = null;
		if (jdbcHandlerMap != null) {
			handler = jdbcHandlerMap.get(jdbcType);
			if (handler == null) {
				handler = jdbcHandlerMap.get(null);
			}
			if (handler == null) {
				// #591
				handler = pickSoleHandler(jdbcHandlerMap);
			}
		}
		// type drives generics here
		return (TypeHandler<T>) handler;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMap(Type type) {
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
		if (NULL_TYPE_HANDLER_MAP.equals(jdbcHandlerMap)) {
			return null;
		}
		if (jdbcHandlerMap == null && type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			if (clazz.isEnum()) {
				jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(clazz, clazz);
				if (jdbcHandlerMap == null) {
					register(clazz, getInstance(clazz, defaultEnumTypeHandler));
					return TYPE_HANDLER_MAP.get(clazz);
				}
			} else {
				jdbcHandlerMap = getJdbcHandlerMapForSuperclass(clazz);
			}
		}
		TYPE_HANDLER_MAP.put(type, jdbcHandlerMap == null ? NULL_TYPE_HANDLER_MAP : jdbcHandlerMap);
		return jdbcHandlerMap;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForEnumInterfaces(Class<?> clazz, Class<?> enumClazz) {
		for (Class<?> iface : clazz.getInterfaces()) {
			Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(iface);
			if (jdbcHandlerMap == null) {
				jdbcHandlerMap = getJdbcHandlerMapForEnumInterfaces(iface, enumClazz);
			}
			if (jdbcHandlerMap != null) {
				// Found a type handler regsiterd to a super interface
				HashMap<JdbcType, TypeHandler<?>> newMap = new HashMap<JdbcType, TypeHandler<?>>();
				for (Entry<JdbcType, TypeHandler<?>> entry : jdbcHandlerMap.entrySet()) {
					// Create a type handler instance with enum type as a constructor arg
					newMap.put(entry.getKey(), getInstance(enumClazz, entry.getValue().getClass()));
				}
				return newMap;
			}
		}
		return null;
	}

	private Map<JdbcType, TypeHandler<?>> getJdbcHandlerMapForSuperclass(Class<?> clazz) {
		Class<?> superclass = clazz.getSuperclass();
		if (superclass == null || Object.class.equals(superclass)) {
			return null;
		}
		Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(superclass);
		if (jdbcHandlerMap != null) {
			return jdbcHandlerMap;
		} else {
			return getJdbcHandlerMapForSuperclass(superclass);
		}
	}

	private TypeHandler<?> pickSoleHandler(Map<JdbcType, TypeHandler<?>> jdbcHandlerMap) {
		TypeHandler<?> soleHandler = null;
		for (TypeHandler<?> handler : jdbcHandlerMap.values()) {
			if (soleHandler == null) {
				soleHandler = handler;
			} else if (!handler.getClass().equals(soleHandler.getClass())) {
				// More than one type handlers registered.
				return null;
			}
		}
		return soleHandler;
	}

	public TypeHandler<Object> getUnknownTypeHandler() {
		return UNKNOWN_TYPE_HANDLER;
	}

	public void register(JdbcType jdbcType, TypeHandler<?> handler) {
		JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
	}

	//
	// REGISTER INSTANCE
	//

	// Only handler

	@SuppressWarnings("unchecked")
	public <T> void register(TypeHandler<T> typeHandler) {
		boolean mappedTypeFound = false;
		MappedTypes mappedTypes = typeHandler.getClass().getAnnotation(MappedTypes.class);
		if (mappedTypes != null) {
			for (Class<?> handledType : mappedTypes.value()) {
				register(handledType, typeHandler);
				mappedTypeFound = true;
			}
		}
		// @since 3.1.0 - try to auto-discover the mapped type
		if (!mappedTypeFound && typeHandler instanceof TypeReference) {
			try {
				TypeReference<T> typeReference = (TypeReference<T>) typeHandler;
				register(typeReference.getRawType(), typeHandler);
				mappedTypeFound = true;
			} catch (Throwable t) {
				// maybe users define the TypeReference with a different type and are not
				// assignable, so just ignore it
			}
		}
		if (!mappedTypeFound) {
			register((Class<T>) null, typeHandler);
		}
	}

	// java type + handler

	public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
		register((Type) javaType, typeHandler);
	}

	private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
		MappedJdbcTypes mappedJdbcTypes = typeHandler.getClass().getAnnotation(MappedJdbcTypes.class);
		if (mappedJdbcTypes != null) {
			for (JdbcType handledJdbcType : mappedJdbcTypes.value()) {
				register(javaType, handledJdbcType, typeHandler);
			}
			if (mappedJdbcTypes.includeNullJdbcType()) {
				register(javaType, null, typeHandler);
			}
		} else {
			register(javaType, null, typeHandler);
		}
	}

	public <T> void register(TypeReference<T> javaTypeReference, TypeHandler<? extends T> handler) {
		register(javaTypeReference.getRawType(), handler);
	}

	// java type + jdbc type + handler

	public <T> void register(Class<T> type, JdbcType jdbcType, TypeHandler<? extends T> handler) {
		register((Type) type, jdbcType, handler);
	}

	private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
		if (javaType != null) {
			Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.get(javaType);
			if (map == null || map == NULL_TYPE_HANDLER_MAP) {
				map = new HashMap<JdbcType, TypeHandler<?>>();
				TYPE_HANDLER_MAP.put(javaType, map);
			}
			map.put(jdbcType, handler);
		}
		ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
	}

	/*
	 * 只根据给定的类型转化器来进行注册操作处理
	 */
	public void register(Class<?> typeHandlerClass) {
		//用于标识是否找到对应的关联关系
		boolean mappedTypeFound = false;
		//首先获取类上设置的MappedTypes注解
		MappedTypes mappedTypes = typeHandlerClass.getAnnotation(MappedTypes.class);
		//检测是否配置了MappedTypes注解
		if (mappedTypes != null) {
			//循环遍历配置的转换类型
			for (Class<?> javaTypeClass : mappedTypes.value()) {
				//将对应的类型与对应的类型转换处理器进行注册关联处理
				register(javaTypeClass, typeHandlerClass);
				//设置找到对应的关联关系
				mappedTypeFound = true;
			}
		}
		//检测是否找到对应的关联关系
		if (!mappedTypeFound) {
			//在没有找到对应的匹配类型时设置一个
			register(getInstance(null, typeHandlerClass));
		}
	}

	//根据提供的类型和类型转化器对应的加载位置来进行注册处理
	public void register(String javaTypeClassName, String typeHandlerClassName) throws ClassNotFoundException {
		//首先加载对应的类型和类型转化器对应的类,然后进行注册操作处理
		register(Resources.classForName(javaTypeClassName), Resources.classForName(typeHandlerClassName));
	}

	public void register(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		register(javaTypeClass, getInstance(javaTypeClass, typeHandlerClass));
	}

	// java type + jdbc type + handler type

	public void register(Class<?> javaTypeClass, JdbcType jdbcType, Class<?> typeHandlerClass) {
		register(javaTypeClass, jdbcType, getInstance(javaTypeClass, typeHandlerClass));
	}

	// Construct a handler (used also from Builders)

	@SuppressWarnings("unchecked")
	public <T> TypeHandler<T> getInstance(Class<?> javaTypeClass, Class<?> typeHandlerClass) {
		if (javaTypeClass != null) {
			try {
				Constructor<?> c = typeHandlerClass.getConstructor(Class.class);
				return (TypeHandler<T>) c.newInstance(javaTypeClass);
			} catch (NoSuchMethodException ignored) {
				// ignored
			} catch (Exception e) {
				throw new TypeException("Failed invoking constructor for handler " + typeHandlerClass, e);
			}
		}
		try {
			Constructor<?> c = typeHandlerClass.getConstructor();
			return (TypeHandler<T>) c.newInstance();
		} catch (Exception e) {
			throw new TypeException("Unable to find a usable constructor for " + typeHandlerClass, e);
		}
	}

	//以包扫描的方式进行类型转换注册处理
	public void register(String packageName) {
		//创建对应的资源扫描处理对象
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		//设置在指定包下进行扫描的目的是继承自TypeHandler的类
		resolverUtil.find(new ResolverUtil.IsA(TypeHandler.class), packageName);
		//获取所有扫描到的指定资源类
		Set<Class<? extends Class<?>>> handlerSet = resolverUtil.getClasses();
		//循环遍历所有的资源处理类进行注册操作处理
		for (Class<?> type : handlerSet) {
			//过滤 内名内部类 接口 抽象类
			if (!type.isAnonymousClass() && !type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
				//符合条件的类进行注册处理
				register(type);
			}
		}
	}

	/**
	 * @since 3.2.2
	 */
	public Collection<TypeHandler<?>> getTypeHandlers() {
		return Collections.unmodifiableCollection(ALL_TYPE_HANDLERS_MAP.values());
	}

}
