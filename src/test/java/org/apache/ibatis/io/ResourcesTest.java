package org.apache.ibatis.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.ibatis.BaseDataTest;
import org.junit.Test;

/**
 * 对资源加载类的测试用例
 */
public class ResourcesTest extends BaseDataTest {

	//记录对应的类加载器对象
	private static final ClassLoader CLASS_LOADER = ResourcesTest.class.getClassLoader();

	/**
	 * 测试根据对应的资源路径来创建对应的统一资源定位对象
	 */
	@Test
	public void shouldGetUrlForResource() throws Exception {
		//根据给定的路径创建对应的统一资源定位对象
		URL url = Resources.getResourceURL(JPETSTORE_PROPERTIES);
		
		//对应的数据格式  file:/E:/work/project/yoyoV/mybatis-3.4.6/target/test-classes/org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb.properties
		System.err.println(url.toString());
		
		assertTrue(url.toString().endsWith("jpetstore/jpetstore-hsqldb.properties"));
	}

	/**
	 * 测试根据对应的资源路径创建对应的统一资源定位对象并根据定位对象来创建对应的属性对象
	 */
	@Test
	public void shouldGetUrlAsProperties() throws Exception {
		//根据资源路径获取对应的资源定位对象
		URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
		//根据资源定位对象对应的资源路径创建对应的属性对象
		Properties props = Resources.getUrlAsProperties(url.toString());
		
		//对应的数据格式 {password=, url=jdbc:hsqldb:., driver=org.hsqldb.jdbcDriver, username=sa}
		System.err.println(props.toString());
		
		assertNotNull(props.getProperty("driver"));
	}

	/**
	 * 测试根据资源路径创建对应的属性对象
	 */
	@Test
	public void shouldGetResourceAsProperties() throws Exception {
		Properties props = Resources.getResourceAsProperties(CLASS_LOADER, JPETSTORE_PROPERTIES);
		
		//对应的数据格式 {password=, url=jdbc:hsqldb:., driver=org.hsqldb.jdbcDriver, username=sa}
		System.err.println(props.toString());
			
		assertNotNull(props.getProperty("driver"));
	}

	/**
	 * 测试根据对应的路径获取对应的资源定位对象 并测试通过对应的资源定位对象拿到对应的流对象
	 */
	@Test
	public void shouldGetUrlAsStream() throws Exception {
		URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
		InputStream in = Resources.getUrlAsStream(url.toString());
		assertNotNull(in);
		in.close();
	}

	/**
	 * 测试根据对应的路径获取对应的资源定位对象 并测试通过对应的资源定位对象拿到对应的流对象
	 */
	@Test
	public void shouldGetUrlAsReader() throws Exception {
		URL url = Resources.getResourceURL(CLASS_LOADER, JPETSTORE_PROPERTIES);
		Reader in = Resources.getUrlAsReader(url.toString());
		assertNotNull(in);
		in.close();
	}

	/**
	 * 测试根据提供的类加载和资源路径来获取对应的字节流对象
	 */
	@Test
	public void shouldGetResourceAsStream() throws Exception {
		InputStream in = Resources.getResourceAsStream(CLASS_LOADER, JPETSTORE_PROPERTIES);
		assertNotNull(in);
		in.close();
	}

	/**
	 * 测试根据提供的类加载和资源路径来获取对应的字符流对象
	 */
	@Test
	public void shouldGetResourceAsReader() throws Exception {
		Reader in = Resources.getResourceAsReader(CLASS_LOADER, JPETSTORE_PROPERTIES);
		assertNotNull(in);
		in.close();
	}

	/**
	 * 测试根据提供的默认类加载和资源路径来获取对应的文件对象
	 */
	@Test
	public void shouldGetResourceAsFile() throws Exception {
		File file = Resources.getResourceAsFile(JPETSTORE_PROPERTIES);
		
		System.err.println("文件名称:"+file.getName());
		System.err.println("文件绝对路径:"+file.getAbsolutePath());
		System.err.println("文件路径:"+file.getPath());
		System.err.println("文件大小:"+file.length());
		System.err.println("文件统一定位:"+file.toURI().toString());
		
		assertTrue(file.getAbsolutePath().replace('\\', '/').endsWith("jpetstore/jpetstore-hsqldb.properties"));
	}

