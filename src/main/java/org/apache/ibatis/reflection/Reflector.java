package org.apache.ibatis.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that allows for easy mapping between property names and getter/setter methods.
 */
public class Reflector {

	//用于记录对应的待解析的类对象
	private final Class<?> type;
	//用于记录对应的解析类的默认构造函数
	private Constructor<?> defaultConstructor;
	private final String[] readablePropertyNames;
	private final String[] writeablePropertyNames;
	private final Map<String, Invoker> setMethods = new HashMap<String, Invoker>();
	private final Map<String, Invoker> getMethods = new HashMap<String, Invoker>();
	private final Map<String, Class<?>> setTypes = new HashMap<String, Class<?>>();
	//用于存储get类型方法  属性名称和对应的返回值类型
	private final Map<String, Class<?>> getTypes = new HashMap<String, Class<?>>();
	

	private Map<String, String> caseInsensitivePropertyMap = new HashMap<String, String>();

	public Reflector(Class<?> clazz) {
		//用于记录对应的需要待解析的类对象
		type = clazz;
		//解析类对应的默认构造函数
		addDefaultConstructor(clazz);
		//解析类对应的get方法
		addGetMethods(clazz);
		//解析类对应的set方法
		addSetMethods(clazz);
		//解析类对应的属性对象
		addFields(clazz);
		readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
		writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
		for (String propName : readablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
		for (String propName : writeablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
	}

	/*
	 * 解析并获取对应的默认构造方法
	 */
	private void addDefaultConstructor(Class<?> clazz) {
		//获取在类中所有的构造方法对象
		Constructor<?>[] consts = clazz.getDeclaredConstructors();
		//循环检测获取到的所有构造方法对象
		for (Constructor<?> constructor : consts) {
			//检测对应的方法参数个数是否是0个------------------------->即默认的构造函数的参数一定是0个参数
			if (constructor.getParameterTypes().length == 0) {
				//检测是否可以访问对应的私有方法的权限
				if (canAccessPrivateMethods()) {
					try {
						//设置本构造方法可以进行访问
						constructor.setAccessible(true);
					} catch (Exception e) {
						// Ignored. This is only a final precaution, nothing we can do.
					}
				}
				//检测给定的构造方法是否可以进行访问
				if (constructor.isAccessible()) {
					//设置默认的构造函数对象
					this.defaultConstructor = constructor;
				}
			}
		}
	}
	
	/*
	 * 处理方法名相同带来的重载操作处理
	 */
	private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
		//首先根据对应的name拿到对应的记录方法重载的方法集合对象
		List<Method> list = conflictingMethods.get(name);
		//检测对应的集合对象是否存在---->即本name对应的方法是否是首次添加
		if (list == null) {
			//创建记录方法重载的集合对象
			list = new ArrayList<Method>();
			//将对应的集合添加到冲突方法总集合中
			conflictingMethods.put(name, list);
		}
		//在重载集合中添加本方法
		list.add(method);
	}

	/*
	 * 解析并获取对应的get方法
	 */
	private void addGetMethods(Class<?> cls) {
		Map<String, List<Method>> conflictingGetters = new HashMap<String, List<Method>>();
		//获取类中记录的所有方法
		Method[] methods = getClassMethods(cls);
		//遍历所有的方法
		for (Method method : methods) {
			//过滤参数个数大于0的方法
			if (method.getParameterTypes().length > 0) {
				continue;
			}
			//获取对应的方法名称
			String name = method.getName();
			//获取需要的以get或者is开头的方法
			if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
				//获取方法名对应的属性名称------>即截取get或者is之后的字符串处理操作
				name = PropertyNamer.methodToProperty(name);
				//处理方法重载问题
				addMethodConflict(conflictingGetters, name, method);
			}
		}
		//进一步解决方法重载造成的影响
		resolveGetterConflicts(conflictingGetters);
	}

