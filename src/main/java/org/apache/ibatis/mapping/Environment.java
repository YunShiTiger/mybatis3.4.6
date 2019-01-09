package org.apache.ibatis.mapping;

import javax.sql.DataSource;

import org.apache.ibatis.transaction.TransactionFactory;

/**
 * 记录对应的数据库执行环境的处理类
 */
public final class Environment {
	
	//执行环境的id标识值
	private final String id;
	//执行环境对应的事物管理工厂对象
	private final TransactionFactory transactionFactory;
	//执行环境对应的数据源对象
	private final DataSource dataSource;

	public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
		if (id == null) {
			throw new IllegalArgumentException("Parameter 'id' must not be null");
		}
		if (transactionFactory == null) {
			throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
		}
		if (dataSource == null) {
			throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
		}
		//初始化对应的属性值
		this.id = id;
		this.transactionFactory = transactionFactory;
		this.dataSource = dataSource;
	}
	
	public String getId() {
		return this.id;
	}

	public TransactionFactory getTransactionFactory() {
		return this.transactionFactory;
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	/*
	 * 构建执行环境的构建器处理类
	 */
	public static class Builder {
		
		private String id;
		private TransactionFactory transactionFactory;
		private DataSource dataSource;

		public Builder(String id) {
			this.id = id;
		}

		public Builder transactionFactory(TransactionFactory transactionFactory) {
			this.transactionFactory = transactionFactory;
			return this;
		}

		public Builder dataSource(DataSource dataSource) {
			this.dataSource = dataSource;
			return this;
		}

		public String id() {
			return this.id;
		}

		//对外暴露的通过配置相关参数来构建执行环境对象的构建方法
		public Environment build() {
			return new Environment(this.id, this.transactionFactory, this.dataSource);
		}

	}

}
