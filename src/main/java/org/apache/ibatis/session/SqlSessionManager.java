package org.apache.ibatis.session;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * SqlSession的管理器处理类
 */
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

	//用于记录对应的SqlSessionFactory对象
	private final SqlSessionFactory sqlSessionFactory;
	//用于记录对应的SqlSession代理对象---------->此处使用了jdk提供的动态代理方式
	private final SqlSession sqlSessionProxy;

	private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<SqlSession>();

	/**
	 * 根据提供的SqlSessionFactory来创建对应的SqlSessionManager对象------->创建SqlSessionManager对象的唯一构造方法
	 * 需要注意此处使用了private进行修饰构造方式: 目的是方式在外部直接进行创建此对象
	 */
	private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
		this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(SqlSessionFactory.class.getClassLoader(), new Class[] { SqlSession.class }, new SqlSessionInterceptor());
	}

	/**
	 * 通过方法重载的方式对外提供多种方式来构建SqlSessionManager对象
	 * 
	 * 配置数据源可以分成三种形式
	 * 1。Reader流方式
	 * 2。InputStream流方式
	 * 3。SqlSessionFactory对象方式
	 */
	public static SqlSessionManager newInstance(Reader reader) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
	}

	public static SqlSessionManager newInstance(Reader reader, String environment) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
	}

	public static SqlSessionManager newInstance(Reader reader, Properties properties) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
	}

	public static SqlSessionManager newInstance(InputStream inputStream) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
	}

	public static SqlSessionManager newInstance(InputStream inputStream, String environment) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
	}

	public static SqlSessionManager newInstance(InputStream inputStream, Properties properties) {
		return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
	}

	public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
		return new SqlSessionManager(sqlSessionFactory);
	}

	/**
	 * 通过方法重载的方式给当前的线程对象设置对应的SqlSession对象
	 */
	public void startManagedSession() {
		this.localSqlSession.set(openSession());
	}

	public void startManagedSession(boolean autoCommit) {
		this.localSqlSession.set(openSession(autoCommit));
	}

	public void startManagedSession(Connection connection) {
		this.localSqlSession.set(openSession(connection));
	}

	public void startManagedSession(TransactionIsolationLevel level) {
		this.localSqlSession.set(openSession(level));
	}

	public void startManagedSession(ExecutorType execType) {
		this.localSqlSession.set(openSession(execType));
	}

	public void startManagedSession(ExecutorType execType, boolean autoCommit) {
		this.localSqlSession.set(openSession(execType, autoCommit));
	}

	public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
		this.localSqlSession.set(openSession(execType, level));
	}

	public void startManagedSession(ExecutorType execType, Connection connection) {
		this.localSqlSession.set(openSession(execType, connection));
	}

	/**
	 * 用于检测当前线程中是否配置了SqlSession对象
	 */
	public boolean isManagedSessionStarted() {
		return this.localSqlSession.get() != null;
	}

	/**
	 * 通过方法重载的方式通过对应的SqlSessionFactory工厂来创建对应的SqlSession对象
	 */
	@Override
	public SqlSession openSession() {
		return sqlSessionFactory.openSession();
	}

	@Override
	public SqlSession openSession(boolean autoCommit) {
		return sqlSessionFactory.openSession(autoCommit);
	}

	@Override
	public SqlSession openSession(Connection connection) {
		return sqlSessionFactory.openSession(connection);
	}

	@Override
	public SqlSession openSession(TransactionIsolationLevel level) {
		return sqlSessionFactory.openSession(level);
	}

	@Override
	public SqlSession openSession(ExecutorType execType) {
		return sqlSessionFactory.openSession(execType);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return sqlSessionFactory.openSession(execType, autoCommit);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return sqlSessionFactory.openSession(execType, level);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return sqlSessionFactory.openSession(execType, connection);
	}

	/**
	 * 提供对应的获取当前Mybatis中配置的相关数据信息
	 */
	@Override
	public Configuration getConfiguration() {
		return sqlSessionFactory.getConfiguration();
	}

	@Override
	public <T> T selectOne(String statement) {
		return sqlSessionProxy.<T>selectOne(statement);
	}

	@Override
	public <T> T selectOne(String statement, Object parameter) {
		return sqlSessionProxy.<T>selectOne(statement, parameter);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return sqlSessionProxy.<K, V>selectMap(statement, mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
		return sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey, rowBounds);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement) {
		return sqlSessionProxy.selectCursor(statement);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter) {
		return sqlSessionProxy.selectCursor(statement, parameter);
	}

	@Override
	public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
		return sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
	}

	@Override
	public <E> List<E> selectList(String statement) {
		return sqlSessionProxy.<E>selectList(statement);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter) {
		return sqlSessionProxy.<E>selectList(statement, parameter);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		return sqlSessionProxy.<E>selectList(statement, parameter, rowBounds);
	}

	@Override
	public void select(String statement, ResultHandler handler) {
		sqlSessionProxy.select(statement, handler);
	}

	@Override
	public void select(String statement, Object parameter, ResultHandler handler) {
		sqlSessionProxy.select(statement, parameter, handler);
	}

	@Override
	public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
		sqlSessionProxy.select(statement, parameter, rowBounds, handler);
	}

	@Override
	public int insert(String statement) {
		return sqlSessionProxy.insert(statement);
	}

	@Override
	public int insert(String statement, Object parameter) {
		return sqlSessionProxy.insert(statement, parameter);
	}

	@Override
	public int update(String statement) {
		return sqlSessionProxy.update(statement);
	}

	@Override
	public int update(String statement, Object parameter) {
		return sqlSessionProxy.update(statement, parameter);
	}

	@Override
	public int delete(String statement) {
		return sqlSessionProxy.delete(statement);
	}

	@Override
	public int delete(String statement, Object parameter) {
		return sqlSessionProxy.delete(statement, parameter);
	}

	@Override
	public <T> T getMapper(Class<T> type) {
		return getConfiguration().getMapper(type, this);
	}

	@Override
	public Connection getConnection() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
		}
		return sqlSession.getConnection();
	}

	@Override
	public void clearCache() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
		}
		sqlSession.clearCache();
	}

	@Override
	public void commit() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
		}
		sqlSession.commit();
	}

	@Override
	public void commit(boolean force) {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
		}
		sqlSession.commit(force);
	}

	@Override
	public void rollback() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		sqlSession.rollback();
	}

	@Override
	public void rollback(boolean force) {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		sqlSession.rollback(force);
	}

	@Override
	public List<BatchResult> flushStatements() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
		}
		return sqlSession.flushStatements();
	}

	@Override
	public void close() {
		final SqlSession sqlSession = localSqlSession.get();
		if (sqlSession == null) {
			throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
		}
		try {
			sqlSession.close();
		} finally {
			localSqlSession.set(null);
		}
	}

	private class SqlSessionInterceptor implements InvocationHandler {
		
		public SqlSessionInterceptor() {
			// Prevent Synthetic Access
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
			if (sqlSession != null) {
				try {
					return method.invoke(sqlSession, args);
				} catch (Throwable t) {
					throw ExceptionUtil.unwrapThrowable(t);
				}
			} else {
				final SqlSession autoSqlSession = openSession();
				try {
					final Object result = method.invoke(autoSqlSession, args);
					autoSqlSession.commit();
					return result;
				} catch (Throwable t) {
					autoSqlSession.rollback();
					throw ExceptionUtil.unwrapThrowable(t);
				} finally {
					autoSqlSession.close();
				}
			}
		}
	}

}
