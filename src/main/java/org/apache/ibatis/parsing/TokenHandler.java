package org.apache.ibatis.parsing;

/**
 * 进行数据转换的通用接口处理类
 * 
 *   即将给定的字符串数据转化成对应的需要的字符串数据
 */
public interface TokenHandler {
	
	//进行字符串数据转换操作处理的统一接口方法
	String handleToken(String content);
}
