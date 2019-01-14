package org.apache.ibatis.submitted.raw_sql_source;

import java.io.Reader;
import java.sql.Connection;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class RawSqlSourceTest {

	private static SqlSessionFactory sqlSessionFactory;

	@BeforeClass
	public static void setUp() throws Exception {
		//create an SqlSessionFactory
		Reader reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/raw_sql_source/mybatis-config.xml");
		sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
		reader.close();

		//populate in-memory database
		SqlSession session = sqlSessionFactory.openSession();
		Connection conn = session.getConnection();
		reader = Resources.getResourceAsReader("org/apache/ibatis/submitted/raw_sql_source/CreateDB.sql");
		ScriptRunner runner = new ScriptRunner(conn);
		runner.setLogWriter(null);
		runner.runScript(reader);
		conn.close();
		reader.close();
		session.close();
	}

	@Test
	public void shouldUseRawSqlSourceForAnStaticStatement() {
		test("getUser1", RawSqlSource.class);
	}

	@Test
	public void shouldUseDynamicSqlSourceForAnStatementWithInlineArguments() {
		test("getUser2", DynamicSqlSource.class);
	}

	@Test
	public void shouldUseDynamicSqlSourceForAnStatementWithXmlTags() {
		test("getUser3", DynamicSqlSource.class);
	}

	private void test(String statement, Class<? extends SqlSource> sqlSource) {
		SqlSession sqlSession = sqlSessionFactory.openSession();
		try {
			Assert.assertEquals(sqlSource, sqlSession.getConfiguration().getMappedStatement(statement).getSqlSource().getClass());
			String sql = sqlSession.getConfiguration().getMappedStatement(statement).getSqlSource().getBoundSql('?').getSql();
			Assert.assertEquals("select * from users where id = ?", sql);
			User user = sqlSession.selectOne(statement, 1);
			Assert.assertEquals("User1", user.getName());
		} finally {
			sqlSession.close();
		}
	}

}
