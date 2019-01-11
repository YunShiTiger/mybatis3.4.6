package org.apache.ibatis.mapping;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * 对于配置多种厂商sql语句鉴别的处理类
 *   根据当前数据库的运行环境来确认能够执行的sql语句
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

	private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);

	private Properties properties;

	@Override
	public String getDatabaseId(DataSource dataSource) {
		//检测对应的数据源是否为空
		if (dataSource == null) {
			//抛出对应的异常
			throw new NullPointerException("dataSource cannot be null");
		}
		try {
			//获取当前数据库支持的对应的标识(即带有当前标识的sql语句在进行注册是会被注册上  同样没带当前标识的数据库sql语句将被抛弃)
			return getDatabaseName(dataSource);
		} catch (Exception e) {
			log.error("Could not get a databaseId from dataSource", e);
		}
		return null;
	}

	@Override
	public void setProperties(Properties p) {
		this.properties = p;
	}

	/*
	 * 获取在支持多厂商情况下,当前数据库对应的厂商
	 */
	private String getDatabaseName(DataSource dataSource) throws SQLException {
		//获取对应的数据库产品名称
		String productName = getDatabaseProductName(dataSource);
		if (this.properties != null) {
			for (Map.Entry<Object, Object> property : properties.entrySet()) {
				//简单使用的数据库产品是否是配置的支持多厂商的数据库类型
				if (productName.contains((String) property.getKey())) {
					//返回支持的对应的类型
					return (String) property.getValue();
				}
			}
			//没有匹配的返回空对象
			return null;
		}
		//直接返回对应的数据库产品名称
		return productName;
	}

	/*
	 * 根据提供的数据源获取对应数据库的产品信息
	 */
	private String getDatabaseProductName(DataSource dataSource) throws SQLException {
		Connection con = null;
		try {
			//获取对应数据源的连接
			con = dataSource.getConnection();
			//获取对应的数据源元数据信息
			DatabaseMetaData metaData = con.getMetaData();
			//获取当前数据库对应的产品名称------->即连接当前数据库对应的数据库品牌
			return metaData.getDatabaseProductName();
		} finally {
			if (con != null) {
				try {
					//关闭对应的连接
					con.close();
				} catch (SQLException e) {
					// ignored
				}
			}
		}
	}

}
