package org.apache.ibatis.builder.annotation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Case;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Property;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.TypeDiscriminator;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.FetchType;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.parsing.PropertyParser;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.UnknownTypeHandler;

/**
 * 进行基于注解方式的mapper解析构建器处理类
 */
public class MapperAnnotationBuilder {

	//用于记录sql注解集合
	private final Set<Class<? extends Annotation>> sqlAnnotationTypes = new HashSet<Class<? extends Annotation>>();
	//用于记录sqlProvider注解集合
	private final Set<Class<? extends Annotation>> sqlProviderAnnotationTypes = new HashSet<Class<? extends Annotation>>();

	private final Configuration configuration;
	private final MapperBuilderAssistant assistant;
	private final Class<?> type;

	public MapperAnnotationBuilder(Configuration configuration, Class<?> type) {
		String resource = type.getName().replace('.', '/') + ".java (best guess)";
		this.assistant = new MapperBuilderAssistant(configuration, resource);
		this.configuration = configuration;
		this.type = type;

		//初始化sql注解集合
		sqlAnnotationTypes.add(Select.class);
		sqlAnnotationTypes.add(Insert.class);
		sqlAnnotationTypes.add(Update.class);
		sqlAnnotationTypes.add(Delete.class);

		//初始化sqlProvider注解集合
		sqlProviderAnnotationTypes.add(SelectProvider.class);
		sqlProviderAnnotationTypes.add(InsertProvider.class);
		sqlProviderAnnotationTypes.add(UpdateProvider.class);
		sqlProviderAnnotationTypes.add(DeleteProvider.class);
	}

	/*
	 * 触发解析对应的mapper
	 */
	public void parse() {
		String resource = type.toString();
		//首先检测对应的mapper资源是否已经完成对应的加载操作处理
		if (!configuration.isResourceLoaded(resource)) {
			//在解析对应的注解类时,首先完成尝试加载对应的xml资源配置
			loadXmlResource();
			//然后将本资源添加到解析集合中
			configuration.addLoadedResource(resource);
			//给对应的解析帮助处理对象设置对应的命名空间
			assistant.setCurrentNamespace(type.getName());
			//解析对应的缓存注解
			parseCache();
			//解析对应的缓存引用注解
			parseCacheRef();
			//获取当前接口中定义的所有方法
			Method[] methods = type.getMethods();
			//循环遍历当前接口中定义的所有方法,完成对对应mapper解析操作处理
			for (Method method : methods) {
				try {
					//过滤对应的桥接方法
					if (!method.isBridge()) {
						//进行对对应方法的解析操作处理
						parseStatement(method);
					}
				} catch (IncompleteElementException e) {
					//当在解析方法时遇到不能完成对应的解析处理,将对应的方法放置到未完成解析操作集合中
					configuration.addIncompleteMethod(new MethodResolver(this, method));
				}
			}
		}
		//触发完成一个对应的mapper,尝试进行对尚未解析完成的方法进行再次尝试解析操作处理----------------------->即当对应的mapper解析完成后,可能使得对应的尚未解析完成的方法能够进行解析完成处理
		parsePendingMethods();
	}

	private void parsePendingMethods() {
		//获取当前记录的尚未解析完成的所有方法
		Collection<MethodResolver> incompleteMethods = configuration.getIncompleteMethods();
		synchronized (incompleteMethods) {
			//获取对应的迭代器对象
			Iterator<MethodResolver> iter = incompleteMethods.iterator();
			//进行循环迭代处理
			while (iter.hasNext()) {
				try {
					//进一步进行尝试操作处理------------->尝试对尚未解析完成的方式进行解析操作处理
					iter.next().resolve();
					//顺利解析完成,将对应的方法移除集合
					iter.remove();
				} catch (IncompleteElementException e) {
					// This method is still missing a resource
					//方法尚未解析完成,不进行移除操作处理
				}
			}
		}
	}

