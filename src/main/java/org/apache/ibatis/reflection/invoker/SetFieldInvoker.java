package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * 设置属性值的执行器处理类
 */
public class SetFieldInvoker implements Invoker {
	private final Field field;

	public SetFieldInvoker(Field field) {
		this.field = field;
	}

	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException {
		field.set(target, args[0]);
		return null;
	}

	@Override
	public Class<?> getType() {
		return field.getType();
	}
	
}
