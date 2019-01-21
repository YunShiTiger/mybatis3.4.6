package org.apache.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * 
 */
public interface Executor {

	ResultHandler NO_RESULT_HANDLER = null;

	//执行update insert delete三种类型的sql语句的接口方法
	int update(MappedStatement ms, Object parameter) throws SQLException;

	//执行select类型的sql语句，返回值分为结果对象列表或者游标对象
	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

	<E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

	<E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

	//执行批量sql语句
	List<BatchResult> flushStatements() throws SQLException;

	//提交事务
	void commit(boolean required) throws SQLException;

	//回滚事务
	void rollback(boolean required) throws SQLException;

	//创建缓存中用到的CacheKey对象
	CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql);

	//根据CacheKey对象查找缓存
	boolean isCached(MappedStatement ms, CacheKey key);

	//清除一级缓存
	void clearLocalCache();

	//延迟加载一级缓存中的数据
	void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType);

	//获取事务对象
	Transaction getTransaction();

	//关闭Executor对象
	void close(boolean forceRollback);

	//检测Executor是否已关闭
	boolean isClosed();

	void setExecutorWrapper(Executor executor);

}