	/*
	 * 此处用于触发解析对应的xml文件的处理
	 *   即在基于注解方式的mapper注入处理时可能会触发对应的基于xml文件的mapper解析操作(即两者可以进行联合使用)
	 */
	private void loadXmlResource() {
		// Spring may not know the real resource name so we check a flag to prevent loading again a resource twice
		// this flag is set at XMLMapperBuilder#bindMapperForNamespace
		//检测配置的xml对应的资源信息是否已经进行过解析操作处理
		if (!configuration.isResourceLoaded("namespace:" + type.getName())) {
			//获取对应的资源文件对应的路径
			String xmlResource = type.getName().replace('.', '/') + ".xml";
			InputStream inputStream = null;
			try {
				//尝试进行加载对应的xml资源mapper
				inputStream = Resources.getResourceAsStream(type.getClassLoader(), xmlResource);
			} catch (IOException e) {
				// ignore, resource is not required
			}
			//检测是否成功获取对应的资源-------->即基于注解方式的mapper注入不一定需要对应的xml配置信息(通过这个地方的解析操作处理,我们发现基于注解方式和基于xml配置方式可以进行结合起来联合使用)
			if (inputStream != null) {
				//创建对应的xml方式的解析构建器对象
				XMLMapperBuilder xmlParser = new XMLMapperBuilder(inputStream, assistant.getConfiguration(), xmlResource, configuration.getSqlFragments(), type.getName());
				//进行基于xml方式的解析操作处理
				xmlParser.parse();
			}
		}
	}

	/*
	 * 解析对应的缓存注解
	 */
	private void parseCache() {
		//获取类上配置的缓存注解
		CacheNamespace cacheDomain = type.getAnnotation(CacheNamespace.class);
		//检测是否配置了对应的缓存注解
		if (cacheDomain != null) {
			//获取缓存注解上配置的缓存大小
			Integer size = cacheDomain.size() == 0 ? null : cacheDomain.size();
			//获取缓存注解上配置的缓存时间间隔
			Long flushInterval = cacheDomain.flushInterval() == 0 ? null : cacheDomain.flushInterval();
			//获取缓存注解上配置的属性集合数据
			Properties props = convertToProperties(cacheDomain.properties());
			//设置本mapper使用了对应的缓存策略
			assistant.useNewCache(cacheDomain.implementation(), cacheDomain.eviction(), flushInterval, size, cacheDomain.readWrite(), cacheDomain.blocking(), props);
		}
	}

	/*
	 * 获取CacheNamespace缓存注解上配置对应的配置属性
	 */
	private Properties convertToProperties(Property[] properties) {
		//检测是否配置了对应的配置属性
		if (properties.length == 0) {
			return null;
		}
		//创建对应的属性对象
		Properties props = new Properties();
		//循环处理并解析对应的带参数类型的属性值
		for (Property property : properties) {
			//添加并解析带参数类型的属性值
			props.setProperty(property.name(), PropertyParser.parse(property.value(), configuration.getVariables()));
		}
		//返回解析后的配置属性
		return props;
	}

	/*
	 * 解析对应的缓存引用注解
	 */
	private void parseCacheRef() {
		//获取类上配置的缓存引用注解
		CacheNamespaceRef cacheDomainRef = type.getAnnotation(CacheNamespaceRef.class);
		//检测是否配置了对应的缓存引用注解
		if (cacheDomainRef != null) {
			//获取引用缓存类的Class
			Class<?> refType = cacheDomainRef.value();
			//获取引用缓存类的全限定名称
			String refName = cacheDomainRef.name();
			//处理两种方式都为空的异常配置问题
			if (refType == void.class && refName.isEmpty()) {
				throw new BuilderException("Should be specified either value() or name() attribute in the @CacheNamespaceRef");
			}
			//处理两种方式都使用的异常配置问题
			if (refType != void.class && !refName.isEmpty()) {
				throw new BuilderException("Cannot use both value() and name() attribute in the @CacheNamespaceRef");
			}
			//获取使用引用缓存对应的全限定名称
			String namespace = (refType != void.class) ? refType.getName() : refName;
			//将对应的缓存设置给解析辅助处理器对象
			assistant.useCacheRef(namespace);
		}
	}

