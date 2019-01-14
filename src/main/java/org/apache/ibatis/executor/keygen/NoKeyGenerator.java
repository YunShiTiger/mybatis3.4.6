package org.apache.ibatis.executor.keygen;

import java.sql.Statement;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

/**
 * 
 */
public class NoKeyGenerator implements KeyGenerator {

	/**
	 * A shared instance.
	 * 
	 * @since 3.4.3
	 */
	public static final NoKeyGenerator INSTANCE = new NoKeyGenerator();

	@Override
	public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// Do Nothing
	}

	@Override
	public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
		// Do Nothing
	}

}
