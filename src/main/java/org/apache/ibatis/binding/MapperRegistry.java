package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 进行数据库操作的mapper注册器处理类
 */
public class MapperRegistry {

	//对应的配置信息对象
	private final Configuration config;
	//用于存储已经明确的mapper集合对象
	private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

	public MapperRegistry(Configuration config) {
		this.config = config;
	}

	/*
	 * 根据提供的类型获取对应的Mapper对象----------------------->注意这个地方获取的不是接口,而是接口的实现类(通过对应的代理直接来完成的)
	 */
	@SuppressWarnings("unchecked")
	public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
		//在对应的集合中查找对应Mapper类型的代理工厂对象------------------------>注意这个地方是一个代理的工厂对象
		final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
		//检测对应类型的代理工厂是否存在
		if (mapperProxyFactory == null) {
			//没有对应类型的Mapper会抛出异常信息
			throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
		}
		try {
			//此处是真正的创建对应的代理对象的处理---------------------->即接口的代理对象都是在此处进行创建的------------>即通过jdk提供的动态代理来创建对应的代理对象---------->基于接口的代理对象
			return mapperProxyFactory.newInstance(sqlSession);
		} catch (Exception e) {
			throw new BindingException("Error getting mapper instance. Cause: " + e, e);
		}
	}

	/*
	 * 检测对应的类型是否已经在mapper集合中
	 */
	public <T> boolean hasMapper(Class<T> type) {
		return knownMappers.containsKey(type);
	}

	/*
	 * 以提供的类型进行注册mapper操作处理
	 */
	public <T> void addMapper(Class<T> type) {
		//首先检测给定的类型是否是接口类型
		if (type.isInterface()) {
			//检测对应类型的mapper是否已经注册到集合中
			if (hasMapper(type)) {
				throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
			}
			//初始化是否完成对应解析操作处理的标识
			//添加这个标识的目的是 在解析并添加对应mapper过程中 可以会遇到缺少相关信息从而导致不能够进行解析完成,从而进行中断处理,同时根据这个标识将对应的mapper加入到后期进行进一步解析处理的队列中
			boolean loadCompleted = false;
			try {
				//这个地方非常的重要,在没有解析前就将对应的类型添加到了集合中了
				knownMappers.put(type, new MapperProxyFactory<T>(type));
				// It's important that the type is added before the parser is run
				// otherwise the binding may automatically be attempted by the
				// mapper parser. If the type is already known, it won't try.
				//创建对应的基于注解方式的mapper解析构建器对象
				MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
				//进行解析基于注解的mapper----------------->此处在解析对应mapper的使用缓存对象时可能会出现抛出异常,即整个mapper需要延迟进行解析操作处理
				parser.parse();
				//顺利解析完成,设置对应的完成标识
				loadCompleted = true;
			} finally {
				//检测是否完成对mapper文件的解析,根据标识来确定是否进行移除操作处理
				if (!loadCompleted) {
					//没有解析完,将其类型移除对应的集合
					knownMappers.remove(type);
				}
			}
		}
	}

	/*
	 * 以包名的方式进行注册mapper对象
	 */
	public void addMappers(String packageName) {
		addMappers(packageName, Object.class);
	}
	
	/*
	 * 以包名和指定对应父类的方式进行注册mapper对象
	 */
	public void addMappers(String packageName, Class<?> superType) {
		//创建对应的资源扫描器对象
		ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
		//设置扫描对应包下继承自对应的类的所有类对象
		resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
		//获取所有扫描到的满足条件的类对象
		Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
		//循环进行注册操作处理
		for (Class<?> mapperClass : mapperSet) {
			//将对应的mapper进行注册操作处理
			addMapper(mapperClass);
		}
	}
	
	public Collection<Class<?>> getMappers() {
		return Collections.unmodifiableCollection(knownMappers.keySet());
	}

}
