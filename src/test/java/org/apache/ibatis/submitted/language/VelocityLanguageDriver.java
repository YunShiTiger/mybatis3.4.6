package org.apache.ibatis.submitted.language;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

/**
 * Just a test case. Not a real Velocity implementation.
 */
public class VelocityLanguageDriver implements LanguageDriver {

	@Override
	public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
		return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
		return new VelocitySqlSource(configuration, script.getStringBody(""));
	}

	@Override
	public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
		return new VelocitySqlSource(configuration, script);
	}
}
