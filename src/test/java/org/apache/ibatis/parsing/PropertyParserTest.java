package org.apache.ibatis.parsing;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Properties;

/*
 * 进行属性解析的测试用例
 */
public class PropertyParserTest {

	@Test
	public void replaceToVariableValue() {
		//创建并配置属性集合对象
		Properties props = new Properties();
		//设置开启默认值功能,同时使用默认的默认值分割符的形式
		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
		//设置进行检测的属性信息
		props.setProperty("key", "value");
		props.setProperty("tableName", "members");
		props.setProperty("orderColumn", "member_id");
		props.setProperty("a:b", "c");
		
		//测试获取对应的值
		Assertions.assertThat(PropertyParser.parse("${key}", props)).isEqualTo("value");
		Assertions.assertThat(PropertyParser.parse("${key:aaaa}", props)).isEqualTo("value");
		Assertions.assertThat(PropertyParser.parse("SELECT * FROM ${tableName:users} ORDER BY ${orderColumn:id}", props)).isEqualTo("SELECT * FROM members ORDER BY member_id");
		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "false");
		Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("c");
		props.remove(PropertyParser.KEY_ENABLE_DEFAULT_VALUE);
		Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("c");
	}

	/*
	 * 测试默认进行查找到对应属性的值的测试用例
	 */
	@Test
	public void notReplace() {
		Properties props = new Properties();
		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
		Assertions.assertThat(PropertyParser.parse("${key}", props)).isEqualTo("${key}");
		Assertions.assertThat(PropertyParser.parse("${key}", null)).isEqualTo("${key}");

		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "false");
		Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("${a:b}");

		props.remove(PropertyParser.KEY_ENABLE_DEFAULT_VALUE);
		Assertions.assertThat(PropertyParser.parse("${a:b}", props)).isEqualTo("${a:b}");
	}

	/*
	 * 测试使用默认值的测试用例
	 */
	@Test
	public void applyDefaultValue() {
		Properties props = new Properties();
		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
		Assertions.assertThat(PropertyParser.parse("${key:default}", props)).isEqualTo("default");
		Assertions.assertThat(PropertyParser.parse("SELECT * FROM ${tableName:users} ORDER BY ${orderColumn:id}", props)).isEqualTo("SELECT * FROM users ORDER BY id");
		Assertions.assertThat(PropertyParser.parse("${key:}", props)).isEmpty();
		Assertions.assertThat(PropertyParser.parse("${key: }", props)).isEqualTo(" ");
		Assertions.assertThat(PropertyParser.parse("${key::}", props)).isEqualTo(":");
	}

	/*
	 * 测试自定义分隔符的测试用例
	 */
	@Test
	public void applyCustomSeparator() {
		Properties props = new Properties();
		props.setProperty(PropertyParser.KEY_ENABLE_DEFAULT_VALUE, "true");
		props.setProperty(PropertyParser.KEY_DEFAULT_VALUE_SEPARATOR, "?:");
		Assertions.assertThat(PropertyParser.parse("${key?:default}", props)).isEqualTo("default");
		Assertions.assertThat(PropertyParser.parse("SELECT * FROM ${schema?:prod}.${tableName == null ? 'users' : tableName} ORDER BY ${orderColumn}",props)).isEqualTo("SELECT * FROM prod.${tableName == null ? 'users' : tableName} ORDER BY ${orderColumn}");
		Assertions.assertThat(PropertyParser.parse("${key?:}", props)).isEmpty();
		Assertions.assertThat(PropertyParser.parse("${key?: }", props)).isEqualTo(" ");
		Assertions.assertThat(PropertyParser.parse("${key?::}", props)).isEqualTo(":");
	}

}
