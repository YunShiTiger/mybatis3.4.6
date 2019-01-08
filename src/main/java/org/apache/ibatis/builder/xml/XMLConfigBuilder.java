package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * 用于解析配置xml文件的解析处理类
 */
public class XMLConfigBuilder extends BaseBuilder {

	//用于标识是否进行了解析操作处理
	private boolean parsed;
	//用于记录对应的解析配置xml文件内容的搜索器对象
	private final XPathParser parser;
	//
	private String environment;
	//
	private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

	/**
	 * 通过重载的方式来提供创建XMLConfigBuilder对象的多种方式
	 */
	public XMLConfigBuilder(Reader reader) {
		this(reader, null, null);
	}

	public XMLConfigBuilder(Reader reader, String environment) {
		this(reader, environment, null);
	}

	public XMLConfigBuilder(Reader reader, String environment, Properties props) {
		this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
	}

	public XMLConfigBuilder(InputStream inputStream) {
		this(inputStream, null, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment) {
		this(inputStream, environment, null);
	}

	public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
		this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
	}

	//构造XMLConfigBuilder对象必须走的构造函数,用于初始化相关的参数对象
	private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
		//此处非常的重要,它初始化了对应的配置信息对象
		super(new Configuration());
		ErrorContext.instance().resource("SQL Mapper Configuration");
		//设置对应的全局配置信息
		this.configuration.setVariables(props);
		//设置当前处理尚未解析对应的xml配置文件的阶段
		this.parsed = false;
		//设置对应的环境信息
		this.environment = environment;
		//存储记录对应的解析xml文件的解析器对象
		this.parser = parser;
	}

	/**
	 * 触发解析对应的配置xml文件信息转换成对应的配置信息类对象的处理函数
	 */
	public Configuration parse() {
		//首先检测是否已经进行过解析操作处理
		if (parsed) {
			//抛出只能使用一次解析对应的配置xml文件
			throw new BuilderException("Each XMLConfigBuilder can only be used once.");
		}
		//设置解析状态位为已解析处理
		parsed = true;
		//根据对应的解析器来解析对应的配置节点------->即完成将xml中配置的信息转换成对应的配置信息类的处理操作
		parseConfiguration(parser.evalNode("/configuration"));
		//返回解析完成后对应的配置信息类对象
		return configuration;
	}

	/**
	 * 完成对应的配置文件的解析操作处理------->即将对应的xml中配置的数据信息转换成对应的配置信息类对象中的相关信息
	 * @param root
	 */
	private void parseConfiguration(XNode root) {
		try {
			//解析配置文件中properties节点配置的属性信息
			propertiesElement(root.evalNode("properties"));
			
			//解析配置文件中settings属性节点配置的属性信息
			Properties settings = settingsAsProperties(root.evalNode("settings"));
			loadCustomVfs(settings);
			
			//
			typeAliasesElement(root.evalNode("typeAliases"));
			
			//
			pluginElement(root.evalNode("plugins"));
			
			//
			objectFactoryElement(root.evalNode("objectFactory"));
			
			//
			objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
			
			//
			reflectorFactoryElement(root.evalNode("reflectorFactory"));
			settingsElement(settings);
			//read it after objectFactory and objectWrapperFactory issue #631
			
			//
			environmentsElement(root.evalNode("environments"));
			
			//
			databaseIdProviderElement(root.evalNode("databaseIdProvider"));
			
			//
			typeHandlerElement(root.evalNode("typeHandlers"));
			
			//
			mapperElement(root.evalNode("mappers"));
		} catch (Exception e) {
			//在解析对应的xml配置文件过程中出现了错误,抛出对应的异常信息
			throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
		}
	}
	
	/*
	 * 解析配置文件中对应的properties属性节点配置的相关信息   
	 	<properties resource="org/apache/ibatis/builder/jdbc.properties">
    		<property name="prop1" value="aaaa"/>
    		<property name="jdbcTypeForNull" value="NULL" />
  		</properties>
  	 通过分析代码逻辑可以得出---->主要的属性配置信息主要来自三种来源
  	    1 全局配置传入的属性信息
  	    2 properties属性节点中配置的属性resource或者url指定的配置信息
  	    3 properties节点内部配置的属性信息
  	    
  	    通过分析最近三种属性来源的覆盖情况为: 全局传入的属性信息  > resource和url指定的配置信息   >  在属性节点中配置的属性信息
	 */
	private void propertiesElement(XNode context) throws Exception {
		//首先检测对应的节点是否存在
		if (context != null) {
			//获取当前节点内子节点的中配置的属性信息
			Properties defaults = context.getChildrenAsProperties();
			//获取本节点上是否配置了resource属性
			String resource = context.getStringAttribute("resource");
			//获取本节点上是否配置了url属性
			String url = context.getStringAttribute("url");
			//进行检测在本节点上resource和url属性不能同时配置的处理
			if (resource != null && url != null) {
				throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
			}
			//加载对应的url或者resource中指定的配置属性信息
			if (resource != null) {
				defaults.putAll(Resources.getResourceAsProperties(resource));
			} else if (url != null) {
				defaults.putAll(Resources.getUrlAsProperties(url));
			}
			//获取配置的全局属性信息
			Properties vars = configuration.getVariables();
			if (vars != null) {
				//将全局属性信息放置到属性对象中
				defaults.putAll(vars);
			}
			//将最终的所有属性信息对象设置给解析器对象------------------>即在解析器对象中可以拿到所有的属性信息
			parser.setVariables(defaults);
			//将最终的所有属性信息对象设置给配置信息对象------------------>即在配置信息对象中可以拿到所有的属性信息
			configuration.setVariables(defaults);
		}
	}

	/*
	 * 解析配置文件中对应的settings属性节点配置的相关信息   
	 */
	private Properties settingsAsProperties(XNode context) {
		//检测当前节点是否存在
		if (context == null) {
			//没有配置,返回对应的没有属性信息的空对象
			return new Properties();
		}
		//获取在settings节点中配置的属性信息
		Properties props = context.getChildrenAsProperties();
		// Check that all settings are known to the configuration class
		MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
		//循环检测对应的属性信息的键所对应的配置对象是否在Configuration对象中存在对应的get或者set设置方法
		for (Object key : props.keySet()) {
			//检测键对应的属性在配置信息类中是否有对应的get和set方法存在
			if (!metaConfig.hasSetter(String.valueOf(key))) {
				//不存在就抛出对应的异常----------------------->即配置settings节点信息出现了错误
				throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
			}
		}
		//返回对应的属性信息对象
		return props;
	}

	private void loadCustomVfs(Properties props) throws ClassNotFoundException {
		String value = props.getProperty("vfsImpl");
		if (value != null) {
			String[] clazzes = value.split(",");
			for (String clazz : clazzes) {
				if (!clazz.isEmpty()) {
					@SuppressWarnings("unchecked")
					Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
					configuration.setVfsImpl(vfsImpl);
				}
			}
		}
	}

	private void typeAliasesElement(XNode parent) {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String typeAliasPackage = child.getStringAttribute("name");
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					String alias = child.getStringAttribute("alias");
					String type = child.getStringAttribute("type");
					try {
						Class<?> clazz = Resources.classForName(type);
						if (alias == null) {
							typeAliasRegistry.registerAlias(clazz);
						} else {
							typeAliasRegistry.registerAlias(alias, clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
					}
				}
			}
		}
	}

	private void pluginElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				String interceptor = child.getStringAttribute("interceptor");
				Properties properties = child.getChildrenAsProperties();
				Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
				interceptorInstance.setProperties(properties);
				configuration.addInterceptor(interceptorInstance);
			}
		}
	}

	private void objectFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties properties = context.getChildrenAsProperties();
			ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
			factory.setProperties(properties);
			configuration.setObjectFactory(factory);
		}
	}

	private void objectWrapperFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
			configuration.setObjectWrapperFactory(factory);
		}
	}

	private void reflectorFactoryElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
			configuration.setReflectorFactory(factory);
		}
	}

	private void settingsElement(Properties props) throws Exception {
		configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
		configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
		configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
		configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
		configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
		configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
		configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
		configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
		configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
		configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
		configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
		configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
		configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
		configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
		configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
		configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
		configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
		configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
		configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
		@SuppressWarnings("unchecked")
		Class<? extends TypeHandler> typeHandler = (Class<? extends TypeHandler>) resolveClass(props.getProperty("defaultEnumTypeHandler"));
		configuration.setDefaultEnumTypeHandler(typeHandler);
		configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
		configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
		configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
		configuration.setLogPrefix(props.getProperty("logPrefix"));
		@SuppressWarnings("unchecked")
		Class<? extends Log> logImpl = (Class<? extends Log>) resolveClass(props.getProperty("logImpl"));
		configuration.setLogImpl(logImpl);
		configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
	}

	private void environmentsElement(XNode context) throws Exception {
		if (context != null) {
			if (environment == null) {
				environment = context.getStringAttribute("default");
			}
			for (XNode child : context.getChildren()) {
				String id = child.getStringAttribute("id");
				if (isSpecifiedEnvironment(id)) {
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					DataSource dataSource = dsFactory.getDataSource();
					Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory).dataSource(dataSource);
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}

	private void databaseIdProviderElement(XNode context) throws Exception {
		DatabaseIdProvider databaseIdProvider = null;
		if (context != null) {
			String type = context.getStringAttribute("type");
			// awful patch to keep backward compatibility
			if ("VENDOR".equals(type)) {
				type = "DB_VENDOR";
			}
			Properties properties = context.getChildrenAsProperties();
			databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
			databaseIdProvider.setProperties(properties);
		}
		Environment environment = configuration.getEnvironment();
		if (environment != null && databaseIdProvider != null) {
			String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
			configuration.setDatabaseId(databaseId);
		}
	}

	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}

	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		if (context != null) {
			String type = context.getStringAttribute("type");
			Properties props = context.getChildrenAsProperties();
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			factory.setProperties(props);
			return factory;
		}
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}

	private void typeHandlerElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String typeHandlerPackage = child.getStringAttribute("name");
					typeHandlerRegistry.register(typeHandlerPackage);
				} else {
					String javaTypeName = child.getStringAttribute("javaType");
					String jdbcTypeName = child.getStringAttribute("jdbcType");
					String handlerTypeName = child.getStringAttribute("handler");
					Class<?> javaTypeClass = resolveClass(javaTypeName);
					JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
					Class<?> typeHandlerClass = resolveClass(handlerTypeName);
					if (javaTypeClass != null) {
						if (jdbcType == null) {
							typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
						} else {
							typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
						}
					} else {
						typeHandlerRegistry.register(typeHandlerClass);
					}
				}
			}
		}
	}

	private void mapperElement(XNode parent) throws Exception {
		if (parent != null) {
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					String mapperPackage = child.getStringAttribute("name");
					configuration.addMappers(mapperPackage);
				} else {
					String resource = child.getStringAttribute("resource");
					String url = child.getStringAttribute("url");
					String mapperClass = child.getStringAttribute("class");
					if (resource != null && url == null && mapperClass == null) {
						ErrorContext.instance().resource(resource);
						InputStream inputStream = Resources.getResourceAsStream(resource);
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url != null && mapperClass == null) {
						ErrorContext.instance().resource(url);
						InputStream inputStream = Resources.getUrlAsStream(url);
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
						mapperParser.parse();
					} else if (resource == null && url == null && mapperClass != null) {
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						configuration.addMapper(mapperInterface);
					} else {
						throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}

	private boolean isSpecifiedEnvironment(String id) {
		if (environment == null) {
			throw new BuilderException("No environment specified.");
		} else if (id == null) {
			throw new BuilderException("Environment requires an id attribute.");
		} else if (environment.equals(id)) {
			return true;
		}
		return false;
	}

}
