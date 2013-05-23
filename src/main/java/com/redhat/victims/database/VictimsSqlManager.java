package com.redhat.victims.database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.h2.jdbcx.JdbcConnectionPool;

import com.redhat.victims.VictimsConfig;

public class VictimsSqlManager {
	// A global connection pool shared with all instances of this class
	protected static JdbcConnectionPool pool = null;

	// PreparedStatement cache
	protected HashMap<String, PreparedStatement> statements = 
			new HashMap<String, PreparedStatement>();

	// Global connection for this isntance
	protected Connection connection = null;

	// Queue for pending batch operations
	protected HashSet<String> batchQueue = new HashSet<String>();

	/**
	 * This retrieves a {@link PreparedStatement} instance corresponding to a
	 * query string if already prepared. Else, it will create a new one using
	 * this instance's global connection and cache it.
	 * 
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	protected PreparedStatement statement(String query) throws SQLException {
		if (!statements.containsKey(query)) {
			statements.put(query, connection().prepareStatement(query));
		}
		return statements.get(query);
	}

	/**
	 * On calling this method, all previously cached prepared statements are
	 * closed gracefully and cleared from the cache.
	 * 
	 * @throws SQLException
	 */
	protected void closePreparedStatements() throws SQLException {
		for (String query : statements.keySet()) {
			PreparedStatement ps = statements.get(query);
			ps.executeBatch();
			ps.close();
		}
		statements.clear();
	}