	/**
	 * 测试根据提供的指定类加载和资源路径来获取对应的文件对象
	 */
	@Test
	public void shouldGetResourceAsFileWithClassloader() throws Exception {
		File file = Resources.getResourceAsFile(CLASS_LOADER, JPETSTORE_PROPERTIES);
		assertTrue(file.getAbsolutePath().replace('\\', '/').endsWith("jpetstore/jpetstore-hsqldb.properties"));
	}

	/**
	 * 测试根据提供的默认类加载和资源路径来获取对应的属性对象
	 */
	@Test
	public void shouldGetResourceAsPropertiesWithOutClassloader() throws Exception {
		Properties file = Resources.getResourceAsProperties(JPETSTORE_PROPERTIES);
		assertNotNull(file);
	}

	/**
	 * 测试根据提供的指定类加载和资源路径来获取对应的属性对象
	 */
	@Test
	public void shouldGetResourceAsPropertiesWithClassloader() throws Exception {
		Properties file = Resources.getResourceAsProperties(CLASS_LOADER, JPETSTORE_PROPERTIES);
		assertNotNull(file);
	}

	/**
	 * 测试给对应的资源加载类设置默认的类加载器
	 */
	@Test
	public void shouldAllowDefaultClassLoaderToBeSet() {
		Resources.setDefaultClassLoader(this.getClass().getClassLoader());
		
		//sun.misc.Launcher$AppClassLoader@2ff4acd0   应用类加载器
		System.err.println("对应的类加载为:"+Resources.getDefaultClassLoader().toString());
		System.err.println("对应的类加载为:"+this.getClass().getClassLoader().toString());
		
		assertEquals(this.getClass().getClassLoader(), Resources.getDefaultClassLoader());
	}

	/**
	 * 测试设置资源加载类的默认字符编码方式
	 */
	@Test
	public void shouldAllowDefaultCharsetToBeSet() {
		Resources.setCharset(Charset.defaultCharset());
		
		//测试发现默认的字符编码方式为UTF-8
		System.err.println(Charset.defaultCharset().toString());
		
		assertEquals(Charset.defaultCharset(), Resources.getCharset());
	}

	/**
	 * 测试根据提供的类名加载对应的类对象
	 */
	@Test
	public void shouldGetClassForName() throws Exception {
		Class<?> clazz = Resources.classForName(ResourcesTest.class.getName());
		
		//org.apache.ibatis.io.ResourcesTest
		System.err.println(ResourcesTest.class.getName());
		
		assertNotNull(clazz);
	}

	/**
	 * 测试加载不存在的类----->此处学习到一个在方法上设置异常的方式
	 */
	@Test(expected = ClassNotFoundException.class)
	public void shouldNotFindThisClass() throws ClassNotFoundException {
		Resources.classForName("some.random.class.that.does.not.Exist");
	}

	/**
	 * 测试设置资源加载的编码方式
	 * @throws IOException
	 */
	@Test
	public void shouldGetReader() throws IOException {
		//保存当前的字符编码方式
		Charset charset = Resources.getCharset();
		//设置新的字符编码方式
		Resources.setCharset(Charset.forName("US-ASCII"));
		assertNotNull(Resources.getResourceAsReader(JPETSTORE_PROPERTIES));
		//设置使用默认的字符编码方式
		Resources.setCharset(null);
		assertNotNull(Resources.getResourceAsReader(JPETSTORE_PROPERTIES));
		//恢复原先的字符编码方式
		Resources.setCharset(charset);
	}

	/**
	 * 测试使用指定的类加载进行资源加载处理
	 */
	@Test
	public void shouldGetReaderWithClassLoader() throws IOException {
		Charset charset = Resources.getCharset();
		Resources.setCharset(Charset.forName("US-ASCII"));
		assertNotNull(Resources.getResourceAsReader(getClass().getClassLoader(), JPETSTORE_PROPERTIES));
		Resources.setCharset(null);
		assertNotNull(Resources.getResourceAsReader(getClass().getClassLoader(), JPETSTORE_PROPERTIES));
		Resources.setCharset(charset);
		
		//sun.misc.Launcher$AppClassLoader@2ff4acd0
		System.err.println(getClass().getClassLoader().toString());
	}

	@Test
	public void stupidJustForCoverage() {
		assertNotNull(new Resources());
	}
}