	/*
	 * 解决在解析所有get属性方法时造成的方法重载问题----------->即找到一个最终合适的方法
	 */
	private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
		//遍历所有的冲突方法集合
		for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
			Method winner = null;
			//获取对应的属性名称
			String propName = entry.getKey();
			//循环遍历同属性名下的重载方法
			for (Method candidate : entry.getValue()) {
				//处理首次进入记录的获胜的方法
				if (winner == null) {
					winner = candidate;
					continue;
				}
				//获取获胜者方法的返回值
				Class<?> winnerType = winner.getReturnType();
				//获取候选者方法的返回值
				Class<?> candidateType = candidate.getReturnType();
				//根据返回值的类型的检测谁是对应的获胜方    相同   派生
				//https://www.cnblogs.com/hzhuxin/p/7536671.html
				//https://www.cnblogs.com/greatfish/p/6097507.html
				if (candidateType.equals(winnerType)) {
					if (!boolean.class.equals(candidateType)) {
						throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property " + propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans specification and can cause unpredictable results.");
					} else if (candidate.getName().startsWith("is")) {
						//此处可以发现同名的is要高于对应的get方法对应的属性
						winner = candidate;
					}
				} else if (candidateType.isAssignableFrom(winnerType)) {
					// OK getter type is descendant
				} else if (winnerType.isAssignableFrom(candidateType)) {
					//此处说明了父类或者是父接口要低于子类或者实现
					winner = candidate;
				} else {
					throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property " + propName + " in class " + winner.getDeclaringClass() + ". This breaks the JavaBeans specification and can cause unpredictable results.");
				}
			}
			//将解析完后对应的属性和方法存储到反射类对象中
			addGetMethod(propName, winner);
		}
	}

	/*
	 * 存储对应的get属性和对应的方法
	 */
	private void addGetMethod(String name, Method method) {
		//检测给定的属性名称是否合法
		if (isValidPropertyName(name)) {
			//将对应的属性和方法添加到获取属性方法的集合中--------->注意此处又对对应的方法进行了一层封装操作处理
			getMethods.put(name, new MethodInvoker(method));
			//获取对应的返回值类型
			Type returnType = TypeParameterResolver.resolveReturnType(method, type);
			//将对应的属性名称和返回值类型放置到集合中
			getTypes.put(name, typeToClass(returnType));
		}
	}

	/*
	 * 解析并获取对应的set方法
	 */
	private void addSetMethods(Class<?> cls) {
		//创建对应的存储方法重载带来的冲突的集合对象
		Map<String, List<Method>> conflictingSetters = new HashMap<String, List<Method>>();
		//获取所有的方法
		Method[] methods = getClassMethods(cls);
		for (Method method : methods) {
			//获取对应的方法名称
			String name = method.getName();
			//检测方法名是否是set开头
			if (name.startsWith("set") && name.length() > 3) {
				//检测对应的方法参数是否是一个
				if (method.getParameterTypes().length == 1) {
					//获取本方法对应的属性名称
					name = PropertyNamer.methodToProperty(name);
					//将对应的属性和方法添加到冲突集合中
					addMethodConflict(conflictingSetters, name, method);
				}
			}
		}
		//最终分析并处理方法名重载带来的冲突问题
		resolveSetterConflicts(conflictingSetters);
	}

	/*
	 * 解除一个set属性方法对应有多个重载方法的处理
	 */
	private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
		//遍历对应的存储冲突的集合
		for (String propName : conflictingSetters.keySet()) {
			//获取对应属性的冲突函数集合
			List<Method> setters = conflictingSetters.get(propName);
			//在对应的get集合中获取对应的返回值类型
			Class<?> getterType = getTypes.get(propName);
			Method match = null;
			ReflectionException exception = null;
			for (Method setter : setters) {
				//获取set属性方法的参数类型
				Class<?> paramType = setter.getParameterTypes()[0];
				//检测对应的参数类型和对应的返回值类型是否匹配
				if (paramType.equals(getterType)) {
					//能够匹配说明是最佳的set属性方法
					match = setter;
					//进行跳出处理,不再进行查询处理了
					break;
				}
				if (exception == null) {
					try {
						//进行比较处理---->选择一个最好的匹配方法
						match = pickBetterSetter(match, setter, propName);
					} catch (ReflectionException e) {
						//there could still be the 'best match'
						match = null;
						exception = e;
					}
				}
			}
			//检测是否找到了本属性对应的最好的方法
			if (match == null) {
				throw exception;
			} else {
				//将对应的属性名称和方法添加到对应的集合中
				addSetMethod(propName, match);
			}
		}
	}

	/*
	 * 通过比较两个方法来确定出一个最好的设置属性的方法
	 */
	private Method pickBetterSetter(Method setter1, Method setter2, String property) {
		if (setter1 == null) {
			return setter2;
		}
		//获取对应的参数类型
		Class<?> paramType1 = setter1.getParameterTypes()[0];
		Class<?> paramType2 = setter2.getParameterTypes()[0];
		//通过比对对应的继承类型来进行确认那个方法更合适
		if (paramType1.isAssignableFrom(paramType2)) {
			//此处可以说明子类比对应的父类更合适
			return setter2;
		} else if (paramType2.isAssignableFrom(paramType1)) {
			return setter1;
		}
		//如果对应的重载方法对应的参数类型不对应就抛出对应的异常错误信息
		throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '" + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '" + paramType2.getName() + "'.");
	}

	/*
	 * 将对应的set属性名称和方法添加集合中
	 */
	private void addSetMethod(String name, Method method) {
		if (isValidPropertyName(name)) {
			//将对应的属性名称和触发方法放置到set属性方法集合中
			setMethods.put(name, new MethodInvoker(method));
			//获取对应的参数类型
			Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
			//将对应的属性名称和参数类型放置到集合中
			setTypes.put(name, typeToClass(paramTypes[0]));
		}
	}

	private Class<?> typeToClass(Type src) {
		Class<?> result = null;
		if (src instanceof Class) {
			result = (Class<?>) src;
		} else if (src instanceof ParameterizedType) {
			result = (Class<?>) ((ParameterizedType) src).getRawType();
		} else if (src instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) src).getGenericComponentType();
			if (componentType instanceof Class) {
				result = Array.newInstance((Class<?>) componentType, 0).getClass();
			} else {
				Class<?> componentClass = typeToClass(componentType);
				result = Array.newInstance((Class<?>) componentClass, 0).getClass();
			}
		}
		if (result == null) {
			result = Object.class;
		}
		return result;
	}

	/*
	 * 解析类中对应的属性信息
	 */
	private void addFields(Class<?> clazz) {
		//获取类中对应的所有属性字段信息
		Field[] fields = clazz.getDeclaredFields();
		//循环遍历所有的属性字段
		for (Field field : fields) {
			//检测是否可以访问私有属性的权限
			if (canAccessPrivateMethods()) {
				try {
					//设置对应的访问权限
					field.setAccessible(true);
				} catch (Exception e) {
					// Ignored. This is only a final precaution, nothing we can do.
				}
			}
			//检测对应的字段是否可以访问
			if (field.isAccessible()) {
				
				if (!setMethods.containsKey(field.getName())) {
					// issue #379 - removed the check for final because JDK 1.5 allows
					// modification of final fields through reflection (JSR-133). (JGB)
					// pr #16 - final static can only be set by the classloader
					int modifiers = field.getModifiers();
					if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
						addSetField(field);
					}
				}
				//
				if (!getMethods.containsKey(field.getName())) {
					addGetField(field);
				}
			}
		}
		//解析对应的父类中的属性字段信息
		if (clazz.getSuperclass() != null) {
			addFields(clazz.getSuperclass());
		}
	}

	private void addSetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			setMethods.put(field.getName(), new SetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			setTypes.put(field.getName(), typeToClass(fieldType));
		}
	}

	private void addGetField(Field field) {
		if (isValidPropertyName(field.getName())) {
			getMethods.put(field.getName(), new GetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			getTypes.put(field.getName(), typeToClass(fieldType));
		}
	}

	/*
	 * 检测给定的属性名称是否有效
	 * $ serialVersionUID class
	 */
	private boolean isValidPropertyName(String name) {
		return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
	}

	/*
	 * This method returns an array containing all methods declared in this class
	 * and any superclass. We use this method, instead of the simpler
	 * Class.getMethods(), because we want to look for private methods as well.
	 *
	 * @param cls The class
	 * 
	 * @return An array containing all methods in this class
	 */
	private Method[] getClassMethods(Class<?> cls) {
		Map<String, Method> uniqueMethods = new HashMap<String, Method>();
		Class<?> currentClass = cls;
		while (currentClass != null && currentClass != Object.class) {
			addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());
			// we also need to look for interface methods - because the class may be abstract
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> anInterface : interfaces) {
				addUniqueMethods(uniqueMethods, anInterface.getMethods());
			}
			currentClass = currentClass.getSuperclass();
		}
		Collection<Method> methods = uniqueMethods.values();
		return methods.toArray(new Method[methods.size()]);
	}

	private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
		for (Method currentMethod : methods) {
			if (!currentMethod.isBridge()) {
				String signature = getSignature(currentMethod);
				// check to see if the method is already known if it is known, then an extended class must have overridden a method
				if (!uniqueMethods.containsKey(signature)) {
					if (canAccessPrivateMethods()) {
						try {
							currentMethod.setAccessible(true);
						} catch (Exception e) {
							// Ignored. This is only a final precaution, nothing we can do.
						}
					}
					uniqueMethods.put(signature, currentMethod);
				}
			}
		}
	}

	private String getSignature(Method method) {
		StringBuilder sb = new StringBuilder();
		Class<?> returnType = method.getReturnType();
		if (returnType != null) {
			sb.append(returnType.getName()).append('#');
		}
		sb.append(method.getName());
		Class<?>[] parameters = method.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (i == 0) {
				sb.append(':');
			} else {
				sb.append(',');
			}
			sb.append(parameters[i].getName());
		}
		return sb.toString();
	}

	/*
	 * https://www.programcreek.com/java-api-examples/index.php?api=java.lang.reflect.ReflectPermission
	 */
	private static boolean canAccessPrivateMethods() {
		try {
			SecurityManager securityManager = System.getSecurityManager();
			if (null != securityManager) {
				securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
			}
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}

	/*
	 * Gets the name of the class the instance provides information for
	 *
	 * @return The class name
	 */
	public Class<?> getType() {
		return type;
	}

	public Constructor<?> getDefaultConstructor() {
		if (defaultConstructor != null) {
			return defaultConstructor;
		} else {
			throw new ReflectionException("There is no default constructor for " + type);
		}
	}

	public boolean hasDefaultConstructor() {
		return defaultConstructor != null;
	}

	public Invoker getSetInvoker(String propertyName) {
		Invoker method = setMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	public Invoker getGetInvoker(String propertyName) {
		Invoker method = getMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	/*
	 * Gets the type for a property setter
	 *
	 * @param propertyName - the name of the property
	 * 
	 * @return The Class of the propery setter
	 */
	public Class<?> getSetterType(String propertyName) {
		Class<?> clazz = setTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/*
	 * Gets the type for a property getter
	 *
	 * @param propertyName - the name of the property
	 * 
	 * @return The Class of the propery getter
	 */
	public Class<?> getGetterType(String propertyName) {
		Class<?> clazz = getTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/*
	 * Gets an array of the readable properties for an object
	 *
	 * @return The array
	 */
	public String[] getGetablePropertyNames() {
		return readablePropertyNames;
	}

	/*
	 * Gets an array of the writeable properties for an object
	 *
	 * @return The array
	 */
	public String[] getSetablePropertyNames() {
		return writeablePropertyNames;
	}

	/*
	 * Check to see if a class has a writeable property by name
	 *
	 * @param propertyName - the name of the property to check
	 * 
	 * @return True if the object has a writeable property by the name
	 */
	public boolean hasSetter(String propertyName) {
		return setMethods.keySet().contains(propertyName);
	}

	/*
	 * Check to see if a class has a readable property by name
	 *
	 * @param propertyName - the name of the property to check
	 * 
	 * @return True if the object has a readable property by the name
	 */
	public boolean hasGetter(String propertyName) {
		return getMethods.keySet().contains(propertyName);
	}

	public String findPropertyName(String name) {
		return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
	}
	
}