	/*
	 * 真正进行mapper中方法的解析操作处理
	 */
	void parseStatement(Method method) {
		//
		Class<?> parameterTypeClass = getParameterType(method);
		//
		LanguageDriver languageDriver = getLanguageDriver(method);
		//
		SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
		if (sqlSource != null) {
			Options options = method.getAnnotation(Options.class);
			final String mappedStatementId = type.getName() + "." + method.getName();
			Integer fetchSize = null;
			Integer timeout = null;
			StatementType statementType = StatementType.PREPARED;
			ResultSetType resultSetType = ResultSetType.FORWARD_ONLY;
			SqlCommandType sqlCommandType = getSqlCommandType(method);
			boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
			boolean flushCache = !isSelect;
			boolean useCache = isSelect;

			KeyGenerator keyGenerator;
			String keyProperty = "id";
			String keyColumn = null;
			if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
				//first check for SelectKey annotation - that overrides everything else
				SelectKey selectKey = method.getAnnotation(SelectKey.class);
				if (selectKey != null) {
					keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
					keyProperty = selectKey.keyProperty();
				} else if (options == null) {
					keyGenerator = configuration.isUseGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
				} else {
					keyGenerator = options.useGeneratedKeys() ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
					keyProperty = options.keyProperty();
					keyColumn = options.keyColumn();
				}
			} else {
				keyGenerator = NoKeyGenerator.INSTANCE;
			}

			if (options != null) {
				if (FlushCachePolicy.TRUE.equals(options.flushCache())) {
					flushCache = true;
				} else if (FlushCachePolicy.FALSE.equals(options.flushCache())) {
					flushCache = false;
				}
				useCache = options.useCache();
				fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; // issue #348
				timeout = options.timeout() > -1 ? options.timeout() : null;
				statementType = options.statementType();
				resultSetType = options.resultSetType();
			}

			String resultMapId = null;
			ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
			if (resultMapAnnotation != null) {
				String[] resultMaps = resultMapAnnotation.value();
				StringBuilder sb = new StringBuilder();
				for (String resultMap : resultMaps) {
					if (sb.length() > 0) {
						sb.append(",");
					}
					sb.append(resultMap);
				}
				resultMapId = sb.toString();
			} else if (isSelect) {
				resultMapId = parseResultMap(method);
			}

			assistant.addMappedStatement(mappedStatementId, sqlSource, statementType, sqlCommandType, fetchSize,
					timeout,
					// ParameterMapID
					null, parameterTypeClass, resultMapId, getReturnType(method), resultSetType, flushCache, useCache,
					// TODO gcode issue #577
					false, keyGenerator, keyProperty, keyColumn,
					// DatabaseID
					null, languageDriver,
					// ResultSets
					options != null ? nullOrEmpty(options.resultSets()) : null);
		}
	}
	
	private String parseResultMap(Method method) {
		Class<?> returnType = getReturnType(method);
		ConstructorArgs args = method.getAnnotation(ConstructorArgs.class);
		Results results = method.getAnnotation(Results.class);
		TypeDiscriminator typeDiscriminator = method.getAnnotation(TypeDiscriminator.class);
		String resultMapId = generateResultMapName(method);
		applyResultMap(resultMapId, returnType, argsIf(args), resultsIf(results), typeDiscriminator);
		return resultMapId;
	}

	private String generateResultMapName(Method method) {
		Results results = method.getAnnotation(Results.class);
		if (results != null && !results.id().isEmpty()) {
			return type.getName() + "." + results.id();
		}
		StringBuilder suffix = new StringBuilder();
		for (Class<?> c : method.getParameterTypes()) {
			suffix.append("-");
			suffix.append(c.getSimpleName());
		}
		if (suffix.length() < 1) {
			suffix.append("-void");
		}
		return type.getName() + "." + method.getName() + suffix;
	}

	private void applyResultMap(String resultMapId, Class<?> returnType, Arg[] args, Result[] results, TypeDiscriminator discriminator) {
		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
		applyConstructorArgs(args, returnType, resultMappings);
		applyResults(results, returnType, resultMappings);
		Discriminator disc = applyDiscriminator(resultMapId, returnType, discriminator);
		// TODO add AutoMappingBehaviour
		assistant.addResultMap(resultMapId, returnType, null, disc, resultMappings, null);
		createDiscriminatorResultMaps(resultMapId, returnType, discriminator);
	}

	private void createDiscriminatorResultMaps(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
		if (discriminator != null) {
			for (Case c : discriminator.cases()) {
				String caseResultMapId = resultMapId + "-" + c.value();
				List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
				// issue #136
				applyConstructorArgs(c.constructArgs(), resultType, resultMappings);
				applyResults(c.results(), resultType, resultMappings);
				// TODO add AutoMappingBehaviour
				assistant.addResultMap(caseResultMapId, c.type(), resultMapId, null, resultMappings, null);
			}
		}
	}

	private Discriminator applyDiscriminator(String resultMapId, Class<?> resultType, TypeDiscriminator discriminator) {
		if (discriminator != null) {
			String column = discriminator.column();
			Class<?> javaType = discriminator.javaType() == void.class ? String.class : discriminator.javaType();
			JdbcType jdbcType = discriminator.jdbcType() == JdbcType.UNDEFINED ? null : discriminator.jdbcType();
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (discriminator.typeHandler() == UnknownTypeHandler.class ? null : discriminator.typeHandler());
			Case[] cases = discriminator.cases();
			Map<String, String> discriminatorMap = new HashMap<String, String>();
			for (Case c : cases) {
				String value = c.value();
				String caseResultMapId = resultMapId + "-" + value;
				discriminatorMap.put(value, caseResultMapId);
			}
			return assistant.buildDiscriminator(resultType, column, javaType, jdbcType, typeHandler, discriminatorMap);
		}
		return null;
	}

	private LanguageDriver getLanguageDriver(Method method) {
		Lang lang = method.getAnnotation(Lang.class);
		Class<?> langClass = null;
		if (lang != null) {
			langClass = lang.value();
		}
		return assistant.getLanguageDriver(langClass);
	}

	/*
	 * 获取对应的参数类型
	 * 如果出现多参数类型,最终转换成成对应的ParamMap类型,否则就是对应的参数类型
	 */
	private Class<?> getParameterType(Method method) {
		//定义记录对应的参数类型的变量
		Class<?> parameterType = null;
		//获取方法的参数数组
		Class<?>[] parameterTypes = method.getParameterTypes();
		//循环遍历所有的参数类型
		for (Class<?> currentParameterType : parameterTypes) {
			//过滤参数类型继承子RowBounds或者ResultHandler类型----------------->即
			if (!RowBounds.class.isAssignableFrom(currentParameterType) && !ResultHandler.class.isAssignableFrom(currentParameterType)) {
				if (parameterType == null) {
					parameterType = currentParameterType;
				} else {
					// issue #135
					parameterType = ParamMap.class;
				}
			}
		}
		return parameterType;
	}

	private Class<?> getReturnType(Method method) {
		Class<?> returnType = method.getReturnType();
		Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, type);
		if (resolvedReturnType instanceof Class) {
			returnType = (Class<?>) resolvedReturnType;
			if (returnType.isArray()) {
				returnType = returnType.getComponentType();
			}
			// gcode issue #508
			if (void.class.equals(returnType)) {
				ResultType rt = method.getAnnotation(ResultType.class);
				if (rt != null) {
					returnType = rt.value();
				}
			}
		} else if (resolvedReturnType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) resolvedReturnType;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();
			if (Collection.class.isAssignableFrom(rawType) || Cursor.class.isAssignableFrom(rawType)) {
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				if (actualTypeArguments != null && actualTypeArguments.length == 1) {
					Type returnTypeParameter = actualTypeArguments[0];
					if (returnTypeParameter instanceof Class<?>) {
						returnType = (Class<?>) returnTypeParameter;
					} else if (returnTypeParameter instanceof ParameterizedType) {
						// (gcode issue #443) actual type can be a also a parameterized type
						returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
					} else if (returnTypeParameter instanceof GenericArrayType) {
						Class<?> componentType = (Class<?>) ((GenericArrayType) returnTypeParameter).getGenericComponentType();
						// (gcode issue #525) support List<byte[]>
						returnType = Array.newInstance(componentType, 0).getClass();
					}
				}
			} else if (method.isAnnotationPresent(MapKey.class) && Map.class.isAssignableFrom(rawType)) {
				// (gcode issue 504) Do not look into Maps if there is not MapKey annotation
				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				if (actualTypeArguments != null && actualTypeArguments.length == 2) {
					Type returnTypeParameter = actualTypeArguments[1];
					if (returnTypeParameter instanceof Class<?>) {
						returnType = (Class<?>) returnTypeParameter;
					} else if (returnTypeParameter instanceof ParameterizedType) {
						// (gcode issue 443) actual type can be a also a parameterized type
						returnType = (Class<?>) ((ParameterizedType) returnTypeParameter).getRawType();
					}
				}
			}
		}
		return returnType;
	}

	/*
	 * 
	 */
	private SqlSource getSqlSourceFromAnnotations(Method method, Class<?> parameterType, LanguageDriver languageDriver) {
		try {
			//在方法上检测对应的sql注解
			Class<? extends Annotation> sqlAnnotationType = getSqlAnnotationType(method);
			Class<? extends Annotation> sqlProviderAnnotationType = getSqlProviderAnnotationType(method);
			//优先检测是否有sql注解
			if (sqlAnnotationType != null) {
				//检测是否配置两种不同类型注解的异常配置错误信息
				if (sqlProviderAnnotationType != null) {
					throw new BindingException("You cannot supply both a static SQL and SqlProvider to method named " + method.getName());
				}
				//进一步在方法上获取对应类型的注解
				Annotation sqlAnnotation = method.getAnnotation(sqlAnnotationType);
				//获取在注解上配置的sql语句内容
				final String[] strings = (String[]) sqlAnnotation.getClass().getMethod("value").invoke(sqlAnnotation);
				//
				return buildSqlSourceFromStrings(strings, parameterType, languageDriver);
			} else if (sqlProviderAnnotationType != null) {
				//进一步在方法上获取对应类型的注解
				Annotation sqlProviderAnnotation = method.getAnnotation(sqlProviderAnnotationType);
				//
				return new ProviderSqlSource(assistant.getConfiguration(), sqlProviderAnnotation, type, method);
			}
			//如果没有在方法上发现对象的注解,返回空对象
			return null;
		} catch (Exception e) {
			throw new BuilderException("Could not find value method on SQL annotation.  Cause: " + e, e);
		}
	}

	private SqlSource buildSqlSourceFromStrings(String[] strings, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
		final StringBuilder sql = new StringBuilder();
		//对给定的sql语句内容进行拼接操作处理
		for (String fragment : strings) {
			sql.append(fragment);
			sql.append(" ");
		}
		//
		return languageDriver.createSqlSource(configuration, sql.toString().trim(), parameterTypeClass);
	}

	/*
	 * 根据提供的方法获取注解对应的sql命令类型
	 */
	private SqlCommandType getSqlCommandType(Method method) {
		//根据方法获取对应的sql注解
		Class<? extends Annotation> type = getSqlAnnotationType(method);
		//检测是否配置了对应的注解
		if (type == null) {
			//根据方法获取对应的sqlProvider注解
			type = getSqlProviderAnnotationType(method);
			if (type == null) {
				return SqlCommandType.UNKNOWN;
			}
			//转换成对应的sql类型
			if (type == SelectProvider.class) {
				type = Select.class;
			} else if (type == InsertProvider.class) {
				type = Insert.class;
			} else if (type == UpdateProvider.class) {
				type = Update.class;
			} else if (type == DeleteProvider.class) {
				type = Delete.class;
			}
		}
		//根据获取的类型获取对应的sql命令对应的命令类型
		return SqlCommandType.valueOf(type.getSimpleName().toUpperCase(Locale.ENGLISH));
	}

	/*
	 * 获取方法上是否配置了一下几种对应的注解
	 * Select
	 * Insert
	 * Update
	 * Delete
	 */
	private Class<? extends Annotation> getSqlAnnotationType(Method method) {
		return chooseAnnotationType(method, sqlAnnotationTypes);
	}

	/*
	 * 获取方法上是否配置了一下几种对应的注解
	 * SelectProvider 
	 * InsertProvider
	 * UpdateProvider
	 * DeleteProvider
	 */
	private Class<? extends Annotation> getSqlProviderAnnotationType(Method method) {
		return chooseAnnotationType(method, sqlProviderAnnotationTypes);
	}

	/*
	 * 获取方法上设置的满足条件的注解
	 */
	private Class<? extends Annotation> chooseAnnotationType(Method method, Set<Class<? extends Annotation>> types) {
		//循环遍历所有给定的集合注解,检测对应的方法是否配置了对应的注解
		for (Class<? extends Annotation> type : types) {
			//获取指定类型的注解
			Annotation annotation = method.getAnnotation(type);
			//检测方式上是否有对应的注解
			if (annotation != null) {
				//返回对应注解的类型
				return type;
			}
		}
		//没有对应的注解返回空对象
		return null;
	}

	private void applyResults(Result[] results, Class<?> resultType, List<ResultMapping> resultMappings) {
		for (Result result : results) {
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			if (result.id()) {
				flags.add(ResultFlag.ID);
			}
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) ((result.typeHandler() == UnknownTypeHandler.class) ? null : result.typeHandler());
			ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(result.property()),
					nullOrEmpty(result.column()), result.javaType() == void.class ? null : result.javaType(),
					result.jdbcType() == JdbcType.UNDEFINED ? null : result.jdbcType(),
					hasNestedSelect(result) ? nestedSelectId(result) : null, null, null, null, typeHandler, flags, null,
					null, isLazy(result));
			resultMappings.add(resultMapping);
		}
	}

	private String nestedSelectId(Result result) {
		String nestedSelect = result.one().select();
		if (nestedSelect.length() < 1) {
			nestedSelect = result.many().select();
		}
		if (!nestedSelect.contains(".")) {
			nestedSelect = type.getName() + "." + nestedSelect;
		}
		return nestedSelect;
	}

	private boolean isLazy(Result result) {
		boolean isLazy = configuration.isLazyLoadingEnabled();
		if (result.one().select().length() > 0 && FetchType.DEFAULT != result.one().fetchType()) {
			isLazy = result.one().fetchType() == FetchType.LAZY;
		} else if (result.many().select().length() > 0 && FetchType.DEFAULT != result.many().fetchType()) {
			isLazy = result.many().fetchType() == FetchType.LAZY;
		}
		return isLazy;
	}

	private boolean hasNestedSelect(Result result) {
		if (result.one().select().length() > 0 && result.many().select().length() > 0) {
			throw new BuilderException("Cannot use both @One and @Many annotations in the same @Result");
		}
		return result.one().select().length() > 0 || result.many().select().length() > 0;
	}

	private void applyConstructorArgs(Arg[] args, Class<?> resultType, List<ResultMapping> resultMappings) {
		for (Arg arg : args) {
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			flags.add(ResultFlag.CONSTRUCTOR);
			if (arg.id()) {
				flags.add(ResultFlag.ID);
			}
			@SuppressWarnings("unchecked")
			Class<? extends TypeHandler<?>> typeHandler = (Class<? extends TypeHandler<?>>) (arg.typeHandler() == UnknownTypeHandler.class ? null : arg.typeHandler());
			ResultMapping resultMapping = assistant.buildResultMapping(resultType, nullOrEmpty(arg.name()),
					nullOrEmpty(arg.column()), arg.javaType() == void.class ? null : arg.javaType(),
					arg.jdbcType() == JdbcType.UNDEFINED ? null : arg.jdbcType(), nullOrEmpty(arg.select()),
					nullOrEmpty(arg.resultMap()), null, null, typeHandler, flags, null, null, false);
			resultMappings.add(resultMapping);
		}
	}

	private String nullOrEmpty(String value) {
		return value == null || value.trim().length() == 0 ? null : value;
	}

	private Result[] resultsIf(Results results) {
		return results == null ? new Result[0] : results.value();
	}

	private Arg[] argsIf(ConstructorArgs args) {
		return args == null ? new Arg[0] : args.value();
	}

	private KeyGenerator handleSelectKeyAnnotation(SelectKey selectKeyAnnotation, String baseStatementId, Class<?> parameterTypeClass, LanguageDriver languageDriver) {
		String id = baseStatementId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
		Class<?> resultTypeClass = selectKeyAnnotation.resultType();
		StatementType statementType = selectKeyAnnotation.statementType();
		String keyProperty = selectKeyAnnotation.keyProperty();
		String keyColumn = selectKeyAnnotation.keyColumn();
		boolean executeBefore = selectKeyAnnotation.before();

		// defaults
		boolean useCache = false;
		KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
		Integer fetchSize = null;
		Integer timeout = null;
		boolean flushCache = false;
		String parameterMap = null;
		String resultMap = null;
		ResultSetType resultSetTypeEnum = null;

		SqlSource sqlSource = buildSqlSourceFromStrings(selectKeyAnnotation.statement(), parameterTypeClass, languageDriver);
		SqlCommandType sqlCommandType = SqlCommandType.SELECT;

		assistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
				parameterTypeClass, resultMap, resultTypeClass, resultSetTypeEnum, flushCache, useCache, false,
				keyGenerator, keyProperty, keyColumn, null, languageDriver, null);

		id = assistant.applyCurrentNamespace(id, false);

		MappedStatement keyStatement = configuration.getMappedStatement(id, false);
		SelectKeyGenerator answer = new SelectKeyGenerator(keyStatement, executeBefore);
		configuration.addKeyGenerator(id, answer);
		return answer;
	}

}
