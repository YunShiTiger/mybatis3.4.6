package org.apache.ibatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 插件链:记录配置文件所有配置的插件信息
 */
public class InterceptorChain {

	//记录插件的集合
	private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

	//执行配置的所有插件动作
	public Object pluginAll(Object target) {
		//循环配置的所有插件
		for (Interceptor interceptor : interceptors) {
			//执行对应的插件
			target = interceptor.plugin(target);
		}
		//返回执行完所有插件之后对应的结果
		return target;
	}

	//将对应的插件添加到插件链中
	public void addInterceptor(Interceptor interceptor) {
		interceptors.add(interceptor);
	}

	//获取插件链中配置的所有插件
	public List<Interceptor> getInterceptors() {
		return Collections.unmodifiableList(interceptors);
	}

}
