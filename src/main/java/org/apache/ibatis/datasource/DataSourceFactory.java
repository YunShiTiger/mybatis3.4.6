package org.apache.ibatis.datasource;

import java.util.Properties;
import javax.sql.DataSource;

/**
 * 数据源工厂处理接口
 *    主要用于定义获取对应的数据源和配置属性的处理
 */
public interface DataSourceFactory {

	//对外暴露的设置属性的接口方法
	void setProperties(Properties props);

	//对外暴露的获取对应数据源的接口方法
	DataSource getDataSource();

}
