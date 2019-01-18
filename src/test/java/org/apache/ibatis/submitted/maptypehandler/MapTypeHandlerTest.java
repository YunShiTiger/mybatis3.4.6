package org.apache.ibatis.submitted.maptypehandler;

import java.io.Reader;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class MapTypeHandlerTest {

	//用于记录对应的SqlSessionFactory工厂对象
	private static SqlSessionFactory sqlSessionFactory;

	/*
	 * 执行测试用例对应的初始化操作处理
	 * 1.根据xml配置参数获取对应的SqlSessionFactory工厂对象
	 * 2根据xml配置参数初始化相关的配置信息
	 * 3初始化对应的数据库数据
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		//创建对应的配置xml数据源对象
		Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/maptypehandler/mybatis-config.xml");
		//根据xml配置对象构建对应的sqlSessionFactory工厂对象
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
		//关闭对应的配置源对象
		reader.close();

		//下面进行加载对应的数据库脚本进行执行对应脚本的处理----->注意是在内存级别的数据库操作处理
		//根据sqlSessionFactory工厂获取一个对应的SqlSession回话对象
		SqlSession session = sqlSessionFactory.openSession();
		//根据回话对象获取对应数据库的连接
		Connection conn = session.getConnection();
		//加载对应的需要执行的数据库sql语句脚本
		reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/maptypehandler/CreateDB.sql");
		//创建对应的脚本执行器对象
		ScriptRunner runner = new ScriptRunner(conn);
		//配置执行器的日志信息
		runner.setLogWriter(null);
		//运行对应的数据库脚本
		runner.runScript(reader);
		//关闭连接对象
		conn.close();
		//关闭对应的流对象
		reader.close();
		//关闭回话对象
		session.close();
	}

	@Test
	public void shouldGetAUserFromAnnotation() {
		//根据SqlSessionFactory工厂获取对应的SqlSession对象
		SqlSession sqlSession = sqlSessionFactory.openSession();
		try {
			//根据提供的mapper类型获取对应的真实代理对象------------------->注意此处不是接口类型,而是接口对应的代理对象
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			//
			User user = mapper.getUser(1, "User1");
			//
			Assert.assertEquals("User1", user.getName());
		} finally {
			//关闭对应的回话对象
			sqlSession.close();
		}
	}

	@Test(expected = PersistenceException.class)
	public void shouldGetAUserFromXML() {
		SqlSession sqlSession = sqlSessionFactory.openSession();
		try {
			Mapper mapper = sqlSession.getMapper(Mapper.class);
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("id", 1);
			params.put("name", "User1");
			User user = mapper.getUserXML(params);
			Assert.assertEquals("User1", user.getName());
		} finally {
			sqlSession.close();
		}
	}

}
