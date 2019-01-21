package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 
 */
public class SimpleStatementHandler extends BaseStatementHandler {

	public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
		super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
	}

	@Override
	public int update(Statement statement) throws SQLException {
		//
		String sql = boundSql.getSql();
		//
		Object parameterObject = boundSql.getParameterObject();
		//
		KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
		//定义变量用于记录影响数据的行数
		int rows;
		if (keyGenerator instanceof Jdbc3KeyGenerator) {
			//
			statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			//
			rows = statement.getUpdateCount();
			//
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else if (keyGenerator instanceof SelectKeyGenerator) {
			//
			statement.execute(sql);
			//
			rows = statement.getUpdateCount();
			//
			keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
		} else {
			//
			statement.execute(sql);
			//获取更新sql语句影响的数据库中数据的行数
			rows = statement.getUpdateCount();
		}
		//返回对应的影响的行数
		return rows;
	}

	@Override
	public void batch(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.addBatch(sql);
	}

	@Override
	public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.<E>handleResultSets(statement);
	}

	@Override
	public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
		String sql = boundSql.getSql();
		statement.execute(sql);
		return resultSetHandler.<E>handleCursorResultSets(statement);
	}

	@Override
	protected Statement instantiateStatement(Connection connection) throws SQLException {
		if (mappedStatement.getResultSetType() != null) {
			return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
		} else {
			return connection.createStatement();
		}
	}

	@Override
	public void parameterize(Statement statement) throws SQLException {
		// N/A
	}

}
