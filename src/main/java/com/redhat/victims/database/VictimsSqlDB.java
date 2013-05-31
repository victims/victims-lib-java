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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.redhat.victims.VictimsConfig;
import com.redhat.victims.VictimsException;
import com.redhat.victims.VictimsRecord;
import com.redhat.victims.VictimsService;
import com.redhat.victims.VictimsService.RecordStream;
import com.redhat.victims.fingerprint.Algorithms;

/**
 * This class implements {@link VictimsDBInterface} for SQL databases.
 * 
 * @author abn
 * 
 */
public class VictimsSqlDB extends VictimsSQL implements VictimsDBInterface {
	// The default file for storing the last sync'ed {@link Date}
	protected static final String UPDATE_FILE_NAME = "lastUpdate";
	protected File lastUpdate;

	// Stores a cache of the content (filehash) count per record
	protected HashMap<Integer, Integer> cachedCount;

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
	public VictimsSqlDB() throws IOException, ClassNotFoundException,
			SQLException {
		super();
		lastUpdate = FileUtils.getFile(VictimsConfig.home(), UPDATE_FILE_NAME);
	}

	/**
	 * Remove all records matching the records in the given {@link RecordStream}
	 * if it exists.
	 * 
	 * @param recordStream
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void remove(Connection connection, RecordStream recordStream)
			throws SQLException, IOException {
		PreparedStatement ps = statement(connection, Query.DELETE_RECORD_HASH);
		while (recordStream.hasNext()) {
			VictimsRecord vr = recordStream.getNext();
			setObjects(ps, vr.hash);
			ps.addBatch();
		}
		executeBatchAndClose(ps);
	}

	/**
	 * Update all records in the given {@link RecordStream}. This will remove
	 * the record if it already exits and then add it. Otherwise, it just adds
	 * it.
	 * 
	 * @param recordStream
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void update(Connection connection, RecordStream recordStream)
			throws SQLException, IOException {
		PreparedStatement insertFileHash = statement(connection,
				Query.INSERT_FILEHASH);
		PreparedStatement insertMeta = statement(connection, Query.INSERT_META);
		PreparedStatement insertCVE = statement(connection, Query.INSERT_CVES);
		while (recordStream.hasNext()) {
			VictimsRecord vr = recordStream.getNext();
			String hash = vr.hash.trim();

			// remove if already present
			deleteRecord(connection, hash);

			// add the new/updated hash
			int id = insertRecord(connection, hash);

			// insert file hahes
			for (String filehash : vr.getHashes(Algorithms.SHA512).keySet()) {
				setObjects(insertFileHash, id, filehash.trim());
				insertFileHash.addBatch();
			}

			// insert metadata key-value pairs
			HashMap<String, String> md = vr.getFlattenedMetaData();
			for (String key : md.keySet()) {
				setObjects(insertMeta, id, key, md.get(key));
				insertMeta.addBatch();
			}

			// insert cves
			for (String cve : vr.cves) {
				setObjects(insertCVE, id, cve.trim());
				insertCVE.addBatch();
			}
		}
		executeBatchAndClose(insertFileHash, insertMeta, insertCVE);
	}

	/**
	 * Sets the last updated date. Once done, next call to lastUpdated() method
	 * will return this date.
	 * 
	 * @param date
	 *            The date to set.
	 * @throws IOException
	 */
	protected void setLastUpdate(Date date) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat(VictimsRecord.DATE_FORMAT);
		String stamp = sdf.format(date);
		FileUtils.write(lastUpdate, stamp);
	}

	public Date lastUpdated() throws VictimsException {
		Throwable throwable = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat(
					VictimsRecord.DATE_FORMAT);
			Date since;

			// The default start
			since = sdf.parse("1970-01-01T00:00:00");

			if (VictimsConfig.forcedUpdate()) {
				return since;
			}

			// get last updated if available
			try {
				if (lastUpdate.exists()) {
					String temp;
					temp = FileUtils.readFileToString(lastUpdate).trim();
					since = sdf.parse(temp);
				}
			} catch (IOException e) {
				throwable = e;
			}
			return since;
		} catch (ParseException e) {
			throwable = e;
		}
		throw new VictimsException("Failed to retreive last updated data",
				throwable);
	}

	public void synchronize() throws VictimsException {
		Throwable throwable = null;
		try {
			Connection connection = getConnection();
			connection.setAutoCommit(false);
			Savepoint savepoint = connection.setSavepoint();

			try {
				VictimsService service = new VictimsService();
				Date since = lastUpdated();

				remove(connection, service.removed(since));
				update(connection, service.updates(since));

				setLastUpdate(new Date());

				// reset cache
				cachedCount = null;
			} catch (IOException e) {
				throwable = e;
			} catch (SQLException e) {
				throwable = e;
			} finally {
				if (throwable != null) {
					connection.rollback(savepoint);
				}
				connection.releaseSavepoint(savepoint);
				connection.commit();
				connection.close();
			}
		} catch (SQLException e) {
			throwable = e;
		}

		if (throwable != null) {
			throw new VictimsException("Failed to sync database", throwable);
		}
	}

	/**
	 * Returns CVEs that are ascociated with a given record id.
	 * 
	 * @param recordId
	 * @return
	 * @throws SQLException
	 */
	protected HashSet<String> getVulnerabilities(int recordId)
			throws SQLException {
		HashSet<String> cves = new HashSet<String>();
		Connection connection = getConnection();
		try {
			PreparedStatement ps = setObjects(connection, Query.FIND_CVES,
					recordId);
			ResultSet matches = ps.executeQuery();
			while (matches.next()) {
				cves.add(matches.getString(1));
			}
			matches.close();
		} finally {
			connection.close();
		}
		return cves;
	}

	public HashSet<String> getVulnerabilities(String sha512)
			throws VictimsException {
		try {
			int id = selectRecordId(sha512);
			return getVulnerabilities(id);
		} catch (SQLException e) {
			throw new VictimsException("Failed to get vulnerabilities for "
					+ sha512, e);
		}
	}

	public HashSet<String> getVulnerabilities(HashMap<String, String> props)
			throws VictimsException {
		try {
			HashSet<String> cves = new HashSet<String>();

			int requiredMinCount = props.size();
			HashMap<Integer, MutableInteger> matchedPropCount = new HashMap<Integer, MutableInteger>();

			ResultSet rs;
			PreparedStatement ps;
			Connection connection = getConnection();
			try {
				for (String key : props.keySet()) {
					String value = props.get(key);
					ps = setObjects(connection, Query.PROPERTY_MATCH, key,
							value);
					rs = ps.executeQuery();
					while (rs.next()) {
						Integer id = rs.getInt("record");
						if (!matchedPropCount.containsKey(id)) {
							matchedPropCount.put(id, new MutableInteger());
						} else {
							MutableInteger count = matchedPropCount.get(id);
							count.increment();
							if (count.get() == requiredMinCount) {
								cves.addAll(getVulnerabilities(id));
							}
						}
					}
					rs.close();
					ps.close();
				}
			} finally {
				connection.close();
			}
			return cves;
		} catch (SQLException e) {
			throw new VictimsException("Failed to search on properties", e);
		}

	}

	protected HashMap<Integer, Integer> embeddedHashCount(Set<String> hashes)
			throws SQLException {
		HashMap<Integer, Integer> hashCount = new HashMap<Integer, Integer>();
		// TODO: Is there a better way?
		String sql = constructInStringsQuery(Query.FILEHASH_MATCHES_PER_RECORD,
				hashes);
		Connection connection = getConnection();
		Statement stmt = connection.createStatement();
		try {
			ResultSet resultSet = stmt.executeQuery(sql);
			while (resultSet.next()) {
				hashCount.put(resultSet.getInt(1), resultSet.getInt(2));
			}
			resultSet.close();
		} finally {
			stmt.close();
			connection.close();
		}
		return hashCount;
	}

	protected void populateCachedCount() throws SQLException {
		Connection connection = getConnection();
		try {
			cachedCount = new HashMap<Integer, Integer>();
			Statement stmt = connection.createStatement();
			ResultSet resultSet = stmt
					.executeQuery(Query.FILEHASH_COUNT_PER_RECORD);
			while (resultSet.next()) {
				cachedCount
						.put(resultSet.getInt("record"), resultSet.getInt(2));
			}
			resultSet.close();
			stmt.close();
		} finally {
			connection.close();
		}
	}

	/**
	 * Internal method implementing search for vulnerabilities checking if the
	 * given {@link VictimsRecord}'s contents are a superset of a record in the
	 * victims database.
	 * 
	 * @param vr
	 * @return
	 * @throws SQLException
	 */
	protected HashSet<String> getEmbeddedVulnerabilities(VictimsRecord vr)
			throws SQLException {
		HashSet<String> cves = new HashSet<String>();

		Set<String> hashes = vr.getHashes(Algorithms.SHA512).keySet();
		if (hashes.size() <= 0) {
			return cves;
		}

		HashMap<Integer, Integer> hashCount = embeddedHashCount(hashes);
		if (hashCount.size() > 0) {
			if (cachedCount == null) {
				populateCachedCount();
			}

			for (Integer id : hashCount.keySet()) {
				// Match every record that has all its filehashes in
				// the provided VictimsRecord
				if (hashCount.get(id).equals(cachedCount.get(id))) {
					cves.addAll(getVulnerabilities(id));
				}
			}
		}
		return cves;
	}

	public HashSet<String> getVulnerabilities(VictimsRecord vr)
			throws VictimsException {
		try {
			HashSet<String> cves = new HashSet<String>();
			// Match jar sha512
			cves.addAll(getVulnerabilities(vr.hash.trim()));
			// Match any embedded filehashes
			cves.addAll(getEmbeddedVulnerabilities(vr));
			return cves;
		} catch (SQLException e) {
			throw new VictimsException(
					"Could not determine vulnerabilities for hash: " + vr.hash,
					e);
		}
	}

	/**
	 * This class is used internally to store counts.
	 * 
	 * @author abn
	 * 
	 */
	protected static class MutableInteger {
		/*
		 * http://stackoverflow.com/questions/81346
		 */
		int value = 1; // note that we start at 1 since we're counting

		public void increment() {
			++value;
		}

		public int get() {
			return value;
		}
	}
}
