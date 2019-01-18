package org.apache.ibatis.submitted.maptypehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

@MappedTypes(Map.class)
public class LabelsTypeHandler implements TypeHandler<Map<String, Object>> {

	@Override
	public void setParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
		// Not Implemented
	}

	@Override
	public Map<String, Object> getResult(ResultSet rs, String columnName) throws SQLException {
		// Not Implemented
		return null;
	}

	@Override
	public Map<String, Object> getResult(ResultSet rs, int columnIndex) throws SQLException {
		// Not Implemented
		return null;
	}

	@Override
	public Map<String, Object> getResult(CallableStatement cs, int columnIndex) throws SQLException {
		// Not Implemented
		return null;
	}

}
