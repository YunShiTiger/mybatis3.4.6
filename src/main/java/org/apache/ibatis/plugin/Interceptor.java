package org.apache.ibatis.plugin;

import java.util.Properties;

/**
 * 统一插件接口
 */
public interface Interceptor {

	//
	Object intercept(Invocation invocation) throws Throwable;

	//
	Object plugin(Object target);

	//提供给对应的插件设置属性信息
	void setProperties(Properties properties);

}