	/**
	 * Get this instance's global connection. This is the connection is used for
	 * all the prepared statements. If this was closed earlier or was never set,
	 * a new connection is retrieved from the pool and closePreparedStatements
	 * is called.
	 * 
	 * @return
	 * @throws SQLException
	 */
	protected Connection connection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			closePreparedStatements();
			connection = pool.getConnection();
		}
		return connection;
	}

	/**
	 * Get a new connection from the {@link VictimsSqlManager} pool.
	 * 
	 * @return
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		return pool.getConnection();
	}

	/**
	 * Execute a given query string. This is a wraper for the executeQuery()
	 * method. The prepared statement corresponding to the query from the cache
	 * is used if availabed, else is created and cached.
	 * 
	 * @param query
	 * @param objects
	 * @return
	 * @throws SQLException
	 */
	protected ResultSet executeQuery(String query, Object... objects)
			throws SQLException {
		return setObjects(query, objects).executeQuery();

	}

	/**
	 * Execute a given query string. This is a wraper for the execute() method.
	 * The prepared statement corresponding to the query from the cache is used
	 * if availabed, else is created and cached.
	 * 
	 * @param query
	 * @param objects
	 * @return
	 * @throws SQLException
	 */
	protected ResultSet execute(String query, Object... objects)
			throws SQLException {
		PreparedStatement ps = setObjects(query, objects);
		ps.execute();
		return ps.getResultSet();
	}

	/**
	 * Wraps the {@link PreparedStatement}.addBatch() method. The prepared
	 * statement from the cache is used if availabed, else is created and
	 * cached. The given objects will be set before calling addBatch and then is
	 * added to the batch queue.
	 * 
	 * @param query
	 * @param objects
	 * @throws SQLException
	 */
	protected void addBatch(String query, Object... objects)
			throws SQLException {
		setObjects(query, objects).addBatch();
		batchQueue.add(query);
	}

	/**
	 * Executes all batch executions that have been queued. Once completed, the
	 * queue is cleared.
	 * 
	 * @throws SQLException
	 */
	protected void executeBatchQueue() throws SQLException {
		for (String query : batchQueue) {
			PreparedStatement ps = statement(query);
			ps.executeBatch();
		}
		batchQueue.clear();
	}

	/**
	 * Give a query and list of objects to set, a prepared statement is created,
	 * cached and returned with the objects set in the order they are provided.
	 * 
	 * @param query
	 * @param objects
	 * @return
	 * @throws SQLException
	 */
	protected PreparedStatement setObjects(String query, Object... objects)
			throws SQLException {
		PreparedStatement ps = statement(query);
		int index = 1;
		for (Object obj : objects) {
			ps.setObject(index, obj);
			index++;
		}
		return ps;
	}

	/**
	 * Given a an sql query containing the string "IN (?)" and a set of strings,
	 * this method constructs a query by safely replacing the first occurence of
	 * "IN (?)" with "IN ('v1','v2'...)", where v1,v2,.. are in values.
	 * 
	 * @param query
	 * @param values
	 * @return
	 */
	protected String constructInStringsQuery(String query, Set<String> values) {
		assert query.contains("IN (?)");
		String sql = query.replaceFirst("IN (?)", "IN (%s)");

		StringBuffer list = new StringBuffer();
		for (String value : values) {
			if (list.length() > 0) {
				list.append(",");
			}
			value = String.format("'%s'", StringEscapeUtils.escapeSql(value));
			list.append(value);
		}

		return String.format(sql, list.toString());
	}

	/**
	 * Initializes a database by created required tables.
	 * 
	 * @throws SQLException
	 */
	protected void createDB() throws SQLException {
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		stmt.execute(Query.CREATE_TABLE_RECORDS);
		stmt.execute(Query.CREATE_TABLE_FILEHASHES);
		stmt.execute(Query.CREATE_TABLE_META);
		stmt.execute(Query.CREATE_TABLE_CVES);
		connection.close();
	}

	/**
	 * Connect to an SQL database.
	 * 
	 * @param dbUrl
	 *            The connection string to use. This is without username and
	 *            pass
	 * @param create
	 *            Does the datase have to be initialized?
	 * @throws SQLException
	 */
	protected void connect(String dbUrl, boolean create) throws SQLException {
		if (pool == null) {
			pool = JdbcConnectionPool.create(dbUrl, VictimsConfig.dbUser(),
					VictimsConfig.dbPass());
		}
		if (create) {
			// we are new in town, initialize the database.
			createDB();
		}
	}

	/**
	 * Create a new instance with the given parameters.
	 * 
	 * @param driver
	 *            The driver class to use.
	 * @param dbUrl
	 *            The connection string without username and password.
	 * @param create
	 *            Are we creating this database? If so initialize.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public VictimsSqlManager(String driver, String dbUrl, boolean create)
			throws IOException, ClassNotFoundException, SQLException {
		Class.forName(driver);
		connect(dbUrl, create);
	}

	/**
	 * Given a hahsh get the first occurance's record id.
	 * 
	 * @param hash
	 * @return
	 * @throws SQLException
	 */
	protected int selectRecordId(String hash) throws SQLException {
		ResultSet rs = setObjects(Query.GET_RECORD_ID, hash).executeQuery();
		int id = -1;
		try {
			while (rs.next()) {
				id = rs.getInt("id");
				break;
			}
		} finally {
			rs.close();
		}
		return id;
	}

	/**
	 * Insert a new record with the given hash and return the record id.
	 * 
	 * @param hash
	 * @return A record id if it was created correctly, else return -1.
	 * @throws SQLException
	 */
	protected int insertRecord(String hash) throws SQLException {
		setObjects(Query.INSERT_RECORD, hash).execute();
		ResultSet rs = statement(Query.INSERT_RECORD).getGeneratedKeys();
		int id = -1;
		try {
			while (rs.next()) {
				id = rs.getInt(1);
				break;
			}
		} finally {
			rs.close();
		}
		return id;
	}

	/**
	 * Remove records matching a given hash. This will cascade to all
	 * references.
	 * 
	 * @param hash
	 * @throws SQLException
	 */
	protected void deleteRecord(String hash) throws SQLException {
		int id = selectRecordId(hash);
		if (id > 0) {
			String[] queries = new String[] { Query.DELETE_FILEHASHES,
					Query.DELETE_METAS, Query.DELETE_CVES,
					Query.DELETE_RECORD_ID };
			for (String query : queries) {
				setObjects(query, id).execute();
			}
		}
	}

	/**
	 * This class defines SQL queries that are available.
	 * 
	 * @author abn
	 * 
	 */
	protected static class Query {
		protected final static String CREATE_TABLE_RECORDS =
				"CREATE TABLE records ( "
				+ "id BIGINT AUTO_INCREMENT, "
				+ "hash VARCHAR(128)"
				+ ")";
		protected final static String CREATE_TABLE_FILEHASHES =
				"CREATE TABLE filehashes ("
				+ "record BIGINT, "
				+ "filehash VARCHAR(128), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE"
				+ ")";
		protected final static String CREATE_TABLE_META =
				"CREATE TABLE meta ("
				+ "record BIGINT, "
				+ "key VARCHAR(255), "
				+ "value VARCHAR(255), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE"
				+ ")";
		protected final static String CREATE_TABLE_CVES =
				"CREATE TABLE cves ("
				+ "record BIGINT, "
				+ "cve VARCHAR(32), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE"
				+ ")";

		protected static final String INSERT_FILEHASH =
				"INSERT INTO filehashes (record, filehash) VALUES (?, ?)";
		protected final static String INSERT_META =
				"INSERT INTO meta (record, key, value) VALUES (?, ?, ?)";
		protected final static String INSERT_CVES =
				"INSERT INTO cves (record, cve) VALUES (?, ?)";
		protected final static String INSERT_RECORD =
				"INSERT INTO records (hash) VALUES (?)";

		protected final static String GET_RECORD_ID =
				"SELECT id FROM records WHERE hash = ?";
		protected final static String FIND_CVES =
				"SELECT cve FROM cves WHERE record = ?";

		protected final static String DELETE_RECORD_HASH =
				"DELETE FROM records WHERE hash = ?";
		protected final static String DELETE_RECORD_ID =
				"DELETE FROM records WHERE id = ?";
		protected final static String DELETE_FILEHASHES =
				"DELETE FROM filehashes WHERE record = ?";
		protected final static String DELETE_METAS =
				"DELETE FROM meta WHERE record = ?";
		protected final static String DELETE_CVES =
				"DELETE FROM cves WHERE record = ?";

		protected final static String FILEHASH_MATCHES_PER_RECORD =
				"SELECT record, count(filehash) FROM filehashes "
				+ "WHERE filehash IN (?) "
				+ "GROUP BY record";
		protected final static String FILEHASH_COUNT_PER_RECORD =
				"SELECT record, count(*) FROM filehashes GROUP BY record";
		protected final static String PROPERTY_MATCH =
				"SELECT record FROM meta WHERE key = ? AND value = ?";
	}
}
