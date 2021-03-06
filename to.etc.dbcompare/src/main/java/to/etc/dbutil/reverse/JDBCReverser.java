package to.etc.dbutil.reverse;

import java.sql.*;
import java.util.*;

import javax.annotation.*;
import javax.sql.*;

import to.etc.dbutil.schema.*;
import to.etc.util.*;
import to.etc.webapp.query.*;

/**
 * Generic impl of a jdbc-based reverser.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Dec 22, 2006
 */
public class JDBCReverser implements Reverser {
	private DataSource m_ds;

	private DatabaseMetaData m_dmd;

	public JDBCReverser(DataSource dbc, DatabaseMetaData dmd) {
		m_ds = dbc;
		m_dmd = dmd;
	}

	@Override
	public synchronized void lazy(@Nonnull IExec what) {
		Connection dbc = null;
		try {
			dbc = m_ds.getConnection();
			what.exec(dbc);
		} catch(RuntimeException x) {
			throw x;
		} catch(Exception x) {
			throw WrappedException.wrap(x);
		} finally {
			FileTool.closeAll(dbc);
		}
	}

	@Override
	public DbSchema loadSchema(@Nullable String name, boolean lazily) throws Exception {
		Connection dbc = m_ds.getConnection();
		try {
			name = translateSchemaName(dbc, name);
			if(name == null)
				throw new IllegalStateException("Schema name not known");

			DbSchema schema = new DbSchema(this, name);
			m_dmd = dbc.getMetaData();
			reverseTables(dbc, schema);

			if(!lazily) {
				reverseColumns(dbc, schema);
				int ncols = 0;
				for(DbTable t : schema.getTables()) {
					ncols += t.getColumnList().size();
				}
//				msg("Loaded " + ncols + " columns");
				reverseIndexes(dbc, schema);
				reversePrimaryKeys(dbc, schema);
				reverseRelations(dbc, schema);
				reverseViews(dbc, schema);
				reverseProcedures(dbc, schema);
				reversePackages(dbc, schema);
				reverseTriggers(dbc, schema);
				reverseConstraints(dbc, schema);

				afterLoad(dbc, schema);
			}
			return schema;
		} finally {
			FileTool.closeAll(dbc);
		}
	}

	protected String translateSchemaName(@Nonnull Connection dbc, @Nullable String name) throws Exception {
		if(null == name)
			return "public";
		return name;
	}

	protected void afterLoad(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		// TODO Auto-generated method stub

	}

	public void reverseIndexes(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		for(DbTable t : schema.getTables())
			reverseIndexes(dbc, t);
	}

	public void reversePrimaryKeys(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		for(DbTable t : schema.getTables())
			reversePrimaryKey(dbc, t);
	}

	public void reverseRelations(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		for(DbTable t : schema.getTables())
			reverseRelations(dbc, t);
	}

	public void reverseColumns(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		for(DbTable t : schema.getTables())
			reverseColumns(dbc, t);
	}

	@Override
	public String getIdent() {
		return "Generic JDBC database reverse-engineering plug-in";
	}

	protected void msg(String s) {
		System.out.println("reverser: " + s);
	}

	protected void warning(String s) {
		System.out.println("reverser: WARNING " + s);
	}

	protected boolean isValidTable(ResultSet rs) throws Exception {
		return true;
	}

