package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于进行引用缓存处理类的注解
 *   引用的方式使用Class或者类的全限定名的方式进行引入
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespaceRef {
	
	/*
	 * 通过以Class方式类引入引用的缓存处理类
	 */
	Class<?> value() default void.class;

	/*
	 * 通过提供缓存处理类的限定名来进行引用对应的缓存处理类
	 */
	String name() default "";
}
