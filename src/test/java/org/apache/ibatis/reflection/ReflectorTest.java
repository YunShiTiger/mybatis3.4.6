package org.apache.ibatis.reflection;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import static com.googlecode.catchexception.apis.BDDCatchException.*;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * 用于测试反射处理类对应的测试用例
 */
public class ReflectorTest {

	@Test
	public void testGetSetterType() throws Exception {
		//创建默认的反射处理类工厂对象
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		//获取对应类的反射处理类对象
		Reflector reflector = reflectorFactory.findForClass(Section.class);
		Assert.assertEquals(Long.class, reflector.getSetterType("id"));
	}

	@Test
	public void testGetGetterType() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Section.class);
		Assert.assertEquals(Long.class, reflector.getGetterType("id"));
	}

	@Test
	public void shouldNotGetClass() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Section.class);
		Assert.assertFalse(reflector.hasGetter("class"));
	}

	static interface Entity<T> {
		
		T getId();

		void setId(T id);
	}

	static abstract class AbstractEntity implements Entity<Long> {

		private Long id;

		@Override
		public Long getId() {
			return id;
		}

		@Override
		public void setId(Long id) {
			this.id = id;
		}
	}

	static class Section extends AbstractEntity implements Entity<Long> {

	}

	@Test
	public void shouldResolveSetterParam() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(String.class, reflector.getSetterType("id"));
	}

	@Test
	public void shouldResolveParameterizedSetterParam() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(List.class, reflector.getSetterType("list"));
	}

	@Test
	public void shouldResolveArraySetterParam() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		Class<?> clazz = reflector.getSetterType("array");
		assertTrue(clazz.isArray());
		assertEquals(String.class, clazz.getComponentType());
	}

	@Test
	public void shouldResolveGetterType() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(String.class, reflector.getGetterType("id"));
	}

	@Test
	public void shouldResolveSetterTypeFromPrivateField() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(String.class, reflector.getSetterType("fld"));
	}

	@Test
	public void shouldResolveGetterTypeFromPublicField() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(String.class, reflector.getGetterType("pubFld"));
	}

	@Test
	public void shouldResolveParameterizedGetterType() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		assertEquals(List.class, reflector.getGetterType("list"));
	}

	@Test
	public void shouldResolveArrayGetterType() throws Exception {
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Child.class);
		Class<?> clazz = reflector.getGetterType("array");
		assertTrue(clazz.isArray());
		assertEquals(String.class, clazz.getComponentType());
	}

	static abstract class Parent<T extends Serializable> {
		protected T id;
		protected List<T> list;
		protected T[] array;
		private T fld;
		public T pubFld;

		public T getId() {
			return id;
		}

		public void setId(T id) {
			this.id = id;
		}

		public List<T> getList() {
			return list;
		}

		public void setList(List<T> list) {
			this.list = list;
		}

		public T[] getArray() {
			return array;
		}

		public void setArray(T[] array) {
			this.array = array;
		}

		public T getFld() {
			return fld;
		}
	}

	static class Child extends Parent<String> {

	}

	@Test
	public void shouldResoleveReadonlySetterWithOverload() throws Exception {
		
		class BeanClass implements BeanInterface<String> {
			
			@Override
			public void setId(String id) {
				
			}
		}
		
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(BeanClass.class);
		assertEquals(String.class, reflector.getSetterType("id"));
	}

	interface BeanInterface<T> {
		void setId(T id);
	}

	@Test
	public void shouldSettersWithUnrelatedArgTypesThrowException() throws Exception {
		
		@SuppressWarnings("unused")
		class BeanClass {
			public void setTheProp(String arg) {
				
			}

			public void setTheProp(Integer arg) {
				
			}
		}

		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		when(reflectorFactory).findForClass(BeanClass.class);
		then(caughtException()).isInstanceOf(ReflectionException.class).hasMessageContaining("theProp").hasMessageContaining("BeanClass").hasMessageContaining("java.lang.String").hasMessageContaining("java.lang.Integer");
	}

	@Test
	public void shouldAllowTwoBooleanGetters() throws Exception {
		
		@SuppressWarnings("unused")
		class Bean {
			
			// JavaBean Spec allows this (see #906)
			public boolean isBool() {
				return true;
			}

			public boolean getBool() {
				return false;
			}

			public void setBool(boolean bool) {
				
			}
		}
		
		ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
		Reflector reflector = reflectorFactory.findForClass(Bean.class);
		assertTrue((Boolean) reflector.getGetInvoker("bool").invoke(new Bean(), new Byte[0]));
	}
	
}
