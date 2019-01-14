package org.apache.ibatis.parsing;

/**
 * 用于统一解析给定标识的解析器处理类
 * 本类可以单独拿出去使用,作为一个工具类使用
 * 
 *  需要学习的一种模式
 *    解析就是进行解析  转换就是转换  各司其职的做任务
 */
public class GenericTokenParser {

	//标识的开头参数
	private final String openToken;
	//标识的结束参数
	private final String closeToken;
	//进行标识中内容转换的处理器对象
	private final TokenHandler handler;

	public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
		this.openToken = openToken;
		this.closeToken = closeToken;
		this.handler = handler;
	}

	/*
	 * 解析给定的内容中标识对应的数据的处理
	 */
	public String parse(String text) {
		//检测出来的内容是否为空
		if (text == null || text.isEmpty()) {
			return "";
		}
		//首先搜索开始标识
		int start = text.indexOf(openToken, 0);
		//检测是否有对应的开始标识
		if (start == -1) {
			//没有就直接返回原始内容
			return text;
		}
		//将需要处理的文本内容转换成对应的字符串数组------------------------>这个地方如果是大文本是否会出现数组过大的问题
		char[] src = text.toCharArray();
		int offset = 0;
		final StringBuilder builder = new StringBuilder();
		StringBuilder expression = null;
		//循环遍历,处理文本中对应的标识
		while (start > -1) {
			//首先处理检测到的开始标识是否是转义
			if (start > 0 && src[start - 1] == '\\') {
				// this open token is escaped. remove the backslash and continue.
				//添加对应的拼接内容
				builder.append(src, offset, start - offset - 1).append(openToken);
				//设置新的搜索对应的偏移位置
				offset = start + openToken.length();
			} else {
				//创建获取重置对应的查找到带转换处理的数据对象
				if (expression == null) {
					//创建对应的对象处理
					expression = new StringBuilder();
				} else {
					//设置数据长度为0,方便存储新的数据
					expression.setLength(0);
				}
				//首先将对应的开始标识前面的内容进行拼接操作处理
				builder.append(src, offset, start - offset);
				//设置进行搜索结束标识对应的偏移位置
				offset = start + openToken.length();
				//获取对应的结束标识对应的位置
				int end = text.indexOf(closeToken, offset);
				//检测是否找到对应的结束标识位置--------->此处的循环的目的是屏蔽转义字符带来的影响
				while (end > -1) {
					//检测当前获取的结束标识是否是对应的转义字符
					if (end > offset && src[end - 1] == '\\') {
						// this close token is escaped. remove the backslash and continue.
						//拼接对应的字符数据
						expression.append(src, offset, end - offset - 1).append(closeToken);
						//设置新的偏移搜索位置
						offset = end + closeToken.length();
						//重新再偏移位置开始搜索对应的结束字符
						end = text.indexOf(closeToken, offset);
					} else {
						//记录需要进行转换处理的字符串数据
						expression.append(src, offset, end - offset);
						//设置新的搜索位置---------------------------------->这个地方进行了设置
						offset = end + closeToken.length();
						//跳出循环检测出来任务
						break;
					}
				}
				//检测是否找到对应的结束标识位置
				if (end == -1) {
					//搜索没有找到对应的结束标识-------->直接拼接所有的字符串数据
					builder.append(src, start, src.length - start);
					//设置偏移为字符串长度,即不需要再次进行搜索了
					offset = src.length;
				} else {
					//触发进行数据转化操作处理,同时拼接转换后的数据
					builder.append(handler.handleToken(expression.toString()));
					//设置新的搜索偏移位置---------------------------------->这个地方也进行了设置了
					offset = end + closeToken.length();
				}
			}
			//获取下一个开始标识所在的位置
			start = text.indexOf(openToken, offset);
		}
		//根据处理的偏移位置,将偏移位置后的字符添加到拼接字符串中
		if (offset < src.length) {
			builder.append(src, offset, src.length - offset);
		}
		//获取拼接后的字符串数据
		return builder.toString();
	}
}
