package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * 基于xml配置方式mapper解析构建器处理类
 */
public class XMLMapperBuilder extends BaseBuilder {

	//记录对应的xml节点搜索对象
	private final XPathParser parser;
	//用于记录对应的解析辅助器对象-------------->注意此处的辅助对象类型和基于注解的辅助类型是一致的
	private final MapperBuilderAssistant builderAssistant;
	//用于记录当前存储的sql片段
	private final Map<String, XNode> sqlFragments;
	//
	private final String resource;

	/*
	 * 提供方法重载的方式来设置对应的解析构建器对象
	 */
	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
		this(reader, configuration, resource, sqlFragments);
		this.builderAssistant.setCurrentNamespace(namespace);
	}

	@Deprecated
	public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration, resource, sqlFragments);
	}

	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
		this(inputStream, configuration, resource, sqlFragments);
		//此处完成对解析辅助对象设置命名空间参数的处理
		this.builderAssistant.setCurrentNamespace(namespace);
	}

	public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()), configuration, resource, sqlFragments);
	}

	private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
		super(configuration);
		//创建对应的解析辅助对象
		this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
		this.parser = parser;
		this.sqlFragments = sqlFragments;
		this.resource = resource;
	}

	/*
	 * 触发解析对应的xml的处理方法
	 */
	public void parse() {
		//首先检测对应的资源是否已经进行过加载解析处理
		if (!configuration.isResourceLoaded(resource)) {
			//触发解析对应的mapper节点配置的信息
			configurationElement(parser.evalNode("/mapper"));
			//解析完成,将对应的资源添加到资源加载解析完成集合中
			configuration.addLoadedResource(resource);
			//
			bindMapperForNamespace();
		}
		//
		parsePendingResultMaps();
		//
		parsePendingCacheRefs();
		//
		parsePendingStatements();
	}

	public XNode getSqlFragment(String refid) {
		return sqlFragments.get(refid);
	}

	/*
	 * 解析对应的mapper节点配置的信息
	 */
	private void configurationElement(XNode context) {
		try {
			//获取当前mapper对应的命名空间值
			String namespace = context.getStringAttribute("namespace");
			//检测当前mapper对应的命名空间值是否为空异常--------------->每个对应的mapper的xml都需要配置对应的命名空间值
			if (namespace == null || namespace.equals("")) {
				throw new BuilderException("Mapper's namespace cannot be empty");
			}
			//给辅助对象设置当前处理mapper对应的命名空间值
			builderAssistant.setCurrentNamespace(namespace);
			//解析缓存引用节点配置信息
			cacheRefElement(context.evalNode("cache-ref"));
			//解析缓存节点配置信息
			cacheElement(context.evalNode("cache"));
			//解析设置的所有parameterMap节点配置信息
			parameterMapElement(context.evalNodes("/mapper/parameterMap"));
			//解析设置的所有resultMap节点配置信息
			resultMapElements(context.evalNodes("/mapper/resultMap"));
			//解析设置的所有sql片段节点配置信息
			sqlElement(context.evalNodes("/mapper/sql"));
			//解析设置的所有sql语句节点对应的配置信息
			buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
		} catch (Exception e) {
			throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
		}
	}

	/*
	 * 解析设置的所有sql语句节点对应的配置信息
	 */
	private void buildStatementFromContext(List<XNode> list) {
		//检测是否配置了数据库当前运行的环境标识
		if (configuration.getDatabaseId() != null) {
			//检测满足当前数据库运行环境标识的sql语句节点
			buildStatementFromContext(list, configuration.getDatabaseId());
		}
		//检测不需要满足当前数据库运行环境标识对应的sql语句节点
		buildStatementFromContext(list, null);
	}

	/*
	 * 检测给定的sql语句节点是否满足给定数据库运行环境的语句解析
	 */
	private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
		//循环遍历所有的sql语句节点
		for (XNode context : list) {
			//创建对应的解析xml中sql语句的构建器对象
			final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
			try {
				//进行sql语句节点的解析操作处理
				statementParser.parseStatementNode();
			} catch (IncompleteElementException e) {
				//当出现未完成异常时,将对应的解析对象添加到未完成集合中------->期待后期进一步进行解析操作处理
				configuration.addIncompleteStatement(statementParser);
			}
		}
	}

	private void parsePendingResultMaps() {
		Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
		synchronized (incompleteResultMaps) {
			Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().resolve();
					iter.remove();
				} catch (IncompleteElementException e) {
					// ResultMap is still missing a resource...
				}
			}
		}
	}

	private void parsePendingCacheRefs() {
		Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
		synchronized (incompleteCacheRefs) {
			Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().resolveCacheRef();
					iter.remove();
				} catch (IncompleteElementException e) {
					// Cache ref is still missing a resource...
				}
			}
		}
	}

	private void parsePendingStatements() {
		Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
		synchronized (incompleteStatements) {
			Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
			while (iter.hasNext()) {
				try {
					iter.next().parseStatementNode();
					iter.remove();
				} catch (IncompleteElementException e) {
					//Statement is still missing a resource...
				}
			}
		}
	}

	/*
	 * 解析缓存引用节点配置信息
	 */
	private void cacheRefElement(XNode context) {
		//检测对应的缓存引用节点是否存在
		if (context != null) {
			//在配置信息对象中添加缓存引用的关联关系
			configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
			//创建对应的缓存引用解析器对象------------>通过阅读代码,可以发现一种常见的编程方式(封装处理,即对对应的缓存引用进行再一次封装,由封装类去进行调用真正的解析处理)
			CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
			try {
				//触发对引用缓存的解析处理----------->注意如果系统没有添加本缓存,那么会触发对应的未解析完成异常
				cacheRefResolver.resolveCacheRef();
			} catch (IncompleteElementException e) {
				//尚未解析到对应的缓存对象,将当前带解析的引用缓存添加到未完成解析集合中
				configuration.addIncompleteCacheRef(cacheRefResolver);
			}
		}
	}

	/*
	 * 解析缓存节点配置信息
	 */
	private void cacheElement(XNode context) throws Exception {
		//检测对应的缓存节点是否配置
		if (context != null) {
			//
			String type = context.getStringAttribute("type", "PERPETUAL");
			//
			Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
			//
			String eviction = context.getStringAttribute("eviction", "LRU");
			//
			Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
			//
			Long flushInterval = context.getLongAttribute("flushInterval");
			//
			Integer size = context.getIntAttribute("size");
			//
			boolean readWrite = !context.getBooleanAttribute("readOnly", false);
			//
			boolean blocking = context.getBooleanAttribute("blocking", false);
			//
			Properties props = context.getChildrenAsProperties();
			//
			builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
		}
	}

	/*
	 * 解析设置的所有parameterMap节点配置信息
	 */
	private void parameterMapElement(List<XNode> list) throws Exception {
		for (XNode parameterMapNode : list) {
			String id = parameterMapNode.getStringAttribute("id");
			String type = parameterMapNode.getStringAttribute("type");
			Class<?> parameterClass = resolveClass(type);
			List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
			List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
			for (XNode parameterNode : parameterNodes) {
				String property = parameterNode.getStringAttribute("property");
				String javaType = parameterNode.getStringAttribute("javaType");
				String jdbcType = parameterNode.getStringAttribute("jdbcType");
				String resultMap = parameterNode.getStringAttribute("resultMap");
				String mode = parameterNode.getStringAttribute("mode");
				String typeHandler = parameterNode.getStringAttribute("typeHandler");
				Integer numericScale = parameterNode.getIntAttribute("numericScale");
				ParameterMode modeEnum = resolveParameterMode(mode);
				Class<?> javaTypeClass = resolveClass(javaType);
				JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
				@SuppressWarnings("unchecked")
				Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
				ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
				parameterMappings.add(parameterMapping);
			}
			builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
		}
	}

	/*
	 * 解析设置的所有resultMap节点配置信息
	 */
	private void resultMapElements(List<XNode> list) throws Exception {
		//循环遍历所有的resultMap节点
		for (XNode resultMapNode : list) {
			try {
				//进行resultMap节点的解析操作处理
				resultMapElement(resultMapNode);
			} catch (IncompleteElementException e) {
				//ignore, it will be retried
			}
		}
	}

	/*
	 * 解析单个给定的resultMap节点信息
	 */
	private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
		//此处为解析对应的resultMap节点创建了对应的集合进行存储数据
		return resultMapElement(resultMapNode, Collections.<ResultMapping>emptyList());
	}

	/*
	 * 对给定的单个resultMap节点进行具体的解析操作处理
	 */
	private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings) throws Exception {
		ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
		//获取当前resultMap节点上配置的id唯一标识属性
		String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
		//获取当前resultMap节点上配置的对应java类的类型
		String type = resultMapNode.getStringAttribute("type", resultMapNode.getStringAttribute("ofType", resultMapNode.getStringAttribute("resultType", resultMapNode.getStringAttribute("javaType"))));
		//获取当前resultMap节点上配置的继承自那个对应的resultMap节点
		String extend = resultMapNode.getStringAttribute("extends");
		//获取当前resultMap节点上配置的自动进行映射标识
		Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
		//获取resultMap对应的java类对应的类型----------------->即最终需要转换成对应的java类的类型
		Class<?> typeClass = resolveClass(type);
		Discriminator discriminator = null;
		//创建用于存储ResultMapping的映射关系对象的集合
		List<ResultMapping> resultMappings = new ArrayList<ResultMapping>();
		//
		resultMappings.addAll(additionalResultMappings);
		//获取resultMap节点配置的所有子一级节点
		List<XNode> resultChildren = resultMapNode.getChildren();
		//循环处理所有的子级节点
		for (XNode resultChild : resultChildren) {
			//根据节点名称类型进行区别对待
			if ("constructor".equals(resultChild.getName())) {
				//解析配置的对应java类中对应的构建函数的节点配置
				processConstructorElement(resultChild, typeClass, resultMappings);
			} else if ("discriminator".equals(resultChild.getName())) {
				//
				discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
			} else {
				//此处是处理普通类型的resultMapping类型的节点的解析
				//创建对应的标识集合
				List<ResultFlag> flags = new ArrayList<ResultFlag>();
				//检测当前的节点是否是id类型的节点
				if ("id".equals(resultChild.getName())) {
					//设置对应的节点时id标识节点类型
					flags.add(ResultFlag.ID);
				}
				//将对应的解析后的resultMapping映射节点添加到对应的映射信息集合中
				resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
			}
		}
		//
		ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
		try {
			//
			return resultMapResolver.resolve();
		} catch (IncompleteElementException e) {
			//
			configuration.addIncompleteResultMap(resultMapResolver);
			//
			throw e;
		}
	}

	/*
	 * 解析resultMap节点中constructor节点的配置信息
	 *   即对应的java类中对应的构造函数的匹配关系
	 */
	private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		//获取constructor节点下的所有节点
		List<XNode> argChildren = resultChild.getChildren();
		//循环解析所有的子级节点---------->注意此处循环处理的构造节点中的所有节点数据------>此处的配置与真实java类中的构造函数参数是对应关系
		for (XNode argChild : argChildren) {
			//创建对应的存储标识的集合对象
			List<ResultFlag> flags = new ArrayList<ResultFlag>();
			//添加当前属性为构造节点类型的标识
			flags.add(ResultFlag.CONSTRUCTOR);
			//检测当前节点是否是idArg命名的节点
			if ("idArg".equals(argChild.getName())) {
				//添加当前属性为ID类型的标识
				flags.add(ResultFlag.ID);
			}
			//将对应的constructor节点下对应的子级节点转换成对应的resultMapping对象存储到集合中
			resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
		}
	}

	/*
	 * 
	 */
	private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
		//获取节点的相关属性值
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		String typeHandler = context.getStringAttribute("typeHandler");
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		Map<String, String> discriminatorMap = new HashMap<String, String>();
		for (XNode caseChild : context.getChildren()) {
			String value = caseChild.getStringAttribute("value");
			String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings));
			discriminatorMap.put(value, resultMap);
		}
		return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
	}

	/*
	 * 解析设置的所有sql片段节点配置信息
	 */
	private void sqlElement(List<XNode> list) throws Exception {
		//检测是否配置了当前运行的数据库环境对应的数据库标识
		if (configuration.getDatabaseId() != null) {
			//以含有必要标识的方式进行过滤对应的sql片段的处理
			sqlElement(list, configuration.getDatabaseId());
		}
		//以不含必要标识的方式进行添加sql片段的处理
		sqlElement(list, null);
	}

	/*
	 * 根据是否有对应的标识来添加sql片段的处理
	 */
	private void sqlElement(List<XNode> list, String requiredDatabaseId) throws Exception {
		//循环遍历所有的sql片段
		for (XNode context : list) {
			//获取当前sql片段对应的数据库运行环境标识
			String databaseId = context.getStringAttribute("databaseId");
			//获取当前片段的唯一标识id值
			String id = context.getStringAttribute("id");
			//拼接对应的唯一标识----->即对相对名的转换成对应的全限定名
			id = builderAssistant.applyCurrentNamespace(id, false);
			//检测当前的sql片段是否能够添加到sql片段集合中
			if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
				//将对应的sql片段添加到sql片段集合中
				sqlFragments.put(id, context);
			}
		}
	}

	/*
	 * 获取给定的sql片段是否能够添加到sql片段集合中的标识
	 * 
	 * 返回真的几种情况
	 *    1. 存在数据库运行环境标识    同时 sql片段的运行环境向匹配
	 *    2. 不存在数据库运行环境标识  sql片段不配置运行环境 
	 *          a.当前标识不在sql片段中
	 */
	private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
		//检测是否配置了当前数据库的运行环境标识
		if (requiredDatabaseId != null) {
			//检测当前sql片段的运行环境标识和当前数据库的运行环境标识是否一致
			if (!requiredDatabaseId.equals(databaseId)) {
				//不一致就返回不进行添加处理的标识
				return false;
			}
		} else {
			//检测当前sql片段是否配置了运行环境
			if (databaseId != null) {
				//即在没有要求运行环境的情况下,在对应的sql片段中添加运行环境,那么本sql片段不能添加到系统中
				return false;
			}
			//检测对应id标识的片段是否已经存储在集合中
			if (this.sqlFragments.containsKey(id)) {
				//获取对应id的sql片段节点数据
				XNode context = this.sqlFragments.get(id);
				//检测已经存储的sql片段节点中是否存储环境标识
				if (context.getStringAttribute("databaseId") != null) {
					//即相同id,有环境标识的sql片段不能够不进行替换
					return false;
				}
			}
		}
		//返回能够进行插入集合的处理操作
		return true;
	}

	/*
	 * 根据resultMap节点下配置的子节点来解析对应的子节点,将其转换成对应ResultMapping对象
	 *   注意本方法是 一般的节点或者对应的构造节点中的子级节点才会进入此处进行分析操作处理,对应discriminator节点不会进入此处进行解析处理
	 */
	private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
		String property;
		if (flags.contains(ResultFlag.CONSTRUCTOR)) {
			property = context.getStringAttribute("name");
		} else {
			property = context.getStringAttribute("property");
		}
		String column = context.getStringAttribute("column");
		String javaType = context.getStringAttribute("javaType");
		String jdbcType = context.getStringAttribute("jdbcType");
		
		String nestedSelect = context.getStringAttribute("select");
		
		String nestedResultMap = context.getStringAttribute("resultMap", processNestedResultMappings(context, Collections.<ResultMapping>emptyList()));
		
		String notNullColumn = context.getStringAttribute("notNullColumn");
		String columnPrefix = context.getStringAttribute("columnPrefix");
		String typeHandler = context.getStringAttribute("typeHandler");
		String resultSet = context.getStringAttribute("resultSet");
		String foreignColumn = context.getStringAttribute("foreignColumn");
		
		boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
		Class<?> javaTypeClass = resolveClass(javaType);
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler<?>> typeHandlerClass = (Class<? extends TypeHandler<?>>) resolveClass(typeHandler);
		//
		JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
		//
		return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
	}

	private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings) throws Exception {
		if ("association".equals(context.getName()) || "collection".equals(context.getName()) || "case".equals(context.getName())) {
			if (context.getStringAttribute("select") == null) {
				ResultMap resultMap = resultMapElement(context, resultMappings);
				return resultMap.getId();
			}
		}
		return null;
	}

	private void bindMapperForNamespace() {
		String namespace = builderAssistant.getCurrentNamespace();
		if (namespace != null) {
			Class<?> boundType = null;
			try {
				boundType = Resources.classForName(namespace);
			} catch (ClassNotFoundException e) {
				// ignore, bound type is not required
			}
			if (boundType != null) {
				if (!configuration.hasMapper(boundType)) {
					// Spring may not know the real resource name so we set a flag
					// to prevent loading again this resource from the mapper interface
					// look at MapperAnnotationBuilder#loadXmlResource
					configuration.addLoadedResource("namespace:" + namespace);
					configuration.addMapper(boundType);
				}
			}
		}
	}

}
