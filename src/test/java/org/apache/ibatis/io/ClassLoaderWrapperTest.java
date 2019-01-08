package org.apache.ibatis.io;

import org.apache.ibatis.BaseDataTest;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

/**
 * 对应封装的类加载器的相关测试用例
 */
public class ClassLoaderWrapperTest extends BaseDataTest {

	//记录封装的类加载器对象
	ClassLoaderWrapper wrapper;
	//记录指定的类加载器对象
	ClassLoader loader;
	private final String RESOURCE_NOT_FOUND = "some_resource_that_does_not_exist.properties";
	private final String CLASS_NOT_FOUND = "some.random.class.that.does.not.Exist";
	private final String CLASS_FOUND = "java.lang.Object";

	@Before
	public void beforeClassLoaderWrapperTest() {
		wrapper = new ClassLoaderWrapper();
		loader = getClass().getClassLoader();
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的存在的类对象
	 */
	@Test
	public void classForName() throws ClassNotFoundException {
		assertNotNull(wrapper.classForName(CLASS_FOUND));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的不存在的类对象
	 */
	@Test(expected = ClassNotFoundException.class)
	public void classForNameNotFound() throws ClassNotFoundException {
		assertNotNull(wrapper.classForName(CLASS_NOT_FOUND));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的存在的类对象
	 */
	@Test
	public void classForNameWithClassLoader() throws ClassNotFoundException {
		assertNotNull(wrapper.classForName(CLASS_FOUND, loader));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的不存在的类对象
	 */
	@Test
	public void getResourceAsURL() {
		assertNotNull(wrapper.getResourceAsURL(JPETSTORE_PROPERTIES));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的不存在的资源对象
	 */
	@Test
	public void getResourceAsURLNotFound() {
		assertNull(wrapper.getResourceAsURL(RESOURCE_NOT_FOUND));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的存在的资源对象
	 */
	@Test
	public void getResourceAsURLWithClassLoader() {
		assertNotNull(wrapper.getResourceAsURL(JPETSTORE_PROPERTIES, loader));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的存在的资源对象
	 */
	@Test
	public void getResourceAsStream() {
		assertNotNull(wrapper.getResourceAsStream(JPETSTORE_PROPERTIES));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的不存在的资源对象
	 */
	@Test
	public void getResourceAsStreamNotFound() {
		assertNull(wrapper.getResourceAsStream(RESOURCE_NOT_FOUND));
	}

	/**
	 * 测试使用封装的类加载器对象去加载对应的存在的资源对象
	 */
	@Test
	public void getResourceAsStreamWithClassLoader() {
		assertNotNull(wrapper.getResourceAsStream(JPETSTORE_PROPERTIES, loader));
	}
	
	/**
	 * 测试获取对应的类加载器
	 */
	@Test
	public void testClassLoader() {
		//sun.misc.Launcher$AppClassLoader@2ff4acd0
		System.err.println("通过线程对象获取的"+Thread.currentThread().getContextClassLoader().toString());
		//sun.misc.Launcher$AppClassLoader@2ff4acd0
		System.err.println("通过getClass对象获取的"+getClass().getClassLoader().toString());
		//sun.misc.Launcher$AppClassLoader@2ff4acd0
		System.err.println("通过ClassLoader对象获取的"+ClassLoader.getSystemClassLoader());
		//sun.misc.Launcher$ExtClassLoader@7591083d
		System.err.println("通过ClassLoader对象获取的"+ClassLoader.getSystemClassLoader().getParent());
		//null
		System.err.println("通过ClassLoader对象获取的"+ClassLoader.getSystemClassLoader().getParent().getParent());
	}

}
