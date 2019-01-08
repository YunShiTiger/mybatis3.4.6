package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;

/**
 * 统一的触发器处理接口
 *     可以出发对应的方法执行    可以触发获取对应的属性   可以触发设置对应的属性
 */
public interface Invoker {
	
	//统一定义触发指定对应行为的接口
	Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;

	Class<?> getType();
}
