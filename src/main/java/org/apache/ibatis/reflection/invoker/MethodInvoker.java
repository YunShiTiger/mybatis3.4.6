package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 执行对应方法的执行器处理类
 */
public class MethodInvoker implements Invoker {

	private final Class<?> type;
	private final Method method;

	public MethodInvoker(Method method) {
		this.method = method;
		//此处通过参数的个数来进一步确实是那种对应的属性方法
		if (method.getParameterTypes().length == 1) {
			type = method.getParameterTypes()[0];
		} else {
			type = method.getReturnType();
		}
	}

	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return method.invoke(target, args);
	}

	@Override
	public Class<?> getType() {
		return type;
	}
	
}
