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
	//默认的分隔符是:----------->即存在这个分割符的后面的内容就是对应的默认值的取值内容
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
		//创建对应的解析对应标识的解析器对象
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
		//用于记录是否开启默认值功能标识
		private final boolean enableDefaultValue;
		//用于记录对应的默认值取值分割符
		private final String defaultValueSeparator;

		private VariableTokenHandler(Properties variables) {
			this.variables = variables;
			//获取是否开启默认值功能标识
			this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
			//获取是否配置对应的分隔符标识
			this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
		}

		//进行带有默认值方式的获取属性值
		private String getPropertyValue(String key, String defaultValue) {
			return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
		}

		/*
		 * 通过对代码分析
		 */
		@Override
		public String handleToken(String content) {
			//检测传入的属性集合对象是否存在
			if (variables != null) {
				String key = content;
				//首先检测是否开启了默认值功能
				if (enableDefaultValue) {
					//获取对应的默认值分隔符所在位置
					final int separatorIndex = content.indexOf(defaultValueSeparator);
					//用于记录默认值的字符串对象
					String defaultValue = null;
					//检测是否找到对应的默认分隔符标识
					if (separatorIndex >= 0) {
						//截取真实的键字符串
						key = content.substring(0, separatorIndex);
						//截取设置的默认值
						defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
					}
					//如果配置了默认值,以默认值方式获取对应的属性值
					if (defaultValue != null) {
						return variables.getProperty(key, defaultValue);
					}
				}
				//默认开始默认值值功能,或者没有配置默认值方式,直接根据对应的键获取对应的属性值
				if (variables.containsKey(key)) {
					//此处根据对应的键获取对应的属性------------------->这个地方需要注意  有可能不存在对应的属性值
					return variables.getProperty(key);
				}
			}
			//没有传入属性集合就拼接会原始的内容
			return "${" + content + "}";
		}
	}

}
