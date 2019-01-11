package org.apache.ibatis.mapping;

import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * 根据运行环境确认当前数据库环境能够支持的数据库语句标识
 * 即:拥有本标识的数据库语句将优先加载到系统中,相同id的数据库语句将被抛弃
 * 从而支持多数据库sql语句的配置,系统系统根据当前的数据库运行环境来确认那条sql语句是对应的能够被执行的
 */
public interface DatabaseIdProvider {

	//设置对应的属性信息
	void setProperties(Properties p);

	//根据提供的数据源获取当前运行环境对应的支持的优先级更高的sql标识
	String getDatabaseId(DataSource dataSource) throws SQLException;
}