	protected void reverseTables(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {
		ResultSet rs = null;
		try {
			rs = m_dmd.getTables(null, schema.getName(), null, new String[]{"TABLE"});
			while(rs.next()) {
				if(isValidTable(rs)) {
					String name = rs.getString("TABLE_NAME");
					DbTable t = schema.createTable(name);
					t.setComments(rs.getString("REMARKS"));
				}
			}
			msg("Loaded " + schema.getTables().size() + " tables");
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	public void reverseColumns(@Nonnull Connection dbc, DbTable t) throws Exception {
		ResultSet rs = null;
		List<DbColumn> columnList = new ArrayList<DbColumn>();
		Map<String, DbColumn> columnMap = new HashMap<String, DbColumn>();

		try {
			rs = m_dmd.getColumns(null, t.getSchema().getName(), t.getName(), null); // All columns in the schema.
			int lastord = -1;
			while(rs.next()) {
				String name = rs.getString("COLUMN_NAME");
				int ord = rs.getInt("ORDINAL_POSITION");
				//                System.out.println(ord+" - " +name);
				if(lastord == -1) {
					lastord = ord;
				} else {
					if(ord <= lastord) {
						throw new IllegalStateException("JDBC driver trouble: getColumns() does not return cols ordered by position: " + lastord + ", " + ord);
					}

					//					if(lastord + 1 != ord)
					//						throw new IllegalStateException("JDBC driver trouble: getColumns() does not return cols ordered by position: " + lastord + ", " + ord);
					lastord = ord;
				}
				if(name.equals("BET_MJB"))
					dumpRow(rs);

				int daty = rs.getInt("DATA_TYPE"); // Types.xxx
				String typename = rs.getString("TYPE_NAME");
				int prec = rs.getInt("COLUMN_SIZE");
				int scale = rs.getInt("DECIMAL_DIGITS");
				int nulla = rs.getInt("NULLABLE");
				if(nulla == DatabaseMetaData.columnNullableUnknown)
					throw new IllegalStateException("JDBC driver does not know nullability of " + t.getName() + "." + name);
				ColumnType ct = decodeColumnType(daty, typename);
				if(ct == null)
					throw new IllegalStateException("Unknown type: SQLType " + daty + " (" + typename + ") in " + t.getName() + "." + name);

				DbColumn c = new DbColumn(t, name, ct, prec, scale, nulla == DatabaseMetaData.columnNullable);
				if(null != columnMap.put(name, c))
					throw new IllegalStateException("Duplicate column name '" + name + "' in table " + t.getName());
				columnList.add(c);

				c.setComment(rs.getString("REMARKS"));
				c.setPlatformTypeName(typename);
				c.setSqlType(daty);
			}
			msg("Loaded " + t.getName() + ": " + columnMap.size() + " columns");
			t.initializeColumns(columnList, columnMap);
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	static private void dumpRow(ResultSet rs) throws Exception {
		for(int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			System.out.println(i + " (" + rs.getMetaData().getColumnName(i) + ") = " + rs.getString(i));
		}
	}

	@Override
	public void reverseIndexes(@Nonnull Connection dbc, DbTable t) throws Exception {
		ResultSet rs = null;
		Map<String, DbIndex> indexMap = new HashMap<String, DbIndex>();
		try {
			rs = m_dmd.getIndexInfo(null, t.getSchema().getName(), t.getName(), false, true);
			int lastord = -1;
			String lastindex = null;
			DbIndex ix = null;
			while(rs.next()) {
				boolean nonunique = rs.getBoolean("NON_UNIQUE");
				String name = rs.getString("INDEX_NAME");
				int ord = rs.getInt("ORDINAL_POSITION");
				String col = rs.getString("COLUMN_NAME");
				String s = rs.getString("ASC_OR_DESC");
				boolean desc = "D".equalsIgnoreCase(s);
				if(col == null) {
					System.out.println("Null index column in index " + name + " of table " + t.getName());
					continue;
				}
				DbColumn c;
				try {
					c = t.getColumn(col);
				} catch(Exception x) {
					x.printStackTrace();
					continue;
				}

				//-- Is a new index being defined?
				if(lastindex == null || !lastindex.equals(name)) {
					lastindex = name;

					ix = new DbIndex(t, name, !nonunique);
					indexMap.put(name, ix);
					lastord = -1;
				} else {
					if(lastord == -1) {
						lastord = ord;
					} else {
						if(lastord + 1 != ord)
							throw new IllegalStateException("JDBC driver trouble: getIndexes() does not return cols ordered by position: " + lastord + ", " + ord);
						lastord = ord;
					}
				}
				ix.addColumn(c, desc);
			}
			t.setIndexMap(indexMap);
			msg("Loaded " + t.getName() + ": " + t.getColumnMap().size() + " indexes");
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	@Override
	public void reversePrimaryKey(@Nonnull Connection dbc, DbTable t) throws Exception {
		ResultSet rs = null;
		List<DbColumn> pkl = new ArrayList<DbColumn>(); // Stupid resultset is ordered by NAME instead of ordinal. Dumbfuckers.
		try {
			rs = m_dmd.getPrimaryKeys(null, t.getSchema().getName(), t.getName());
			DbPrimaryKey pk = null;
			String name = null;
			while(rs.next()) {
				name = rs.getString("PK_NAME");
				int ord = rs.getInt("KEY_SEQ");
				String col = rs.getString("COLUMN_NAME");
				if(col == null) {
					System.out.println("Null PK column in PK " + name + " of table " + t.getName());
					continue;
				}
				DbColumn c = t.getColumn(col);
				while(pkl.size() <= ord)
					pkl.add(null);
				pkl.set(ord, c);
			}
			if(name != null) {
				pk = new DbPrimaryKey(t, name);
				t.setPrimaryKey(pk);
				for(DbColumn c : pkl)
					if(c != null)
						pk.addColumn(c);
			}
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	protected void reverseRelations(@Nonnull Connection dbc, DbTable t) throws Exception {
		reverseRelations(dbc, t, true);
		t.markRelationsInitialized();
	}

	/**
	 * Reverse-engineer all PK -> FK relations.
	 * @throws Exception
	 */
	protected void reverseRelations(@Nonnull Connection dbc, DbTable t, boolean appendalways) throws Exception {
		ResultSet rs = null;
		try {
			String name = null;
			rs = m_dmd.getExportedKeys(null, t.getSchema().getName(), t.getName());
			int lastord = -1;
			DbRelation rel = null;
			while(rs.next()) {
				String fktname = rs.getString("FKTABLE_NAME");
				String pktname = rs.getString("PKTABLE_NAME");
				String fkcname = rs.getString("FKCOLUMN_NAME");
				String pkcname = rs.getString("PKCOLUMN_NAME");
				String fkname = rs.getString("FK_NAME");
				if(fkname != null && fkname.length() > 0 && name == null)
					name = fkname;
				int ord = rs.getInt("KEY_SEQ");
				if(!pktname.equals(t.getName()))
					throw new IllegalStateException("JDBC driver trouble: getExportedKeys returned key from table " + pktname + " while asking for table " + t.getName());

				//-- Find FK table and column and PK column referred to
				DbTable fkt = t.getSchema().getTable(fktname);
				DbColumn fkc = fkt.getColumn(fkcname);
				DbColumn pkc = t.getColumn(pkcname);

				//-- If this is a new sequence start a new relation else add to current,
				if(lastord == -1 || ord <= lastord) {
					//-- New relation.
					rel = new DbRelation(t, fkt);
					lastord = ord;
					if(appendalways || !t.internalGetParentRelationList().contains(rel)) {
						t.internalGetParentRelationList().add(rel);
						fkt.internalGetChildRelationList().add(rel);
					}
				}

				//-- Add the relation fields.
				if(name != null)
					rel.setName(name);
				rel.addPair(pkc, fkc);
			}
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	/**
	 * Lazy method to determine what relations the table is a PARENT in.
	 * @see to.etc.dbutil.reverse.Reverser#reverseParentRelation(java.sql.Connection, to.etc.dbutil.schema.DbTable)
	 */
	@Override
	public void reverseParentRelation(Connection dbc, DbTable dbTable) throws Exception {
		reverseRelations(dbc, dbTable, false);
	}

	@Override
	public void reverseChildRelations(Connection dbc, DbTable t) throws Exception {
		ResultSet rs = null;
		try {
			String name = null;
			rs = m_dmd.getExportedKeys(null, t.getSchema().getName(), t.getName());
			int lastord = -1;
			DbRelation rel = null;
			while(rs.next()) {
				String fktname = rs.getString("FKTABLE_NAME");
				String pktname = rs.getString("PKTABLE_NAME");
				String fkcname = rs.getString("FKCOLUMN_NAME");
				String pkcname = rs.getString("PKCOLUMN_NAME");
				String fkname = rs.getString("FK_NAME");
				if(fkname != null && fkname.length() > 0 && name == null)
					name = fkname;
				int ord = rs.getInt("KEY_SEQ");
				if(!pktname.equals(t.getName()))
					throw new IllegalStateException("JDBC driver trouble: getExportedKeys returned key from table " + pktname + " while asking for table " + t.getName());

				//-- Find FK table and column and PK column referred to
				DbTable fkt = t.getSchema().getTable(fktname);
				DbColumn fkc = fkt.getColumn(fkcname);
				DbColumn pkc = t.getColumn(pkcname);

				//-- If this is a new sequence start a new relation else add to current,
				if(lastord == -1 || ord <= lastord) {
					//-- New relation.
					rel = new DbRelation(t, fkt);
					lastord = ord;
					if(!t.internalGetParentRelationList().contains(rel)) {
						t.internalGetParentRelationList().add(rel);
						fkt.internalGetChildRelationList().add(rel);
					}
				}

				//-- Add the relation fields.
				if(name != null)
					rel.setName(name);
				rel.addPair(pkc, fkc);
			}
		} finally {
			try {
				if(rs != null)
					rs.close();
			} catch(Exception x) {}
		}
	}

	/**
	 * Very simple and naive impl of mapping the genericized type.
	 * @param sqltype
	 * @param typename
	 * @return
	 */
	public ColumnType decodeColumnType(int sqltype, String typename) {
		for(ColumnType t : ColumnType.getTypes()) {
			if(t.getSqlType() == sqltype)
				return t;
		}
		switch(sqltype){
			case Types.BIT:
				return ColumnType.BOOLEAN;

			case Types.DECIMAL:
				return ColumnType.NUMBER;
			case Types.LONGVARCHAR:
				return ColumnType.CLOB;
			case Types.VARBINARY:
			case Types.BINARY:
				return ColumnType.BLOB;
			case Types.FLOAT:
				return ColumnType.FLOAT;
			case Types.LONGVARBINARY:
				return ColumnType.BLOB;
		}
		return null;
		//        throw new IllegalStateException("Cannot convert SQLTYPE="+sqltype+": "+typename+" to a generic column type");
	}

	public void reverseViews(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {}

	public void reverseProcedures(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {}

	public void reverseTriggers(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {}

	public void reversePackages(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {}

	public void reverseConstraints(@Nonnull Connection dbc, @Nonnull DbSchema schema) throws Exception {}

	@Override
	public boolean typeHasPrecision(@Nonnull DbColumn column) {
		switch(column.getSqlType()){
			case Types.CHAR:
			case Types.DECIMAL:
//			case Types.INTEGER:
			case Types.LONGVARCHAR:
			case Types.NCHAR:
			case Types.NUMERIC:
			case Types.NVARCHAR:
			case Types.VARCHAR:
				return true;

			default:
				return false;
		}
	}

	@Override
	public boolean typeHasScale(@Nonnull DbColumn column) {
		switch(column.getSqlType()){
			case Types.DECIMAL:
			case Types.NUMERIC:
				return true;

			default:
				return false;
		}
	}


	/*--------------------------------------------------------------*/
	/*	CODING:	Data retrieval.										*/
	/*--------------------------------------------------------------*/
	/**
	 *
	 * @see to.etc.dbutil.reverse.Reverser#getData(to.etc.dbutil.schema.DbTable, int, int)
	 */
	@Override
	@Nonnull
	public SQLRowSet getData(@Nonnull QCriteria<SQLRow> table, int start, int end) throws Exception {
		final SQLBuilder b = new SQLBuilder(this, table, isOracleLimitSyntaxDisaster());
		b.createSelect();
		final SQLRowSet[] res = new SQLRowSet[1];
		lazy(new IExec() {

			@Override
			public void exec(@Nonnull Connection dbc) throws Exception {
				res[0] = b.query(dbc);
			}
		});
		return res[0];
	}

	@Override
	public void addSelectColumnAs(StringBuilder sb, String colname, String alias) {
		sb.append("\"" + colname + "\"").append(" as ").append(alias);
	}

	@Override
	public String wrapQueryWithRange(List<DbColumn> collist, String sql, int first, int last) {
		return null;
	}

	public boolean isOracleLimitSyntaxDisaster() {
		return false;
	}

}
