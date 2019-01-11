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
	//设置外部设定的全局环境变量值
	private String environment;
	//设置默认的反射对象生成工厂对象
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
		//此处非常的重要,它初始化了对应的配置信息对象-------------------------------------------->这个地方说明了 在解析对应的xml文件之前需要新初始化好对应的配置信息对象(此处是唯一创建配置信息类对象的地方)
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

	/*
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
			//根据获取的属性节点配置信息来初始化Vfs对象
			loadCustomVfs(settings);
			
			//解析配置文件中typeAliases节点配置的别名信息
			typeAliasesElement(root.evalNode("typeAliases"));
			
			//解析配置文件中plugins节点配置的插件信息
			pluginElement(root.evalNode("plugins"));
			
			//解析配置文件中objectFactory节点配置的对象工厂信息
			objectFactoryElement(root.evalNode("objectFactory"));
			
			//解析配置文件中objectWrapperFactory节点配置的包装对象工厂信息
			objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
			
			//解析配置文件中reflectorFactory节点配置的反射工厂处理类
			reflectorFactoryElement(root.evalNode("reflectorFactory"));
			
			//通过解析settings节点获取的相关属性来配置对应的配置信息类对象------------------->这个地方为什么不在解析完成节点之后直接执行,而是在执行完一定的解析后再进行设置呢 ？？？？？？？？？？？？
			settingsElement(settings);
			
			//read it after objectFactory and objectWrapperFactory issue #631
			//解析配置文件中environments节点配置的数据库执行环境信息
			environmentsElement(root.evalNode("environments"));
			
			//解析配置文件中databaseIdProvider节点配置的当前数据库对应的运行环境标识
			databaseIdProviderElement(root.evalNode("databaseIdProvider"));
			
			//解析配置文件中typeHandlers节点配置的类型转化器
			typeHandlerElement(root.evalNode("typeHandlers"));
			
			//解析配置文件中mappers节点配置的数据库操作映射
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
		//获取对Configuration类的反射属性解析操作处理------->即获取Configuration类的相关属性信息
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

	/*
	 * 根据获取的配置属性来设置vfs的加载方式
	 */
	private void loadCustomVfs(Properties props) throws ClassNotFoundException {
		//首先检测是否配置了对应的vfsImpl实现方式
		String value = props.getProperty("vfsImpl");
		//检测是否获取对应的属性
		if (value != null) {
			//进行字符串分割获取配置的多个实现方式
			String[] clazzes = value.split(",");
			//循环遍历所有给定的实现方式
			for (String clazz : clazzes) {
				//检测对应的实现方式是否为空
				if (!clazz.isEmpty()) {
					@SuppressWarnings("unchecked")
					//加载对应的实现方式类
					Class<? extends VFS> vfsImpl = (Class<? extends VFS>) Resources.classForName(clazz);
					//将对应的实现方式设置给配置信息实体类对象
					configuration.setVfsImpl(vfsImpl);
				}
			}
		}
	}

	/*
	 * 解析对应的typeAliases节点的别名信息
	 * 通过分析代码发现有两种设置别名的方式
	 *    1 通过配置package来进行扫描对应包名下的类来进行设置
	 *    2 通过配置alias和type属性进行设置
	 */
	private void typeAliasesElement(XNode parent) {
		//检测对应的节点是否存在
		if (parent != null) {
			//解析配置的别名子节点信息
			for (XNode child : parent.getChildren()) {
				//检测是否是通过包名方式进行注册
				if ("package".equals(child.getName())) {
					//获取对应的包名
					String typeAliasPackage = child.getStringAttribute("name");
					//以包名的方式进行批量注册别名处理
					configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
				} else {
					//获取设定的别名
					String alias = child.getStringAttribute("alias");
					//获取设置需要起别名的类
					String type = child.getStringAttribute("type");
					try {
						//加载对应的类资源-------------->如果此处没有加载到对应的资源类就会抛出对应的异常信息
						Class<?> clazz = Resources.classForName(type);
						//检测是否自己设定了对应的别名
						if (alias == null) {
							//通过使用简单类名或者注解的方式进行别名注册
							typeAliasRegistry.registerAlias(clazz);
						} else {
							//通过指定别名的方式进行注册
							typeAliasRegistry.registerAlias(alias, clazz);
						}
					} catch (ClassNotFoundException e) {
						throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
					}
				}
			}
		}
	}

	/*
	 * 解析对应的plugins节点的插件信息
	 */
	private void pluginElement(XNode parent) throws Exception {
		//检测对应的节点是否存在
		if (parent != null) {
			//循环遍历所有的插件子节点
			for (XNode child : parent.getChildren()) {
				//获取需要加载的插件处理类
				String interceptor = child.getStringAttribute("interceptor");
				//获取配置字属性信息
				Properties properties = child.getChildrenAsProperties();
				//加载对应的插件处理类
				Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
				//给对应的插件添加对应的配置属性
				interceptorInstance.setProperties(properties);
				//将配置好的插件添加到插件链中
				configuration.addInterceptor(interceptorInstance);
			}
		}
	}

	/*
	 * 解析对应的objectFactory节点的对象工厂
	 */
	private void objectFactoryElement(XNode context) throws Exception {
		//检测对应的节点是否存在
		if (context != null) {
			//获取配置的工厂对象的加载位置
			String type = context.getStringAttribute("type");
			//获取配置给本工厂中的属性信息
			Properties properties = context.getChildrenAsProperties();
			//创建对应的对象工厂类对象
			ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
			//给对象工厂设置对应的属性信息
			factory.setProperties(properties);
			//将配置的工厂对象设置到配置信息类对象中
			configuration.setObjectFactory(factory);
		}
	}

	/*
	 * 解析对应的objectWrapperFactory节点的包装对象工厂
	 */
	private void objectWrapperFactoryElement(XNode context) throws Exception {
		//检测对应的节点是否存在
		if (context != null) {
			//获取设置的包装对象工厂的类位置
			String type = context.getStringAttribute("type");
			//加载并创建对应的包装对象工厂
			ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
			//将对应的包装对象工厂设置给配置信息对象
			configuration.setObjectWrapperFactory(factory);
		}
	}

	/*
	 * 解析对应的reflectorFactory节点的获取设置的反射工厂处理类
	 */
	private void reflectorFactoryElement(XNode context) throws Exception {
		//检测对应的节点是否存在
		if (context != null) {
			//获取配置的反射工厂对应的位置
			String type = context.getStringAttribute("type");
			//加载对应的类对象并创建对应的反射工厂处理类对象
			ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
			//给配置信息对象设置对应的反射工厂处理类对象
			configuration.setReflectorFactory(factory);
		}
	}

	/*
	 * 根据解析的settings节点获取的属性信息来设置配置信息类对象
	 */
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

	/*
	 * 解析配置文件中environments节点配置的数据库执行环境信息
	 */
	private void environmentsElement(XNode context) throws Exception {
		//检测是否配置了对应的节点
		if (context != null) {
			//检测是否通过全局配置了环境信息
			if (environment == null) {
				//获取当前环境节点中对应的默认信息
				environment = context.getStringAttribute("default");
			}
			//循环遍历环境节点中对应的子节点信息(即environments中可以有多个environment子节点   所有需要根据一定的策略来选择对应的执行环境)
			//此处的目的是  可以配置多个环境对象(例如测试环境 生成环境  就需要两套对应的环境信息配置)
			for (XNode child : context.getChildren()) {
				//获取对应的环境id属性-------->注意此处的id属性在environment节点中必须要进行配置
				String id = child.getStringAttribute("id");
				//检测当前配置的环境信息是否是需要的执行环境信息
				if (isSpecifiedEnvironment(id)) {
					//解析获取对应的事物工厂对象
					TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
					//解析获取对应的数据源工厂对象
					DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
					//通过数据源工厂创建对应的数据源对象
					DataSource dataSource = dsFactory.getDataSource();
					//通过环境id 事物工厂 数据源构建对应的环境装配类对象
					Environment.Builder environmentBuilder = new Environment.Builder(id).transactionFactory(txFactory).dataSource(dataSource);
					//给配置信息对象设置对应的执行环境对象
					configuration.setEnvironment(environmentBuilder.build());
				}
			}
		}
	}
	
	/*
	 * 解析配置文件中environment节点下配置的transactionManager节点中事物管理器对象的信息
	 */
	private TransactionFactory transactionManagerElement(XNode context) throws Exception {
		//检测对应的节点是否存在
		if (context != null) {
			//获取进行事物管理的类型
			String type = context.getStringAttribute("type");
			//获取配置的属性信息
			Properties props = context.getChildrenAsProperties();
			//创建对应的事物管理器工厂对象
			TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
			//给事物管理器对象设置对应的属性
			factory.setProperties(props);
			//返回对应的事物管理器工厂对象
			return factory;
		}
		//对应的节点不存在就抛出异常------------->所以执行环境中必须要配置对应的transactionManager节点
		throw new BuilderException("Environment declaration requires a TransactionFactory.");
	}

	/*
	 * 解析配置文件中environment节点下配置的dataSource节点中数据源工厂的信息
	 */
	private DataSourceFactory dataSourceElement(XNode context) throws Exception {
		//检测对应的节点是否存在
		if (context != null) {
			//获取配置的数据源工厂类型
			String type = context.getStringAttribute("type");
			//获取对应的配置信息
			Properties props = context.getChildrenAsProperties();
			//创建对应类型的数据源工厂对象
			DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
			//给数据源工厂对象配置对应的属性信息
			factory.setProperties(props);
			//返回对应的数据源工厂对象
			return factory;
		}
		//对应的节点不存在就抛出异常------------->所以执行环境中必须要配置对应的dataSource节点
		throw new BuilderException("Environment declaration requires a DataSourceFactory.");
	}

	/*
	 * 解析配置文件中databaseIdProvider节点配置的
	 * mybatis可以根据不同的数据库厂商执行不同的语句,这种多厂商的支持是基于映射语句中的databaseId属性.mybatis会加载不带databaseId属性和带有匹配当前数据库databaseId属性的所有语句.如果同时找到带有databaseId和不带databaseId的相同语句,则后者会被舍弃.
	 */
	private void databaseIdProviderElement(XNode context) throws Exception {
		DatabaseIdProvider databaseIdProvider = null;
		//检测对应的节点是否配置
		if (context != null) {
			//获取配置的进行支持多厂商的适配的处理类类型
			String type = context.getStringAttribute("type");
			//从这里可以看出,指定为VENDOR,也会被当作DB_VENDOR
			if ("VENDOR".equals(type)) {
				type = "DB_VENDOR";
			}
			//获取配置的对应属性
			Properties properties = context.getChildrenAsProperties();
			//实例化别名为type的DatabaseIdProvider(内置的为DB_VENDOR)
			databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
			//给对应的DatabaseIdProvider设置对应的属性
			databaseIdProvider.setProperties(properties);
		}
		//获取当前的执行环境对象
		Environment environment = configuration.getEnvironment();
		//检测数据库执行环境和DatabaseIdProvider对象不为空------------>即配置了databaseIdProvider节点
		if (environment != null && databaseIdProvider != null) {
			//获取当前数据库执行环境对应的标识------->带有当前标识的数据库语句具有注册优先权
			String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
			//将对应的标识设置到配置信息对象上
			configuration.setDatabaseId(databaseId);
		}
	}

	/*
	 * 解析配置文件中typeHandlers节点配置的类型处理器
	 * 通过分析代码可以发现有两种方式来进行注册类型处理器对象
	 *   1 通过配置package来进行包扫描进行注册
	 *   2直接配置typeHandler来进行注册
	 */
	private void typeHandlerElement(XNode parent) throws Exception {
		//检测对应的节点是否配置
		if (parent != null) {
			//循环遍历所有的转化器子节点
			for (XNode child : parent.getChildren()) {
				//检测是否是配置以包形式进行注册类型处理器对象
				if ("package".equals(child.getName())) {
					//获取需要注册的类型处理器对象对应的包名
					String typeHandlerPackage = child.getStringAttribute("name");
					//以包名的形式进行注册对应的类型处理器对象
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

	/*
	 * 解析配置文件中mappers节点配置的数据库操作映射
	 * 通过分析代码发现有两大类进行设置数据库操作的映射方式
	 * 1 通过配置包名来进行扫描对应包下的类进行注册对应sql语句的处理 (以类的方式进行配置)
	 * 2通过配置相关数据来进行注册对应sql语句的处理 (以xml文件的形式进行配置)
	 *    a 配置resource
	 *    b 配置url
	 *    c 配置class
	 */
	private void mapperElement(XNode parent) throws Exception {
		//检测是否配置了对应的mappers节点
		if (parent != null) {
			//循环遍历所有对应的一级子节点
			for (XNode child : parent.getChildren()) {
				if ("package".equals(child.getName())) {
					//获取对应的配置的包名
					String mapperPackage = child.getStringAttribute("name");
					//根据提供的包名进行注册Mapper
					configuration.addMappers(mapperPackage);
				} else {
					//获取配置的相关属性
					String resource = child.getStringAttribute("resource");
					String url = child.getStringAttribute("url");
					String mapperClass = child.getStringAttribute("class");
					//根据配置属性的不同进行不同方式的注册操作处理
					if (resource != null && url == null && mapperClass == null) {
						ErrorContext.instance().resource(resource);
						//获取对应的mapper资源流对象
						InputStream inputStream = Resources.getResourceAsStream(resource);
						//创建对应的xml方式的mapper构建器对象
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
						//进行解析操作处理
						mapperParser.parse();
					} else if (resource == null && url != null && mapperClass == null) {
						ErrorContext.instance().resource(url);
						//获取对应的mapper资源流对象
						InputStream inputStream = Resources.getUrlAsStream(url);
						//创建对应的xml方式的mapper构建器对象
						XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
						//进行解析操作处理
						mapperParser.parse();
					} else if (resource == null && url == null && mapperClass != null) {
						//加载对应的类对象
						Class<?> mapperInterface = Resources.classForName(mapperClass);
						//以类的方式进行注册
						configuration.addMapper(mapperInterface);
					} else {
						throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
					}
				}
			}
		}
	}

	/*
	 * 检测给定id值的环境信息是否是当前需要的执行环境信息
	 */
	private boolean isSpecifiedEnvironment(String id) {
		//首先检测对应的需要的执行环境参数是否为空-------->这个参数必须设定  主要的设置来源有两个地方   
		//  1 创建全局SqlSessionManager对象时进行指定 (此处的配置的环境信息优先级最高)
		//  2配置xml文件中environments节点的default属性进行配置
		if (environment == null) {
			throw new BuilderException("No environment specified.");
		} else if (id == null) {
			//没有配置对应的id信息抛出异常------------->即environment节点必须配置id属性值
			throw new BuilderException("Environment requires an id attribute.");
		} else if (environment.equals(id)) {
			//如果配置的id环境和真实需要的执行环境相匹配就返回   此id对应的环境为真实需要的执行环境
			return true;
		}
		//信息不匹配,返回不是需要的执行环境
		return false;
	}

}
