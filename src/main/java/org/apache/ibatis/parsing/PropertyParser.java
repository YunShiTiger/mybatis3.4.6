package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * 属性解析器处理类----->主要完成将变量类型的参数转换成对应的真实数据
 */
public class PropertyParser {

	//mybaits中属性名称前缀
	private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";

	//mybatis中配置是否去默认值的属性参数
	public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

	//mybatis中配置默认值分割符对应的属性参数
	public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

	//默认不取默认值
	private static final String ENABLE_DEFAULT_VALUE = "false";
	//默认的分隔符是:
	private static final String DEFAULT_VALUE_SEPARATOR = ":";

	private PropertyParser() {
		
	}

	/*
	 * 对给定的字符串数据进行尝试解析操作处理
	 *   即尝试将变量类型的字符串转换成真实在属性集合中对应的数据
	 */
	public static String parse(String string, Properties variables) {
		//定义进行变量转换操作处理的处理器对象
		VariableTokenHandler handler = new VariableTokenHandler(variables);
		GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
		//进行变量解析操作处理
		return parser.parse(string);
	}

	/*
	 * 通过传入的属性集合对象进行将对应的字符串转化成对应属性数据对应的字符串数据的处理类
	 */
	private static class VariableTokenHandler implements TokenHandler {
		
		//记录对应的属性集合
		private final Properties variables;
		//
		private final boolean enableDefaultValue;
		private final String defaultValueSeparator;

		private VariableTokenHandler(Properties variables) {
			this.variables = variables;
			this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
			this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
		}

		private String getPropertyValue(String key, String defaultValue) {
			return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
		}

		@Override
		public String handleToken(String content) {
			//检测传入的属性集合对象是否存在
			if (variables != null) {
				String key = content;
				if (enableDefaultValue) {
					final int separatorIndex = content.indexOf(defaultValueSeparator);
					String defaultValue = null;
					if (separatorIndex >= 0) {
						key = content.substring(0, separatorIndex);
						defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
					}
					if (defaultValue != null) {
						return variables.getProperty(key, defaultValue);
					}
				}
				if (variables.containsKey(key)) {
					return variables.getProperty(key);
				}
			}
			//没有传入属性集合就拼接会原始的内容
			return "${" + content + "}";
		}
	}

}
