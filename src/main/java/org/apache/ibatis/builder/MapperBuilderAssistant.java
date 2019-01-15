package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.CacheBuilder;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * 进行解析注解或者xml的解析辅助处理器对象
 */
public class MapperBuilderAssistant extends BaseBuilder {

	//当前辅助对象mapper对应的命名空间值
	private String currentNamespace;
	//
	private final String resource;
	//用于记录本mapper对应的使用的缓存处理类
	private Cache currentCache;
	//未解析对应引用缓存标识  当为假时说明对应的引用缓存对象已经在缓存集合中了
	private boolean unresolvedCacheRef;

	public MapperBuilderAssistant(Configuration configuration, String resource) {
		super(configuration);
		ErrorContext.instance().resource(resource);
		this.resource = resource;
	}

	public String getCurrentNamespace() {
		return currentNamespace;
	}

	/*
	 * 设置辅助解析当前mapper对应命名空间的处理操作
	 */
	public void setCurrentNamespace(String currentNamespace) {
		//检测给定的命名空间是否为配置空异常问题-------------->即每个mapper都必须有唯一的命名空间值
		if (currentNamespace == null) {
			throw new BuilderException("The mapper element requires a namespace attribute to be specified.");
		}
		
		//检测设置的命名空间值与当前记录的命名空间值是否匹配---------->不一致抛出对应的异常
		if (this.currentNamespace != null && !this.currentNamespace.equals(currentNamespace)) {
			throw new BuilderException("Wrong namespace. Expected '" + this.currentNamespace + "' but found '" + currentNamespace + "'.");
		}
		//设置辅助对象对应mapper所在的命名空间值
		this.currentNamespace = currentNamespace;
	}

	/*
	 * 对相对名的转换成对应的全限定名-----即装配唯一标识
	 */
	public String applyCurrentNamespace(String base, boolean isReference) {
		//检测带拼接字符串是否为空
		if (base == null) {
			return null;
		}
		//检测是否使用了相对名称标识
		if (isReference) {
			//通过检测对应的.符号来确定带拼接的字符串是否是全限定名
			if (base.contains(".")) {
				//直接返回对应的字符串数据
				return base;
			}
		} else {
			//检测给定的字符串是否以当前的全限定名开头
			if (base.startsWith(currentNamespace + ".")) {
				return base;
			}
			//检测设置的相对字符串格式是否有问题----------------->即相对名称不能含有.符号
			if (base.contains(".")) {
				throw new BuilderException("Dots are not allowed in element names, please remove it from " + base);
			}
		}
		//拼接唯一的限定名
		return currentNamespace + "." + base;
	}

	/*
	 * 通过引用缓存的方式来进行设置缓存本mapper对应的缓存处理类
	 */
	public Cache useCacheRef(String namespace) {
		//检测缓存对应的全限定名不能为空的检测
		if (namespace == null) {
			throw new BuilderException("cache-ref element requires a namespace attribute.");
		}
		try {
			//设置未解析对应引用缓存标识为真
			unresolvedCacheRef = true;
			//检测对应的缓存处理类是否已经在缓存集合中
			Cache cache = configuration.getCache(namespace);
			if (cache == null) {
				//抛出对应的尚未完成的异常
				throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.");
			}
			//设置当前mapper对应的使用的缓存对象
			currentCache = cache;
			//设置未解析对应引用缓存标识为假----------->即对应的引用缓存已经在对应的缓存集合中了
			unresolvedCacheRef = false;
			//返回对应的缓存对象
			return cache;
		} catch (IllegalArgumentException e) {
			throw new IncompleteElementException("No cache for namespace '" + namespace + "' could be found.", e);
		}
	}

	public Cache useNewCache(Class<? extends Cache> typeClass, Class<? extends Cache> evictionClass, Long flushInterval, Integer size, boolean readWrite, boolean blocking, Properties props) {
		Cache cache = new CacheBuilder(currentNamespace).implementation(valueOrDefault(typeClass, PerpetualCache.class)).addDecorator(valueOrDefault(evictionClass, LruCache.class)).clearInterval(flushInterval).size(size).readWrite(readWrite).blocking(blocking).properties(props).build();
		configuration.addCache(cache);
		currentCache = cache;
		return cache;
	}

	public ParameterMap addParameterMap(String id, Class<?> parameterClass, List<ParameterMapping> parameterMappings) {
		id = applyCurrentNamespace(id, false);
		ParameterMap parameterMap = new ParameterMap.Builder(configuration, id, parameterClass, parameterMappings).build();
		configuration.addParameterMap(parameterMap);
		return parameterMap;
	}

	public ParameterMapping buildParameterMapping(Class<?> parameterType, String property, Class<?> javaType, JdbcType jdbcType, String resultMap, ParameterMode parameterMode, Class<? extends TypeHandler<?>> typeHandler, Integer numericScale) {
		resultMap = applyCurrentNamespace(resultMap, true);

		// Class parameterType = parameterMapBuilder.type();
		Class<?> javaTypeClass = resolveParameterJavaType(parameterType, property, javaType, jdbcType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);

		return new ParameterMapping.Builder(configuration, property, javaTypeClass).jdbcType(jdbcType).resultMapId(resultMap).mode(parameterMode).numericScale(numericScale).typeHandler(typeHandlerInstance).build();
	}

