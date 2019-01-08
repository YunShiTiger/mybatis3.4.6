package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * 创建于数据库进行回话的SqlSession对象的接口
 *   可以根据对应的数据源或者连接来创建对应的SqlSession对象
 */
public interface SqlSessionFactory {

	SqlSession openSession();

	SqlSession openSession(boolean autoCommit);

	SqlSession openSession(Connection connection);

	SqlSession openSession(TransactionIsolationLevel level);

	SqlSession openSession(ExecutorType execType);

	SqlSession openSession(ExecutorType execType, boolean autoCommit);

	SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);

	SqlSession openSession(ExecutorType execType, Connection connection);

	Configuration getConfiguration();

}
