package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 此处使用了静态代理对象机制来隐藏内部真实的对象
 */
public class RoutingStatementHandler implements StatementHandler {

	//用于代表真实的StatementHandler对象
	private final StatementHandler delegate;

	/*
	 * 根据对应的类型创建对应的真实的StatementHandler对象
	 */
	public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		//根据类型创建对应的StatementHandler对象
		switch (ms.getStatementType()) {
			case STATEMENT:
				delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
				break;
			case PREPARED:
				delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
				break;
			case CALLABLE:
				delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
				break;
			default:
				throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
		}
	}

	@Override
	public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
		return delegate.prepare(connection, transactionTimeout);
	}

	@Override
	public void parameterize(Statement statement) throws SQLException {
		delegate.parameterize(statement);
	}

	@Override
	public void batch(Statement statement) throws SQLException {
		delegate.batch(statement);
	}

	@Override
	public int update(Statement statement) throws SQLException {
		return delegate.update(statement);
	}

	@Override
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		return delegate.<E>query(statement, resultHandler);
	}

	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		return delegate.queryCursor(statement);
	}

	@Override
	public BoundSql getBoundSql() {
		return delegate.getBoundSql();
	}

	@Override
	public ParameterHandler getParameterHandler() {
		return delegate.getParameterHandler();
	}
}
