package org.apache.ibatis.io;

import java.io.InputStream;
import java.net.URL;

/**
 * 封装了可以使用配置的多个类加载器进行加载资源和类对象的处理类
 * 这样自己可以灵活配置类加载器来进行资源加载操作处理
 * 通过本类也可以看到类加载器的一般作用
 *    1.进行对指定类的加载操作处理
 *    2.通过给定的路径获取对应的统一资源定位对象
 *    3.通过给定的路径获取对应的流对象
 */
public class ClassLoaderWrapper {

	//记录默认类加载器
	ClassLoader defaultClassLoader;
	//记录应用类加载器
	ClassLoader systemClassLoader;

	ClassLoaderWrapper() {
		try {
			//初始化对应的应用类加载器对象
			systemClassLoader = ClassLoader.getSystemClassLoader();
		} catch (SecurityException ignored) {
			// AccessControlException on Google App Engine
		}
	}

	/*
	 * 根据提供的资源路径和默认类加载器数组去加载对应的统一资源定位对象
	 */
	public URL getResourceAsURL(String resource) {
		return getResourceAsURL(resource, getClassLoaders(null));
	}

	/*
	 * 根据提供的资源路径和指定类加载器数组去加载对应的统一资源定位对象
	 */
	public URL getResourceAsURL(String resource, ClassLoader classLoader) {
		return getResourceAsURL(resource, getClassLoaders(classLoader));
	}

	/*
	 * 根据提供的资源路径和默认类加载器数组去加载对应的字节流对象
	 */
	public InputStream getResourceAsStream(String resource) {
		return getResourceAsStream(resource, getClassLoaders(null));
	}

	/*
	 * 根据提供的资源路径和指定类加载器数组去加载对应的字节流对象
	 */
	public InputStream getResourceAsStream(String resource, ClassLoader classLoader) {
		return getResourceAsStream(resource, getClassLoaders(classLoader));
	}

	/*
	 * 根据提供的类名和默认类加载器数组去加载对应的类对象
	 */
	public Class<?> classForName(String name) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(null));
	}

	/*
	 * 根据提供的类名和指定类加载器数组去加载对应的类对象
	 */
	public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
		return classForName(name, getClassLoaders(classLoader));
	}

	/*
	 * 根据提供的资源路径和类加载器数组来创建对应的流对象
	 */
	InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
		//循环配置的所有类加载器对象
		for (ClassLoader cl : classLoader) {
			//检测对应的类加载器对象是否存在
			if (null != cl) {
				//尝试使用本类加载器获取对应的流对象
				InputStream returnValue = cl.getResourceAsStream(resource);
				//检测是否获取对应的流对象,在没有找到的情况下 尝试通过加/来进行再次尝试
				if (null == returnValue) {
					returnValue = cl.getResourceAsStream("/" + resource);
				}
				//检测是否找到对应的流对象
				if (null != returnValue) {
					//返回根据本加载器找到的流对象
					return returnValue;
				}
			}
		}
		//尝试所有的类加载都不成功的情况下,返回对应的空对象
		return null;
	}

	/*
	 * 根据提供的资源路径和类加载器数组来创建对应的统一资源定位对象
	 */
	URL getResourceAsURL(String resource, ClassLoader[] classLoader) {
		URL url;
		//循环配置的所有类加载器对象
		for (ClassLoader cl : classLoader) {
			//检测对应的类加载器对象是否存在
			if (null != cl) {
				//尝试使用本类加载器获取对应的统一资源定位对象
				url = cl.getResource(resource);
				//检测是否获取对应的统一资源定位对象,在没有找到的情况下 尝试通过加/来进行再次尝试
				if (null == url) {
					url = cl.getResource("/" + resource);
				}
				//检测是否找到对应的统一资源定位对象
				if (null != url) {
					//返回根据本加载器找到的统一资源定位对象
					return url;
				}
			}
		}
		//尝试所有的类加载都不成功的情况下,返回对应的空对象
		return null;
	}

	/*
	 * 尝试使用提供的一个类加载数组来加载对应的类对象
	 */
	Class<?> classForName(String name, ClassLoader[] classLoader) throws ClassNotFoundException {
		//尝试使用配置的类加载器数组中的加载器对象去加载对应的类
		for (ClassLoader cl : classLoader) {
			//检测对应的类加载器是否存在
			if (null != cl) {
				try {
					//尝试使用对应的类加载器进行类加载处理
					Class<?> c = Class.forName(name, true, cl);
					//检测是否加载到对应的类对象
					if (null != c) {
						//返回对应的类对象
						return c;
					}
				} catch (ClassNotFoundException e) {
					//如果当前类加载器没有加载到此对应的类 将会进入此异常 此处是进行忽律对应的异常
				}
			}
		}
		//当所有的类加载器都没有加载到对应的类时,此处才会抛出对应的异常
		throw new ClassNotFoundException("Cannot find class: " + name);
	}

	//配置的几种不同方式的类加载对象  顺序是  自己指定的 自己设置的默认值 线程对象中 等顺序
	ClassLoader[] getClassLoaders(ClassLoader classLoader) {
		//注意此处每次都会创建一个对应的类加载数组--------------->这个地方效率感觉不高,每次都需要创建数组,处理完后需要回收此处的空间
		return new ClassLoader[] { classLoader, defaultClassLoader, Thread.currentThread().getContextClassLoader(), getClass().getClassLoader(), systemClassLoader };
	}

}
