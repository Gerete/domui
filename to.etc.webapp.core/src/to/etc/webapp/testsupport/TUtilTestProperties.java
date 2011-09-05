package to.etc.webapp.testsupport;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

import javax.annotation.*;
import javax.sql.*;

import org.junit.*;

import to.etc.dbpool.*;
import to.etc.util.*;

public class TUtilTestProperties {
	static private Properties m_properties;

	static private boolean m_checkedProperties;

	static public class DbConnectionInfo {
		public String hostname;

		public String sid;

		public String userid;

		public String password;

		public int port;
	}

	static private DbConnectionInfo m_dbconn;

	static private DataSource m_rawDS;

	static private String m_viewpointLoginName;

	static private boolean m_gotLoginName;


	@Nonnull
	static public Properties getTestProperties() {
		Properties p = findTestProperties();
		if(null == p)
			throw new IllegalStateException("I cannot find the proper test properties.");
		return p;
	}

	@Nullable
	static public Properties findTestProperties() {
		if(m_checkedProperties)
			return m_properties;
		m_checkedProperties = true;
		InputStream is = null;
		try {
			String env = System.getenv("VPTESTCFG");
			if(env != null)
				return loadProperties(env, "VPTESTCFG");

			String sysProp = System.getProperty("VPTESTCFG");
			if(sysProp != null)
				return loadProperties(sysProp, "VPTESTCFG");

			String testFileName = System.getProperty("testProperties");
			if(testFileName != null) {
				is = testFileName.getClass().getResourceAsStream("/resource/test/" + testFileName);
				m_properties = new Properties();
				m_properties.load(is);
				return m_properties;
			}

			String uh = System.getProperty("user.home");
			if(uh != null) {
				File uhf = new File(new File(uh), ".test.properties");
				if(uhf.exists()) {
					m_properties = FileTool.loadProperties(uhf);
					return m_properties;
				}
			}

			File src = new File("./test.properties");
			if(src.exists()) {
				m_properties = FileTool.loadProperties(src);
				return m_properties;
			}

			//-- Try to open a resource depending on the host's name
			try {
				String name = InetAddress.getLocalHost().getCanonicalHostName();
				if(name != null) {
					int dot = name.indexOf('.');
					if(dot != -1)
						name = name.substring(0, dot);
					if(!name.equals("localhost")) {
						is = TUtilTestProperties.class.getResourceAsStream(name + ".properties");
						if(is != null) {
							m_properties = new Properties();
							m_properties.load(is);
							return m_properties;
						}
					}
				}
			} catch(Exception x) {}

			//-- Cannot find
			return null;

		} catch(Exception x) {
			x.printStackTrace();
			throw new RuntimeException(x);
		} finally {
			try {
				if(is != null)
					is.close();
			} catch(Exception x) {}
		}
	}

	private static Properties loadProperties(@Nonnull String sysProp, String propNamen) throws Exception {
		File f = new File(sysProp);
		if(f.exists()) {
			m_properties = FileTool.loadProperties(f);
			return m_properties;
		} else
			throw new IllegalStateException(propNamen + " System property has nonexisting file " + f);
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Test environment database config.					*/
	/*--------------------------------------------------------------*/
	/**
	 * Check if a database config is present. It does not OPEN the database, but
	 * when true database tests will run. If the database is configured but a connection
	 * cannot be made this <b>will</b> fail the tests.
	 * @return
	 */
	static public boolean hasDbConfig() {
		String db = System.getenv("VPTESTDB");
		if(db != null)
			return true;
		Properties p = findTestProperties();
		if(p == null)
			return false;
		db = p.getProperty("database");
		if(db != null)
			return true;
		return false;
	}

	/**
	 * Get the database connection string. This fails hard when no connection string
	 * is present. Use {@link #hasDbConfig()} to check if a test database is configured
	 * if the test needs to be conditional.
	 * @return
	 */
	static public String getDbString() {
		String db = System.getenv("VPTESTDB");
		if(db != null)
			return db;
		Properties p = getTestProperties();
		db = p.getProperty("database");
		if(db != null)
			return db;
		throw new IllegalStateException("No test database specified.");
	}

	/**
	 * Callable from JUnit fixures, this will "ignore" a JUnit tests when the database
	 * is unconfigured.
	 */
	static public final void assumeDatabase() {
		Assume.assumeTrue(hasDbConfig());
	}

	/**
	 *
	 * @return
	 */
	static public DbConnectionInfo getDbConn() {
		if(m_dbconn != null)
			return m_dbconn;
		String db = getDbString();
		DbConnectionInfo c = new DbConnectionInfo();

		int pos = db.indexOf('@');
		if(pos != -1) {
			String a = db.substring(0, pos);
			String b = db.substring(pos + 1);

			//-- Get userid/pw
			pos = a.indexOf(':');
			if(pos != -1) {
				c.userid = a.substring(0, pos).trim();
				c.password = a.substring(pos + 1).trim();

				pos = b.indexOf('/');
				if(pos != -1) {
					c.sid = b.substring(pos + 1).trim();
					b = b.substring(0, pos);
					pos = b.indexOf(':');
					c.port = Integer.parseInt(b.substring(pos + 1).trim());
					c.hostname = b.substring(0, pos);

					m_dbconn = c;
					return c;
				}
			}
		}
		throw new IllegalStateException("Invalid database connect string: must be 'user:password@host:port/SID', not " + db);
	}

	/**
	 * Returns the SID for the test database.
	 * @return
	 */
	static public String getDbSID() {
		return getDbConn().sid;
	}

	/**
	 * This returns the ViewPoint user name to use as the 'current user' when database
	 * related tests are running. The name defaults to 'VIEWPOINT' but can be set to
	 * another value by setting the 'userid' value in the test properties file. If the
	 * userid is set to ANONYMOUS this will return NULL.
	 * @return
	 */
	static public String getViewpointLoginName() {
		if(!m_gotLoginName) {
			m_gotLoginName = true;
			m_viewpointLoginName = getTestProperties().getProperty("loginid");
			if(m_viewpointLoginName == null)
				m_viewpointLoginName = "VIEWPOINT";
			else if("ANONYMOUS".equalsIgnoreCase(m_viewpointLoginName))
				m_viewpointLoginName = null;
		}
		return m_viewpointLoginName;
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Database connection basics.							*/
	/*--------------------------------------------------------------*/
	/**
	 * Returns a raw, unaltered datasource to the ViewPoint test database. This datasource
	 * does not alter the "current user" in red_environment.
	 *
	 * @return
	 */
	static public DataSource getRawDataSource() {
		assumeDatabase();
		if(m_rawDS == null) {
			String url = "jdbc:oracle:thin:@" + getDbConn().hostname + ":" + getDbConn().port + ":" + getDbConn().sid;
			try {
				ConnectionPool p = PoolManager.getInstance().definePool("test", "oracle.jdbc.driver.OracleDriver", url, getDbConn().userid, getDbConn().password,
					getTestProperties().getProperty("driverpath"));
				m_rawDS = p.getUnpooledDataSource();
			} catch(SQLException x) {
				throw new RuntimeException("cannot init pool: " + x, x);
			}
		}
		return m_rawDS;
	}

	static public Connection makeRawConnection() throws Exception {
		return getRawDataSource().getConnection();
	}
}
