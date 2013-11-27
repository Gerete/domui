package to.etc.test.webapp.query;

import java.sql.*;

import javax.sql.*;

import org.junit.*;

import to.etc.webapp.qsql.*;
import to.etc.webapp.query.*;
import to.etc.webapp.testsupport.*;

public class TestJdbcUtil {
	static private DataSource m_ds;

	static private QDataContext m_dc;

	@BeforeClass
	static public void setUp() throws Exception {
		m_ds = TUtilTestProperties.getRawDataSource();
		Connection dbc = m_ds.getConnection();
		m_dc = new JdbcDataContext(null, dbc);
	}

	@Test
	public void testCallableStatement() throws Exception {
		//FIXME: currently this test depends on certain table, we have to find solution for this...
		String sql = "begin INSERT INTO RED_PARAMETERS (rpr_module, rpr_name, rpr_char1) VALUES (?,?,?) RETURNING rpr_id INTO ? ; end;";
		JdbcOutParam<Long> idParam = new JdbcOutParam<Long>(Long.class);
		if(JdbcUtil.executeCallableStatement(m_ds.getConnection(), sql, "test", "test", "test", idParam)) {
			Assert.assertNotNull(idParam.getValue());
		}
		m_dc.rollback();
	}
}