	public ResultMap addResultMap(String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
		id = applyCurrentNamespace(id, false);
		extend = applyCurrentNamespace(extend, true);

		if (extend != null) {
			if (!configuration.hasResultMap(extend)) {
				throw new IncompleteElementException("Could not find a parent resultmap with id '" + extend + "'");
			}
			ResultMap resultMap = configuration.getResultMap(extend);
			List<ResultMapping> extendedResultMappings = new ArrayList<ResultMapping>(resultMap.getResultMappings());
			extendedResultMappings.removeAll(resultMappings);
			// Remove parent constructor if this resultMap declares a constructor.
			boolean declaresConstructor = false;
			for (ResultMapping resultMapping : resultMappings) {
				if (resultMapping.getFlags().contains(ResultFlag.CONSTRUCTOR)) {
					declaresConstructor = true;
					break;
				}
			}
			if (declaresConstructor) {
				Iterator<ResultMapping> extendedResultMappingsIter = extendedResultMappings.iterator();
				while (extendedResultMappingsIter.hasNext()) {
					if (extendedResultMappingsIter.next().getFlags().contains(ResultFlag.CONSTRUCTOR)) {
						extendedResultMappingsIter.remove();
					}
				}
			}
			resultMappings.addAll(extendedResultMappings);
		}
		ResultMap resultMap = new ResultMap.Builder(configuration, id, type, resultMappings, autoMapping).discriminator(discriminator).build();
		configuration.addResultMap(resultMap);
		return resultMap;
	}

	public Discriminator buildDiscriminator(Class<?> resultType, String column, Class<?> javaType, JdbcType jdbcType, Class<? extends TypeHandler<?>> typeHandler, Map<String, String> discriminatorMap) {
		ResultMapping resultMapping = buildResultMapping(resultType, null, column, javaType, jdbcType, null, null, null, null, typeHandler, new ArrayList<ResultFlag>(), null, null, false);
		Map<String, String> namespaceDiscriminatorMap = new HashMap<String, String>();
		for (Map.Entry<String, String> e : discriminatorMap.entrySet()) {
			String resultMap = e.getValue();
			resultMap = applyCurrentNamespace(resultMap, true);
			namespaceDiscriminatorMap.put(e.getKey(), resultMap);
		}
		return new Discriminator.Builder(configuration, resultMapping, namespaceDiscriminatorMap).build();
	}

