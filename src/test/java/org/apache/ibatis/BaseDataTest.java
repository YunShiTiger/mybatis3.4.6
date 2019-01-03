package org.apache.ibatis;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 测试使用的基础数据源配置处理类
 */
public abstract class BaseDataTest {

	//连接博客数据库的配置参数
	public static final String BLOG_PROPERTIES = "org/apache/ibatis/databases/blog/blog-derby.properties";
	//博客数据库对应的建表语句
	public static final String BLOG_DDL = "org/apache/ibatis/databases/blog/blog-derby-schema.sql";
	//博客数据库对应的插入数据语句
	public static final String BLOG_DATA = "org/apache/ibatis/databases/blog/blog-derby-dataload.sql";

	public static final String JPETSTORE_PROPERTIES = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb.properties";
	public static final String JPETSTORE_DDL = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-schema.sql";
	public static final String JPETSTORE_DATA = "org/apache/ibatis/databases/jpetstore/jpetstore-hsqldb-dataload.sql";

	/**
	 * 统一的根据提供的配置参数来创建对应无池化的数据源对象
	 */
	public static UnpooledDataSource createUnpooledDataSource(String resource) throws IOException {
		//根据提供的配置文件路径加载对应文件并生成对应的属性对象
		Properties props = Resources.getResourceAsProperties(resource);
		//创建一个不需要管理池对象的数据源对象
		UnpooledDataSource ds = new UnpooledDataSource();
		//配置数据源对应的相关参数
		ds.setDriver(props.getProperty("driver"));
		ds.setUrl(props.getProperty("url"));
		ds.setUsername(props.getProperty("username"));
		ds.setPassword(props.getProperty("password"));
		//返回对应的数据源对象
		return ds;
	}

	/**
	 * 统一的根据提供的配置参数来创建对应有池化的数据源对象
	 */
	public static PooledDataSource createPooledDataSource(String resource) throws IOException {
		Properties props = Resources.getResourceAsProperties(resource);
		PooledDataSource ds = new PooledDataSource();
		ds.setDriver(props.getProperty("driver"));
		ds.setUrl(props.getProperty("url"));
		ds.setUsername(props.getProperty("username"));
		ds.setPassword(props.getProperty("password"));
		return ds;
	}

	/**
	 * 根据提供的数据源和sql脚本来执行对应的sql脚本
	 */
	public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
		//根据对应的数据源拿到对应的连接对象
		Connection connection = ds.getConnection();
		try {
			//创建对应的脚本执行器对象
			ScriptRunner runner = new ScriptRunner(connection);
			//设置对应的脚本执行器的相关属性
			runner.setAutoCommit(true);
			runner.setStopOnError(false);
			runner.setLogWriter(null);
			runner.setErrorLogWriter(null);
			//执行对应的脚本
			runScript(runner, resource);
		} finally {
			//操作完成脚本后,关闭对应的数据库连接
			connection.close();
		}
	}

	/**
	 * 根据提供的脚本执行器和对应的脚本路径进行执行对应的脚本
	 */
	public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
		//根据提供的脚本路径加载对应的脚本并获取对应的流对象
		Reader reader = Resources.getResourceAsReader(resource);
		try {
			//通过脚本执行器执行对应的脚本
			runner.runScript(reader);
		} finally {
			//执行完成后关闭对应的流对象
			reader.close();
		}
	}

	/**
	 * 创建对应的博客数据库对应的数据源
	 */
	public static DataSource createBlogDataSource() throws IOException, SQLException {
		//根据配置参数获取对应的数据源对象
		DataSource ds = createUnpooledDataSource(BLOG_PROPERTIES);
		//执行对应的建表语句
		runScript(ds, BLOG_DDL);
		//执行对应的插入数据语句
		runScript(ds, BLOG_DATA);
		//返回对应的数据源对象
		return ds;
	}

	public static DataSource createJPetstoreDataSource() throws IOException, SQLException {
		DataSource ds = createUnpooledDataSource(JPETSTORE_PROPERTIES);
		runScript(ds, JPETSTORE_DDL);
		runScript(ds, JPETSTORE_DATA);
		return ds;
	}
}
