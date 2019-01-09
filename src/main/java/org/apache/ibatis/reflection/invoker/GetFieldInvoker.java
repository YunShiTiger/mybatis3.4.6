package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * 获取属性值的执行器处理类
 */
public class GetFieldInvoker implements Invoker {
	
	private final Field field;

	public GetFieldInvoker(Field field) {
		this.field = field;
	}

	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		return field.get(target);
	}

	@Override
	public Class<?> getType() {
		return field.getType();
	}
	
}