	public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
			SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap,
			Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType,
			boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty,
			String keyColumn, String databaseId, LanguageDriver lang, String resultSets) {

		if (unresolvedCacheRef) {
			throw new IncompleteElementException("Cache-ref not yet resolved");
		}

		id = applyCurrentNamespace(id, false);
		boolean isSelect = sqlCommandType == SqlCommandType.SELECT;

		MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource,
				sqlCommandType).resource(resource).fetchSize(fetchSize).timeout(timeout).statementType(statementType)
						.keyGenerator(keyGenerator).keyProperty(keyProperty).keyColumn(keyColumn).databaseId(databaseId)
						.lang(lang).resultOrdered(resultOrdered).resultSets(resultSets)
						.resultMaps(getStatementResultMaps(resultMap, resultType, id)).resultSetType(resultSetType)
						.flushCacheRequired(valueOrDefault(flushCache, !isSelect))
						.useCache(valueOrDefault(useCache, isSelect)).cache(currentCache);

		ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
		if (statementParameterMap != null) {
			statementBuilder.parameterMap(statementParameterMap);
		}

		MappedStatement statement = statementBuilder.build();
		configuration.addMappedStatement(statement);
		return statement;
	}

	private <T> T valueOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}

	private ParameterMap getStatementParameterMap(String parameterMapName, Class<?> parameterTypeClass, String statementId) {
		parameterMapName = applyCurrentNamespace(parameterMapName, true);
		ParameterMap parameterMap = null;
		if (parameterMapName != null) {
			try {
				parameterMap = configuration.getParameterMap(parameterMapName);
			} catch (IllegalArgumentException e) {
				throw new IncompleteElementException("Could not find parameter map " + parameterMapName, e);
			}
		} else if (parameterTypeClass != null) {
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			parameterMap = new ParameterMap.Builder(configuration, statementId + "-Inline", parameterTypeClass, parameterMappings).build();
		}
		return parameterMap;
	}

	private List<ResultMap> getStatementResultMaps(String resultMap, Class<?> resultType, String statementId) {
		resultMap = applyCurrentNamespace(resultMap, true);
		List<ResultMap> resultMaps = new ArrayList<ResultMap>();
		if (resultMap != null) {
			String[] resultMapNames = resultMap.split(",");
			for (String resultMapName : resultMapNames) {
				try {
					resultMaps.add(configuration.getResultMap(resultMapName.trim()));
				} catch (IllegalArgumentException e) {
					throw new IncompleteElementException("Could not find result map " + resultMapName, e);
				}
			}
		} else if (resultType != null) {
			ResultMap inlineResultMap = new ResultMap.Builder(configuration, statementId + "-Inline", resultType, new ArrayList<ResultMapping>(), null).build();
			resultMaps.add(inlineResultMap);
		}
		return resultMaps;
	}

	public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
			JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
			Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags, String resultSet, String foreignColumn, boolean lazy) {
		Class<?> javaTypeClass = resolveResultJavaType(resultType, property, javaType);
		TypeHandler<?> typeHandlerInstance = resolveTypeHandler(javaTypeClass, typeHandler);
		List<ResultMapping> composites = parseCompositeColumnName(column);
		return new ResultMapping.Builder(configuration, property, column, javaTypeClass).jdbcType(jdbcType)
				.nestedQueryId(applyCurrentNamespace(nestedSelect, true))
				.nestedResultMapId(applyCurrentNamespace(nestedResultMap, true)).resultSet(resultSet)
				.typeHandler(typeHandlerInstance).flags(flags == null ? new ArrayList<ResultFlag>() : flags)
				.composites(composites).notNullColumns(parseMultipleColumnNames(notNullColumn))
				.columnPrefix(columnPrefix).foreignColumn(foreignColumn).lazy(lazy).build();
	}

	private Set<String> parseMultipleColumnNames(String columnName) {
		Set<String> columns = new HashSet<String>();
		if (columnName != null) {
			if (columnName.indexOf(',') > -1) {
				StringTokenizer parser = new StringTokenizer(columnName, "{}, ", false);
				while (parser.hasMoreTokens()) {
					String column = parser.nextToken();
					columns.add(column);
				}
			} else {
				columns.add(columnName);
			}
		}
		return columns;
	}

	private List<ResultMapping> parseCompositeColumnName(String columnName) {
		List<ResultMapping> composites = new ArrayList<ResultMapping>();
		if (columnName != null && (columnName.indexOf('=') > -1 || columnName.indexOf(',') > -1)) {
			StringTokenizer parser = new StringTokenizer(columnName, "{}=, ", false);
			while (parser.hasMoreTokens()) {
				String property = parser.nextToken();
				String column = parser.nextToken();
				ResultMapping complexResultMapping = new ResultMapping.Builder(configuration, property, column, configuration.getTypeHandlerRegistry().getUnknownTypeHandler()).build();
				composites.add(complexResultMapping);
			}
		}
		return composites;
	}

	private Class<?> resolveResultJavaType(Class<?> resultType, String property, Class<?> javaType) {
		if (javaType == null && property != null) {
			try {
				MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
				javaType = metaResultType.getSetterType(property);
			} catch (Exception e) {
				// ignore, following null check statement will deal with the situation
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}

	private Class<?> resolveParameterJavaType(Class<?> resultType, String property, Class<?> javaType, JdbcType jdbcType) {
		if (javaType == null) {
			if (JdbcType.CURSOR.equals(jdbcType)) {
				javaType = java.sql.ResultSet.class;
			} else if (Map.class.isAssignableFrom(resultType)) {
				javaType = Object.class;
			} else {
				MetaClass metaResultType = MetaClass.forClass(resultType, configuration.getReflectorFactory());
				javaType = metaResultType.getGetterType(property);
			}
		}
		if (javaType == null) {
			javaType = Object.class;
		}
		return javaType;
	}

	/** Backward compatibility signature */
	public ResultMapping buildResultMapping(Class<?> resultType, String property, String column, Class<?> javaType,
			JdbcType jdbcType, String nestedSelect, String nestedResultMap, String notNullColumn, String columnPrefix,
			Class<? extends TypeHandler<?>> typeHandler, List<ResultFlag> flags) {
		return buildResultMapping(resultType, property, column, javaType, jdbcType, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandler, flags, null, null, configuration.isLazyLoadingEnabled());
	}

	/*
	 * 根据驱动类型获取对应的语言操作驱动对象
	 */
	public LanguageDriver getLanguageDriver(Class<?> langClass) {
		//根据传入的类型来获取对应的驱动对象类型
		if (langClass != null) {
			//将对应的类型注册到语言操作驱动对象集合中
			configuration.getLanguageRegistry().register(langClass);
		} else {
			//获取系统默认配置的语言操作驱动对象
			langClass = configuration.getLanguageRegistry().getDefaultDriverClass();
		}
		//根据驱动类型获取对应的语言操作驱动对象
		return configuration.getLanguageRegistry().getDriver(langClass);
	}

	/** Backward compatibility signature */
	public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType,
			SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap,
			Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType,
			boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty,
			String keyColumn, String databaseId, LanguageDriver lang) {
		return addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap,
				parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator,
				keyProperty, keyColumn, databaseId, lang, null);
	}

}
