package org.apache.ibatis.mapping;

/**
 * sql命令类型枚举
 */
public enum SqlCommandType {
	UNKNOWN, 
	INSERT, 
	UPDATE, 
	DELETE, 
	SELECT, 
	FLUSH;
}
