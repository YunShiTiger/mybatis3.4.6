package org.apache.ibatis.session.defaults;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;

/**
 * 默认的SqlSessionFactory工厂处理类
 * 本类的主要功能是根据配置信息类和提供的相关参数来构建对应的与数据库的回话对象SqlSession
 */
public class DefaultSqlSessionFactory implements SqlSessionFactory {

	//用于记录核心的配置信息类
	private final Configuration configuration;

	public DefaultSqlSessionFactory(Configuration configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public SqlSession openSession() {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
	}

	@Override
	public SqlSession openSession(boolean autoCommit) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, autoCommit);
	}

	@Override
	public SqlSession openSession(ExecutorType execType) {
		return openSessionFromDataSource(execType, null, false);
	}

	@Override
	public SqlSession openSession(TransactionIsolationLevel level) {
		return openSessionFromDataSource(configuration.getDefaultExecutorType(), level, false);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
		return openSessionFromDataSource(execType, level, false);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
		return openSessionFromDataSource(execType, null, autoCommit);
	}

	private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
		Transaction tx = null;
		try {
			final Environment environment = configuration.getEnvironment();
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
			final Executor executor = configuration.newExecutor(tx, execType);
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			closeTransaction(tx); //may have fetched a connection so lets call close()
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}
	
	@Override
	public SqlSession openSession(Connection connection) {
		return openSessionFromConnection(configuration.getDefaultExecutorType(), connection);
	}

	@Override
	public SqlSession openSession(ExecutorType execType, Connection connection) {
		return openSessionFromConnection(execType, connection);
	}

	/*
	 * 根据提供的执行器类型和连接对象创建对应的SqlSession回话对象
	 */
	private SqlSession openSessionFromConnection(ExecutorType execType, Connection connection) {
		try {
			boolean autoCommit;
			try {
				//获取是否是自动进行提交标识
				autoCommit = connection.getAutoCommit();
			} catch (SQLException e) {
				// Failover to true, as most poor drivers or databases won't support transactions
				autoCommit = true;
			}
			//获取当前的数据库运行环境对象
			final Environment environment = configuration.getEnvironment();
			//获取当前数据库运行环境对应的事物管理器工厂对象
			final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
			//获取对应的事物管理器对象
			final Transaction tx = transactionFactory.newTransaction(connection);
			//根据执行器类型和事物管理器来创建对应的执行器对象-------------->用于执行对应sql语句的执行器对象
			final Executor executor = configuration.newExecutor(tx, execType);
			//构建回话对象
			return new DefaultSqlSession(configuration, executor, autoCommit);
		} catch (Exception e) {
			throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
		} finally {
			ErrorContext.instance().reset();
		}
	}

	private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
		if (environment == null || environment.getTransactionFactory() == null) {
			return new ManagedTransactionFactory();
		}
		return environment.getTransactionFactory();
	}

	private void closeTransaction(Transaction tx) {
		if (tx != null) {
			try {
				tx.close();
			} catch (SQLException ignore) {
				// Intentionally ignore. Prefer previous error.
			}
		}
	}

}
