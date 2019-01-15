package org.apache.ibatis.builder;

import org.apache.ibatis.cache.Cache;

/**
 * 进行缓存引用解析处理的解析器处理类
 */
public class CacheRefResolver {
	
	//记录带解析mapper所对应的解析辅助对象
	private final MapperBuilderAssistant assistant;
	//记录引用的缓存对应的命名空间值
	private final String cacheRefNamespace;

	public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
		this.assistant = assistant;
		this.cacheRefNamespace = cacheRefNamespace;
	}

	//触发对引用缓存的解析操作处理
	public Cache resolveCacheRef() {
		return assistant.useCacheRef(cacheRefNamespace);
	}
	
}