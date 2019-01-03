package org.apache.ibatis.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Reader;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.domain.blog.Author;
import org.apache.ibatis.domain.blog.mappers.AuthorMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 对应SqlSessionManager类的相关测试用例
 */
public class SqlSessionManagerTest extends BaseDataTest {

	//对外提供的SqlSessionManager管理对象
	private static SqlSessionManager manager;

	//触发对应测试方法前需要的初始化操作处理
	@BeforeClass
	public static void setup() throws Exception {
		//创建对应的测试用户需要使用的博客数据库和对应的数据
		createBlogDataSource();
		//对应的测试配置文件路径
		final String resource = "org/apache/ibatis/builder/MapperConfig.xml";
		//加载对应的资源文件并转换成对应的流对象
		final Reader reader = Resources.getResourceAsReader(resource);
		//根据提供的流对象来解析并创建对应的SqlSessionManager对象
		manager = SqlSessionManager.newInstance(reader);
	}

	@Test
	public void shouldThrowExceptionIfMappedStatementDoesNotExistAndSqlSessionIsOpen() throws Exception {
		try {
			manager.startManagedSession();
			manager.selectList("ThisStatementDoesNotExist");
			fail("Expected exception to be thrown due to statement that does not exist.");
		} catch (PersistenceException e) {
			assertTrue(e.getMessage().contains("does not contain value for ThisStatementDoesNotExist"));
		} finally {
			manager.close();
		}
	}

	@Test
	public void shouldCommitInsertedAuthor() throws Exception {
		try {
			manager.startManagedSession();
			AuthorMapper mapper = manager.getMapper(AuthorMapper.class);
			Author expected = new Author(500, "cbegin", "******", "cbegin@somewhere.com", "Something...", null);
			mapper.insertAuthor(expected);
			manager.commit();
			Author actual = mapper.selectAuthor(500);
			assertNotNull(actual);
		} finally {
			manager.close();
		}
	}

	@Test
	public void shouldRollbackInsertedAuthor() throws Exception {
		try {
			manager.startManagedSession();
			AuthorMapper mapper = manager.getMapper(AuthorMapper.class);
			Author expected = new Author(501, "lmeadors", "******", "lmeadors@somewhere.com", "Something...", null);
			mapper.insertAuthor(expected);
			manager.rollback();
			Author actual = mapper.selectAuthor(501);
			assertNull(actual);
		} finally {
			manager.close();
		}
	}

	@Test
	public void shouldImplicitlyRollbackInsertedAuthor() throws Exception {
		manager.startManagedSession();
		AuthorMapper mapper = manager.getMapper(AuthorMapper.class);
		Author expected = new Author(502, "emacarron", "******", "emacarron@somewhere.com", "Something...", null);
		mapper.insertAuthor(expected);
		manager.close();
		Author actual = mapper.selectAuthor(502);
		assertNull(actual);
	}

}
