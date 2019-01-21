package org.apache.ibatis.executor;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
public class SimpleExecutor extends BaseExecutor {

	public SimpleExecutor(Configuration configuration, Transaction transaction) {
		super(configuration, transaction);
	}

	@Override
	public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
		//定义变量用于记录对应的Statement对象
		Statement stmt = null;
		try {
			//获取配置信息类对象
			Configuration configuration = ms.getConfiguration();
			//获取对应的StatementHandler对象
			StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
			//获取对应的底层的Statement对象
			stmt = prepareStatement(handler, ms.getStatementLog());
			//执行对应的更新sql的处理
			return handler.update(stmt);
		} finally {
			//
			closeStatement(stmt);
		}
	}

	@Override
	public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
		Statement stmt = null;
		try {
			Configuration configuration = ms.getConfiguration();
			StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
			stmt = prepareStatement(handler, ms.getStatementLog());
			return handler.<E>query(stmt, resultHandler);
		} finally {
			closeStatement(stmt);
		}
	}

	@Override
	protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
		Configuration configuration = ms.getConfiguration();
		StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
		Statement stmt = prepareStatement(handler, ms.getStatementLog());
		return handler.<E>queryCursor(stmt);
	}

	@Override
	public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
		return Collections.emptyList();
	}

	/*
	 * 准备并获取对应的Statement对象
	 */
	private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
		Statement stmt;
		//获取对应的连接对象
		Connection connection = getConnection(statementLog);
		//获取对应的Statement对象------------->此处才是真正的调用并获取底层的jdbc封装的对象
		stmt = handler.prepare(connection, transaction.getTimeout());
		//触发执行对应的参数化处理
		handler.parameterize(stmt);
		//返回对应的Statement对象
		return stmt;
	}

}
