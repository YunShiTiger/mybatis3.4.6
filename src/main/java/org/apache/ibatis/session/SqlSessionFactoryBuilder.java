package org.apache.ibatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;

/**
 * 构建SqlSessionFactory的构建器处理类  
 * 通过分析可以发现: 内部有三种方式来进行创建对应的SqlSessionFactory对象
 *     1.通过对应的字符流对象进行创建
 *     2.通过对应的字节流对象进行创建
 *     3.直接通过生成配置对象来进行构建
 */
public class SqlSessionFactoryBuilder {

	/**
	 * 通过多种字符流对象的方式来配置SqlSessionFactory的处理
	 */
	public SqlSessionFactory build(Reader reader) {
		return build(reader, null, null);
	}

	public SqlSessionFactory build(Reader reader, String environment) {
		return build(reader, environment, null);
	}

	public SqlSessionFactory build(Reader reader, Properties properties) {
		return build(reader, null, properties);
	}

	//使用字符流方式构建对象最终要走的地方
	public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
		try {
			//通过传入的字符流对象 环境参数 属性对象 来构建配置Xml的解析器对象
			XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
			//进行解析对应的xml文件并构建对应的SqlSessionFactory对象----------------------------------------->核心操作   解析对应的xml配置文件的处理
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				//关闭对应的流对象
				reader.close();
			} catch (IOException e) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

	/**
	 * 通过多种字节流对象的方式来配置SqlSessionFactory的处理
	 */
	public SqlSessionFactory build(InputStream inputStream) {
		return build(inputStream, null, null);
	}

	public SqlSessionFactory build(InputStream inputStream, String environment) {
		return build(inputStream, environment, null);
	}

	public SqlSessionFactory build(InputStream inputStream, Properties properties) {
		return build(inputStream, null, properties);
	}

	//使用字节流方式构建对象最终要走的地方
	public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
		try {
			//通过传入的字节流对象 环境参数 属性对象 来构建配置Xml的解析器对象
			XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
			//进行解析对应的xml文件并构建对应的SqlSessionFactory对象----------------------------------------->核心操作   解析对应的xml配置文件的处理
			return build(parser.parse());
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error building SqlSession.", e);
		} finally {
			ErrorContext.instance().reset();
			try {
				//关闭对应的流对象
				inputStream.close();
			} catch (IOException e) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

	/**
	 * 通过直接传入配置类对象的方式来配置SqlSessionFactory的处理
	 */
	//此处是构建SqlSessionFactory对象必须要走的地方
	public SqlSessionFactory build(Configuration config) {
		return new DefaultSqlSessionFactory(config);
	}

}
