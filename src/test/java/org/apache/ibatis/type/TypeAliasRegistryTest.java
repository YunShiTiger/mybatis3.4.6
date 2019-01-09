package org.apache.ibatis.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.math.BigDecimal;

/*
 * 别名注册处理的测试用例类
 */
public class TypeAliasRegistryTest {

	/*
	 * 测试进行注册别名和解析操作处理
	 */
	@Test
	public void shouldRegisterAndResolveTypeAlias() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		typeAliasRegistry.registerAlias("rich", "org.apache.ibatis.domain.misc.RichType");
		assertEquals("org.apache.ibatis.domain.misc.RichType", typeAliasRegistry.resolveAlias("rich").getName());
	}

	/*
	 * 测试默认注册的别名
	 */
	@Test
	public void shouldFetchArrayType() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		assertEquals(Byte[].class, typeAliasRegistry.resolveAlias("byte[]"));
	}

	/*
	 * 测试一个别名可以进行多次重复注册同一类对象(其实集合中还是只有一条记录)
	 */
	@Test
	public void shouldBeAbleToRegisterSameAliasWithSameTypeAgain() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		typeAliasRegistry.registerAlias("String", String.class);
		typeAliasRegistry.registerAlias("string", String.class);
	}

	/*
	 * 测试一个别名不能够定义两个不一样的类对象
	 */
	@Test(expected = TypeException.class)
	public void shouldNotBeAbleToRegisterSameAliasWithDifferentType() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		typeAliasRegistry.registerAlias("string", BigDecimal.class);
	}

	/*
	 * 测试可以给一个别名定义一个空对象
	 */
	@Test
	public void shouldBeAbleToRegisterAliasWithNullType() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		typeAliasRegistry.registerAlias("foo", (Class<?>) null);
		assertNull(typeAliasRegistry.resolveAlias("foo"));
	}

	/*
	 * 测试一个别名开始对应空对象,后期可以使用非空对象进行替换处理
	 */
	@Test
	public void shouldBeAbleToRegisterNewTypeIfRegisteredTypeIsNull() {
		TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
		typeAliasRegistry.registerAlias("foo", (Class<?>) null);
		typeAliasRegistry.registerAlias("foo", String.class);
	}

}
