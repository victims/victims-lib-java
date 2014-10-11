package com.redhat.victims.database;

/*
 * #%L
 * This file is part of victims-lib.
 * %%
 * Copyright (C) 2013 The Victims Project
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;

import com.redhat.victims.VictimsConfig;
import com.redhat.victims.VictimsException;

/**
 * This class implements SQL queries, connection managers and helper methods for
 * JDBC drivers.
 *
 * @author abn
 *
 */
public class VictimsSQL {
	private String dbDriver = null;
	private String dbUrl = null;
	private String dbUser = null;
	private String dbPass = null;

	/**
	 * Get a new connection from the {@link VictimsSqlManager} pool.
	 *
	 * @return
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		return DriverManager.getConnection(dbUrl, dbUser, dbPass);
	}

	protected boolean isSetUp(Connection connection) throws SQLException {
		boolean result = false;
		DatabaseMetaData dbm = connection.getMetaData();
		ResultSet rs = dbm.getTables(null, null, "RECORDS", null);
		result = rs.next();
		rs.close();
		return result;
	}

	/**
	 * Initializes a database by created required tables.
	 *
	 * @throws SQLException
	 */
	protected void setUp() throws SQLException {
		Connection connection = getConnection();
		try {
			if (!isSetUp(connection)) {
				Statement stmt = connection.createStatement();
				stmt.execute(Query.CREATE_TABLE_RECORDS);
				stmt.execute(Query.CREATE_TABLE_FILEHASHES);
				stmt.execute(Query.CREATE_TABLE_META);
				stmt.execute(Query.CREATE_TABLE_CVES);
				stmt.close();
			}
		} finally {
			connection.close();
		}
	}

	public VictimsSQL() throws VictimsException {
		try {
			dbDriver = VictimsConfig.dbDriver();
			dbUrl = VictimsConfig.dbUrl();
			dbUser = VictimsConfig.dbUser();
			dbPass = VictimsConfig.dbPass();
			Class.forName(dbDriver);
			setUp();
		} catch (Throwable t) {
			throw new VictimsException(
					"Could not create a VictimsSQL instance: " + t.getMessage(),
                    t);
		}
	}

	/**
	 * Wrapper to create a prepared statement.
	 *
	 * @param connection
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	protected PreparedStatement statement(Connection connection, String query)
			throws SQLException {
		return connection.prepareStatement(query);
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
	protected PreparedStatement setObjects(Connection connection, String query,
			Object... objects) throws SQLException {
		PreparedStatement ps = statement(connection, query);
		setObjects(ps, objects);
		return ps;
	}

	/**
	 * Helper function to set the values given, to a {@link PreparedStatement},
	 * in the order in which they are given.
	 *
	 * @param ps
	 * @param objects
	 * @throws SQLException
	 */
	protected void setObjects(PreparedStatement ps, Object... objects)
			throws SQLException {
		int index = 1;
		for (Object obj : objects) {
			ps.setObject(index, obj);
			index++;
		}
	}

