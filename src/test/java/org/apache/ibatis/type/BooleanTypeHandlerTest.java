package org.apache.ibatis.type;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class BooleanTypeHandlerTest extends BaseTypeHandlerTest {

	private static final TypeHandler<Boolean> TYPE_HANDLER = new BooleanTypeHandler();

	@Override
	@Test
	public void shouldSetParameter() throws Exception {
		TYPE_HANDLER.setParameter(ps, 1, true, null);
		verify(ps).setBoolean(1, true);
	}

	@Override
	@Test
	public void shouldGetResultFromResultSetByName() throws Exception {
		when(rs.getBoolean("column")).thenReturn(true);
		when(rs.wasNull()).thenReturn(false);
		assertEquals(true, TYPE_HANDLER.getResult(rs, "column"));
	}

	@Override
	public void shouldGetResultNullFromResultSetByName() throws Exception {
		// Unnecessary
	}

	@Override
	@Test
	public void shouldGetResultFromResultSetByPosition() throws Exception {
		when(rs.getBoolean(1)).thenReturn(true);
		when(rs.wasNull()).thenReturn(false);
		assertEquals(true, TYPE_HANDLER.getResult(rs, 1));
	}

	@Override
	public void shouldGetResultNullFromResultSetByPosition() throws Exception {
		// Unnecessary
	}

	@Override
	@Test
	public void shouldGetResultFromCallableStatement() throws Exception {
		when(cs.getBoolean(1)).thenReturn(true);
		when(cs.wasNull()).thenReturn(false);
		assertEquals(true, TYPE_HANDLER.getResult(cs, 1));
	}

	@Override
	public void shouldGetResultNullFromCallableStatement() throws Exception {
		// Unnecessary
	}

}