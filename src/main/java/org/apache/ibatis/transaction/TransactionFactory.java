package org.apache.ibatis.transaction;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;

/**
 * 获取对应的事物管理器对象的事物管理器生成工厂处理类
 */
public interface TransactionFactory {

	//对外暴露的设置属性的接口方法
	void setProperties(Properties props);

	//对外暴露的通过连接获取对应的事物管理器的接口
	Transaction newTransaction(Connection conn);

	//对外暴露的通过数据源 隔离级别 以及是否自动提交属性来 获取对应的事物管理器的接口
	Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}