	/**
	 * Helper function to execute all pending patch operations in the given
	 * {@link PreparedStatement}s and close it.
	 *
	 * @param preparedStatements
	 * @throws SQLException
	 */
	protected void executeBatchAndClose(PreparedStatement... preparedStatements)
			throws SQLException {
		for (PreparedStatement ps : preparedStatements) {
			ps.executeBatch();
			ps.clearBatch();
			ps.close();
		}
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
		String replace = "IN (?)";
		assert query.lastIndexOf(replace) == query.indexOf(replace);
		String sql = query.replace("IN (?)", "IN (%s)");

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
	 * Given a hash get the first occurance's record id.
	 *
	 * @param hash
	 * @return
	 * @throws SQLException
	 */
	protected int selectRecordId(String hash) throws SQLException {
		int id = -1;
		Connection connection = getConnection();
		try {
			PreparedStatement ps = setObjects(connection, Query.GET_RECORD_ID,
					hash);
			ResultSet rs = ps.executeQuery();
			try {
				while (rs.next()) {
					id = rs.getInt("id");
					break;
				}
			} finally {
				rs.close();
				ps.close();
			}
		} finally {
			connection.close();
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
	protected int insertRecord(Connection connection, String hash)
			throws SQLException {
		int id = -1;
		PreparedStatement ps = setObjects(connection, Query.INSERT_RECORD, hash);
		ps.execute();
		ResultSet rs = ps.getGeneratedKeys();
		try {
			while (rs.next()) {
				id = rs.getInt(1);
				break;
			}
		} finally {
			rs.close();
			ps.close();
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
	protected void deleteRecord(Connection connection, String hash)
			throws SQLException {
		int id = selectRecordId(hash);
		if (id > 0) {
			String[] queries = new String[] { Query.DELETE_FILEHASHES,
					Query.DELETE_METAS, Query.DELETE_CVES,
					Query.DELETE_RECORD_ID };
			for (String query : queries) {
				PreparedStatement ps = setObjects(connection, query, id);
				ps.execute();
				ps.close();
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
		protected final static String CREATE_TABLE_RECORDS = "CREATE TABLE records ( "
				+ "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
				+ "hash VARCHAR(128)" + ")";
		protected final static String CREATE_TABLE_FILEHASHES = "CREATE TABLE filehashes ("
				+ "record BIGINT, "
				+ "filehash VARCHAR(128), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";
		protected final static String CREATE_TABLE_META = "CREATE TABLE meta ("
				+ "record BIGINT, " + "prop VARCHAR(255), "
				+ "value VARCHAR(255), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";
		protected final static String CREATE_TABLE_CVES = "CREATE TABLE cves ("
				+ "record BIGINT, " + "cve VARCHAR(32), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";

		protected static final String INSERT_FILEHASH = "INSERT INTO filehashes (record, filehash) VALUES (?, ?)";
		protected final static String INSERT_META = "INSERT INTO meta (record, prop, value) VALUES (?, ?, ?)";
		protected final static String INSERT_CVES = "INSERT INTO cves (record, cve) VALUES (?, ?)";
		protected final static String INSERT_RECORD = "INSERT INTO records (hash) VALUES (?)";

		protected final static String GET_RECORD_ID = "SELECT id FROM records WHERE hash = ?";
		protected final static String FIND_CVES = "SELECT cve FROM cves WHERE record = ?";

		protected final static String DELETE_RECORD_HASH = "DELETE FROM records WHERE hash = ?";
		protected final static String DELETE_RECORD_ID = "DELETE FROM records WHERE id = ?";
		protected final static String DELETE_FILEHASHES = "DELETE FROM filehashes WHERE record = ?";
		protected final static String DELETE_METAS = "DELETE FROM meta WHERE record = ?";
		protected final static String DELETE_CVES = "DELETE FROM cves WHERE record = ?";

		protected final static String FILEHASH_MATCHES_PER_RECORD = "SELECT record, count(filehash) FROM filehashes "
				+ "WHERE filehash IN (?) " + "GROUP BY record";
		protected final static String FILEHASH_EMBEDDED_MATCH = "SELECT m.record FROM"
				+ "(SELECT record, count(*) AS count FROM TABLE(h varchar=?) t "
				+ "INNER JOIN filehashes ON t.h = filehashes.filehash GROUP BY filehashes.record) as m, "
				+ "(SELECT record, count(*) as count FROM filehashes GROUP BY record) as rc "
				+ "WHERE rc.record = m.record AND rc.count = m.count;";
		protected final static String FILEHASHES = "SELECT record, filehash FROM filehashes";
		protected final static String PROPERTY_MATCH = "SELECT record, COUNT(record) as count "
				+ "FROM TABLE (p varchar =  ?, v varchar = ? ) t "
				+ "INNER JOIN meta ON t.p = meta.prop AND t.v = meta.value "
				+ "GROUP BY record";
		protected final static String RECORD_COUNT = "SELECT COUNT(*) from records";
	}
}
