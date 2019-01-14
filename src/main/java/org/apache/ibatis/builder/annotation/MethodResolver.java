package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * 对尚未解析完成的方法的封装处理类
 */
public class MethodResolver {
	
	//记录对应的解析构建器对象
	private final MapperAnnotationBuilder annotationBuilder;
	//记录尚未解析完成的方法
	private final Method method;

	public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
		this.annotationBuilder = annotationBuilder;
		this.method = method;
	}

	//进一步触发对方法的解析操作处理
	public void resolve() {
		annotationBuilder.parseStatement(method);
	}

}