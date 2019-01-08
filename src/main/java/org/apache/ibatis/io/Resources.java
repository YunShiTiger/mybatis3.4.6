package org.apache.ibatis.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * 通过对应的类加载器来简化对指定资源的加载操作处理
 */
public class Resources {

	private static ClassLoaderWrapper classLoaderWrapper = new ClassLoaderWrapper();

	//记录创建对应字符流对象时对应的字符编码方式
	private static Charset charset;

	Resources() {
		
	}

	/*
	 * 获取默认的类加载器对象
	 */
	public static ClassLoader getDefaultClassLoader() {
		return classLoaderWrapper.defaultClassLoader;
	}

	/*
	 * 设置默认的类加载器对象
	 */
	public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
		classLoaderWrapper.defaultClassLoader = defaultClassLoader;
	}

	/*
	 * 根据提供的资源路径和默认的类加载创建对应的资源统一定位对象
	 */
	public static URL getResourceURL(String resource) throws IOException {
		return getResourceURL(null, resource);
	}

	/*
	 * 根据提供的资源路径和指定的类加载创建对应的资源统一定位对象
	 */
	public static URL getResourceURL(ClassLoader loader, String resource) throws IOException {
		//通过对应的类加载器来加载并创建对应的资源统一定位对象
		URL url = classLoaderWrapper.getResourceAsURL(resource, loader);
		if (url == null) {
			throw new IOException("Could not find resource " + resource);
		}
		//返回资源的统一定位对象
		return url;
	}

	/*
	 * 根据给定的资源路径和默认类加载来创建对应的流对象
	 *  注意:
	 *  如果对应的资源不存在或者不可读的情况下会触发抛出对应的异常信息
	 */
	public static InputStream getResourceAsStream(String resource) throws IOException {
		return getResourceAsStream(null, resource);
	}

	/*
	 * 根据给定的资源路径和指定类加载来创建对应的流对象
	 *  注意:
	 *  如果对应的资源不存在或者不可读的情况下会触发抛出对应的异常信息
	 */
	public static InputStream getResourceAsStream(ClassLoader loader, String resource) throws IOException {
		//通过提供的类加载器来加载指定的资源并创建对应的流对象
		InputStream in = classLoaderWrapper.getResourceAsStream(resource, loader);
		//检测是否获取对应的流对象
		if (in == null) {
			//抛出对应的没有找到对应资源的异常
			throw new IOException("Could not find resource " + resource);
		}
		//返回对应的流对象
		return in;
	}

	/*
	 * 根据给定的资源路径和默认类加载来创建对应的属性对象
	 *  注意:
	 *  如果对应的资源不存在或者不可读的情况下会触发抛出对应的异常信息
	 * 此处代码实现有点啰嗦  内部其实可以直接调用下一个方法进行实现
	 */
	public static Properties getResourceAsProperties(String resource) throws IOException {
		//创建对应的属性对象
		Properties props = new Properties();
		//根据提供的路径创建对应的流对象
		InputStream in = getResourceAsStream(resource);
		//通过流对象中的内容来设置对应的属性对象
		props.load(in);
		//关闭对应的流对象
		in.close();
		//返回对应的属性对象
		return props;
	}

	/*
	 * 根据给定的资源路径和指定类加载来创建对应的属性对象
	 *  注意:
	 *  如果对应的资源不存在或者不可读的情况下会触发抛出对应的异常信息
	 */
	public static Properties getResourceAsProperties(ClassLoader loader, String resource) throws IOException {
		//创建对应的属性对象
		Properties props = new Properties();
		//根据提供的路径创建对应的流对象
		InputStream in = getResourceAsStream(loader, resource);
		//通过流对象中的内容来设置对应的属性对象
		props.load(in);
		//关闭对应的流对象
		in.close();
		//返回对应的属性对象
		return props;
	}

	/*
	 * 根据提供的资源路径和默认的类加载获取对应的字符流对象
	 */
	public static Reader getResourceAsReader(String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(resource), charset);
		}
		return reader;
	}

	/*
	 * 根据提供的资源路径和指定的类加载获取对应的字符流对象
	 */
	public static Reader getResourceAsReader(ClassLoader loader, String resource) throws IOException {
		Reader reader;
		if (charset == null) {
			reader = new InputStreamReader(getResourceAsStream(loader, resource));
		} else {
			reader = new InputStreamReader(getResourceAsStream(loader, resource), charset);
		}
		return reader;
	}

	/*
	 * 根据提供的资源路径和默认类加载来创建对应的文件对象
	 */
	public static File getResourceAsFile(String resource) throws IOException {
		//首先根据给定的资源路径获取对应的资源定位对象,然后根据资源定位对象获取对应的文件对象----------->这个地方一定要注意拿取的方式
		return new File(getResourceURL(resource).getFile());
	}

	/*
	 * 根据提供的资源路径和指定类加载来创建对应的文件对象
	 */
	public static File getResourceAsFile(ClassLoader loader, String resource) throws IOException {
		return new File(getResourceURL(loader, resource).getFile());
	}

	/*
	 * 根据提供的资源统一定位对象对应的资源路径获取对应的字节流对象
	 */
	public static InputStream getUrlAsStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		//拿到资源定位对象对应的连接对象
		URLConnection conn = url.openConnection();
		//根据连接获取对应的流对象
		return conn.getInputStream();
	}

	/*
	 * 根据提供的资源统一定位对象对应的资源路径获取对应的字符流对象
	 */
	public static Reader getUrlAsReader(String urlString) throws IOException {
		Reader reader;
		//检测是否设置了字符对应的编码方式
		if (charset == null) {
			//使用默认的编码方式获取对应的字符流对象
			reader = new InputStreamReader(getUrlAsStream(urlString));
		} else {
			//使用指定的编码方式获取对应的字符流对象
			reader = new InputStreamReader(getUrlAsStream(urlString), charset);
		}
		//返回对应的流对象
		return reader;
	}

	/*
	 * 根据提供的统一资源定位对象对应的资源路径来创建对应的属性对象
	 */
	public static Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		InputStream in = getUrlAsStream(urlString);
		props.load(in);
		in.close();
		return props;
	}

	/*
	 * 根据提供的类名来加载对应的类对象
	 */
	public static Class<?> classForName(String className) throws ClassNotFoundException {
		return classLoaderWrapper.classForName(className);
	}

	public static Charset getCharset() {
		return charset;
	}

	public static void setCharset(Charset charset) {
		Resources.charset = charset;
	}

}
