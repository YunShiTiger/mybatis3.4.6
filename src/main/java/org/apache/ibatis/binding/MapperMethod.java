package org.apache.ibatis.binding;

import org.apache.ibatis.annotations.Flush;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.reflection.TypeParameterResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 与mapper方法对应的sql语句执行方法处理类
 */
public class MapperMethod {

	//用于存储本方法对应的需要执行的sql命令对象信息
	private final SqlCommand command;
	//记录对应方法的相关签名信息
	private final MethodSignature method;

	public MapperMethod(Class<?> mapperInterface, Method method, Configuration config) {
		//
		this.command = new SqlCommand(config, mapperInterface, method);
		//解析对应方法想相关签名信息
		this.method = new MethodSignature(config, mapperInterface, method);
	}

	/*
	 * 通过传入的真实参数来执行方法对应的sql语句
	 */
	public Object execute(SqlSession sqlSession, Object[] args) {
		//定义对应用于存储执行sql后需要转换成对应的返回结果值
		Object result;
		//根据命令的类型来进行不同形式的sql语句执行处理
		switch (command.getType()) {
			//处理插入数据处理
			case INSERT: {
				//将传入的真实参数转换成在执行sql语句需要的参数形式的处理方法
				Object param = method.convertArgsToSqlCommandParam(args);
				//执行对应的插入语句,同时分析执行sql语句影响的行数结果值
				result = rowCountResult(sqlSession.insert(command.getName(), param));
				break;
			}
			//处理更新数据处理
			case UPDATE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.update(command.getName(), param));
				break;
			}
			//处理删除数据处理
			case DELETE: {
				Object param = method.convertArgsToSqlCommandParam(args);
				result = rowCountResult(sqlSession.delete(command.getName(), param));
				break;
			}
			//处理查询数据处理
			case SELECT:
				if (method.returnsVoid() && method.hasResultHandler()) {
					executeWithResultHandler(sqlSession, args);
					result = null;
				} else if (method.returnsMany()) {
					result = executeForMany(sqlSession, args);
				} else if (method.returnsMap()) {
					result = executeForMap(sqlSession, args);
				} else if (method.returnsCursor()) {
					result = executeForCursor(sqlSession, args);
				} else {
					Object param = method.convertArgsToSqlCommandParam(args);
					result = sqlSession.selectOne(command.getName(), param);
				}
				break;
			case FLUSH:
				result = sqlSession.flushStatements();
				break;
			default:
				throw new BindingException("Unknown execution method for: " + command.getName());
		}
		if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
			throw new BindingException("Mapper method '" + command.getName() + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
		}
		//对外返回需要的结果对象
		return result;
	}

	/*
	 * 将执行sql语句影响的函数转换成对应方法真实需要的返回值类型
	 */
	private Object rowCountResult(int rowCount) {
		final Object result;
		//根据方法返回值的类型来处理返回的结果值
		if (method.returnsVoid()) {
			//方法不需要返回值,直接返回空对象
			result = null;
		} else if (Integer.class.equals(method.getReturnType()) || Integer.TYPE.equals(method.getReturnType())) {
			//方法需要int类型数据,直接返回当前的数据
			result = rowCount;
		} else if (Long.class.equals(method.getReturnType()) || Long.TYPE.equals(method.getReturnType())) {
			//方法需要long类型数据,直接返回处理后的数据
			result = (long) rowCount;
		} else if (Boolean.class.equals(method.getReturnType()) || Boolean.TYPE.equals(method.getReturnType())) {
			//方法需要Boolean类型数据,通过影响的行数确定最终返回的boolean值
			result = rowCount > 0;
		} else {
			//返回类型不匹配的异常错误信息
			throw new BindingException("Mapper method '" + command.getName() + "' has an unsupported return type: " + method.getReturnType());
		}
		//返回对应的处理之后的结果
		return result;
	}

	private void executeWithResultHandler(SqlSession sqlSession, Object[] args) {
		MappedStatement ms = sqlSession.getConfiguration().getMappedStatement(command.getName());
		if (!StatementType.CALLABLE.equals(ms.getStatementType()) && void.class.equals(ms.getResultMaps().get(0).getType())) {
			throw new BindingException("method " + command.getName() + " needs either a @ResultMap annotation, a @ResultType annotation," + " or a resultType attribute in XML so a ResultHandler can be used as a parameter.");
		}
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			sqlSession.select(command.getName(), param, rowBounds, method.extractResultHandler(args));
		} else {
			sqlSession.select(command.getName(), param, method.extractResultHandler(args));
		}
	}

	private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
		List<E> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<E>selectList(command.getName(), param);
		}
		// issue #510 Collections & arrays support
		if (!method.getReturnType().isAssignableFrom(result.getClass())) {
			if (method.getReturnType().isArray()) {
				return convertToArray(result);
			} else {
				return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
			}
		}
		return result;
	}

	private <T> Cursor<T> executeForCursor(SqlSession sqlSession, Object[] args) {
		Cursor<T> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<T>selectCursor(command.getName(), param, rowBounds);
		} else {
			result = sqlSession.<T>selectCursor(command.getName(), param);
		}
		return result;
	}

	private <E> Object convertToDeclaredCollection(Configuration config, List<E> list) {
		Object collection = config.getObjectFactory().create(method.getReturnType());
		MetaObject metaObject = config.newMetaObject(collection);
		metaObject.addAll(list);
		return collection;
	}

	@SuppressWarnings("unchecked")
	private <E> Object convertToArray(List<E> list) {
		Class<?> arrayComponentType = method.getReturnType().getComponentType();
		Object array = Array.newInstance(arrayComponentType, list.size());
		if (arrayComponentType.isPrimitive()) {
			for (int i = 0; i < list.size(); i++) {
				Array.set(array, i, list.get(i));
			}
			return array;
		} else {
			return list.toArray((E[]) array);
		}
	}

	private <K, V> Map<K, V> executeForMap(SqlSession sqlSession, Object[] args) {
		Map<K, V> result;
		Object param = method.convertArgsToSqlCommandParam(args);
		if (method.hasRowBounds()) {
			RowBounds rowBounds = method.extractRowBounds(args);
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey(), rowBounds);
		} else {
			result = sqlSession.<K, V>selectMap(command.getName(), param, method.getMapKey());
		}
		return result;
	}

	public static class ParamMap<V> extends HashMap<String, V> {

		private static final long serialVersionUID = -2212268410512043556L;

		@Override
		public V get(Object key) {
			if (!super.containsKey(key)) {
				throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + keySet());
			}
			return super.get(key);
		}

	}

	public static class SqlCommand {

		//用于记录对应sql语句的标识------------------------->即 命名空间 + 对应sql语句的id标识
		private final String name;
		//记录当前sql语句对应的执行类型
		private final SqlCommandType type;

		public SqlCommand(Configuration configuration, Class<?> mapperInterface, Method method) {
			//获取对应的方法名称
			final String methodName = method.getName();
			//
			final Class<?> declaringClass = method.getDeclaringClass();
			//
			MappedStatement ms = resolveMappedStatement(mapperInterface, methodName, declaringClass, configuration);
			//
			if (ms == null) {
				if (method.getAnnotation(Flush.class) != null) {
					name = null;
					type = SqlCommandType.FLUSH;
				} else {
					throw new BindingException("Invalid bound statement (not found): " + mapperInterface.getName() + "." + methodName);
				}
			} else {
				//获取对应的执行语句对应的MappedStatement标识------------------------->即 命名空间 + 对应sql语句的id标识
				name = ms.getId();
				//获取sql语句对应的执行类型
				type = ms.getSqlCommandType();
				//检测给定的sql语句类型是否是未知类型
				if (type == SqlCommandType.UNKNOWN) {
					//抛出对应类型不能识别的异常
					throw new BindingException("Unknown execution method for: " + name);
				}
			}
		}

		public String getName() {
			return name;
		}

		public SqlCommandType getType() {
			return type;
		}

		private MappedStatement resolveMappedStatement(Class<?> mapperInterface, String methodName, Class<?> declaringClass, Configuration configuration) {
			String statementId = mapperInterface.getName() + "." + methodName;
			if (configuration.hasStatement(statementId)) {
				return configuration.getMappedStatement(statementId);
			} else if (mapperInterface.equals(declaringClass)) {
				return null;
			}
			for (Class<?> superInterface : mapperInterface.getInterfaces()) {
				if (declaringClass.isAssignableFrom(superInterface)) {
					MappedStatement ms = resolveMappedStatement(superInterface, methodName, declaringClass, configuration);
					if (ms != null) {
						return ms;
					}
				}
			}
			return null;
		}
	}

	/*
	 * 用于记录对应方法的相关签名信息的处理类
	 */
	public static class MethodSignature {

		private final boolean returnsMany;
		private final boolean returnsMap;
		private final boolean returnsVoid;
		private final boolean returnsCursor;
		private final Class<?> returnType;
		private final String mapKey;
		private final Integer resultHandlerIndex;
		private final Integer rowBoundsIndex;
		private final ParamNameResolver paramNameResolver;

		public MethodSignature(Configuration configuration, Class<?> mapperInterface, Method method) {
			//
			Type resolvedReturnType = TypeParameterResolver.resolveReturnType(method, mapperInterface);
			if (resolvedReturnType instanceof Class<?>) {
				this.returnType = (Class<?>) resolvedReturnType;
			} else if (resolvedReturnType instanceof ParameterizedType) {
				this.returnType = (Class<?>) ((ParameterizedType) resolvedReturnType).getRawType();
			} else {
				this.returnType = method.getReturnType();
			}
			this.returnsVoid = void.class.equals(this.returnType);
			this.returnsMany = configuration.getObjectFactory().isCollection(this.returnType) || this.returnType.isArray();
			this.returnsCursor = Cursor.class.equals(this.returnType);
			//
			this.mapKey = getMapKey(method);
			this.returnsMap = this.mapKey != null;
			//获取方法参数中继承自RowBounds对应的索引位置
			this.rowBoundsIndex = getUniqueParamIndex(method, RowBounds.class);
			//获取方法参数中继承自ResultHandler对应的索引位置
			this.resultHandlerIndex = getUniqueParamIndex(method, ResultHandler.class);
			//创建对应的参数名称的解析处理器对象
			this.paramNameResolver = new ParamNameResolver(configuration, method);
		}

		/*
		 * 将传入的真实参数转换成在执行sql语句需要的参数形式的处理方法
		 * 将真实方法调用时传入的参数值转换成在sql语句中可以使用的名称参数的取值
		 * 分为三种情况
		 *    1无参数--------->null
		 *    2一个参数----->对应参数类型值
		 *    3map---->键是对应的参数真实名称  值是调用方法时传入的真实参数值
		 */
		public Object convertArgsToSqlCommandParam(Object[] args) {
			return paramNameResolver.getNamedParams(args);
		}

		public boolean hasRowBounds() {
			return rowBoundsIndex != null;
		}

		public RowBounds extractRowBounds(Object[] args) {
			return hasRowBounds() ? (RowBounds) args[rowBoundsIndex] : null;
		}

		public boolean hasResultHandler() {
			return resultHandlerIndex != null;
		}

		public ResultHandler extractResultHandler(Object[] args) {
			return hasResultHandler() ? (ResultHandler) args[resultHandlerIndex] : null;
		}

		public String getMapKey() {
			return mapKey;
		}

		public Class<?> getReturnType() {
			return returnType;
		}

		public boolean returnsMany() {
			return returnsMany;
		}

		public boolean returnsMap() {
			return returnsMap;
		}

		public boolean returnsVoid() {
			return returnsVoid;
		}

		public boolean returnsCursor() {
			return returnsCursor;
		}

		/*
		 * 获取给定类型的参数类型在方法中所处的索引位置
		 */
		private Integer getUniqueParamIndex(Method method, Class<?> paramType) {
			//记录对应类型所在的参数索引位置------------------>注意这个地方使用的是对象类型
			Integer index = null;
			//获取方法所有参数的类型
			final Class<?>[] argTypes = method.getParameterTypes();
			//循环遍历所有的方法参数类型
			for (int i = 0; i < argTypes.length; i++) {
				//对应位置上的参数类型是否继承自对应的给定类型
				if (paramType.isAssignableFrom(argTypes[i])) {
					//检测是否设置已经找到过对应的类型
					if (index == null) {
						//设置初次找到对应类型的参数位置
						index = i;
					} else {
						//抛出不能在同一个方法中设置两个继承自同一个给定类型的参数
						throw new BindingException(method.getName() + " cannot have multiple " + paramType.getSimpleName() + " parameters");
					}
				}
			}
			//返回对应类型对应的索引位置值
			return index;
		}

		private String getMapKey(Method method) {
			String mapKey = null;
			if (Map.class.isAssignableFrom(method.getReturnType())) {
				final MapKey mapKeyAnnotation = method.getAnnotation(MapKey.class);
				if (mapKeyAnnotation != null) {
					mapKey = mapKeyAnnotation.value();
				}
			}
			return mapKey;
		}
	}

}
