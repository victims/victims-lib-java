package com.redhat.victims.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
public class VictimsSqlDB implements VictimsDBInterface {
	// The default file for storing the last sync'ed {@link Date}
	protected static final String UPDATE_FILE_NAME = "lastUpdate";
	protected File lastUpdate;

	protected String cache;
	protected Connection connection;

	// Stores a cache of the content (filehash) count per record
	protected HashMap<Integer, Integer> cachedCount;

	// Prepared statements used
	protected PreparedStatement insertRecord;
	protected PreparedStatement insertFileHash;
	protected PreparedStatement insertMeta;
	protected PreparedStatement insertCVE;
	protected PreparedStatement fetchRecordId;
	protected PreparedStatement fetchCVES;
	protected PreparedStatement deleteRecordHash;
	protected PreparedStatement deleteRecordId;
	protected PreparedStatement deleteFileHashes;
	protected PreparedStatement deleteMeta;
	protected PreparedStatement deleteCVES;
	protected PreparedStatement countMatchedFileHash;
	protected PreparedStatement countFileHashes;
	protected PreparedStatement matchProperty;

	// The set of prepared statements that need to be executed to delete a
	// record. This is to work around h2 jdbc ON DELETE CASCADE issues.
	protected PreparedStatement[] cascadeDeleteOnId;

	/**
	 * Initializes a database by created required tables.
	 * 
	 * @throws SQLException
	 */
	protected void createDB() throws SQLException {
		Statement stmt = this.connection.createStatement();
		stmt.execute(Query.CREATE_TABLE_RECORDS);
		stmt.execute(Query.CREATE_TABLE_FILEHASHES);
		stmt.execute(Query.CREATE_TABLE_META);
		stmt.execute(Query.CREATE_TABLE_CVES);
	}

	/**
	 * Sets up all prepared statements for use with the connection.
	 * 
	 * @throws SQLException
	 */
	protected void prepareStatements() throws SQLException {
		insertRecord = connection.prepareStatement(Query.INSERT_RECORD);
		insertFileHash = connection.prepareStatement(Query.INSERT_FILEHASH);
		insertMeta = connection.prepareStatement(Query.INSERT_META);
		insertCVE = connection.prepareStatement(Query.INSERT_CVES);

		fetchRecordId = connection.prepareStatement(Query.GET_RECORD_ID);
		fetchCVES = connection.prepareStatement(Query.FIND_CVES);

		deleteRecordHash = connection
				.prepareStatement(Query.DELETE_RECORD_HASH);
		deleteRecordId = connection.prepareStatement(Query.DELETE_RECORD_ID);
		deleteFileHashes = connection.prepareStatement(Query.DELETE_FILEHASHES);
		deleteMeta = connection.prepareStatement(Query.DELETE_METAS);
		deleteCVES = connection.prepareStatement(Query.DELETE_CVES);
		cascadeDeleteOnId = new PreparedStatement[] { deleteFileHashes,
				deleteMeta, deleteCVES, deleteRecordId };

		countMatchedFileHash = connection
				.prepareStatement(Query.FILEHASH_MATCHES_PER_RECORD);
		countFileHashes = connection
				.prepareStatement(Query.FILEHASH_COUNT_PER_RECORD);

		matchProperty = connection.prepareStatement(Query.PROPERTY_MATCH);
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
		this.connection = DriverManager.getConnection(dbUrl,
				VictimsConfig.dbUser(), VictimsConfig.dbPass());
		if (create) {
			// we are new in town, initialize the database.
			createDB();
		}
		prepareStatements();

		// this is to enable savepoints while synchronizing
		this.connection.setAutoCommit(false);
	}

	/**
	 * Create an instance for a given driver class.
	 * 
	 * @param driver
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public VictimsSqlDB(String driver) throws IOException,
			ClassNotFoundException {
		this.cache = VictimsConfig.cache().toString();
		this.lastUpdate = FileUtils.getFile(this.cache, UPDATE_FILE_NAME);
		Class.forName(driver);
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
	public VictimsSqlDB(String driver, String dbUrl, boolean create)
			throws IOException, ClassNotFoundException, SQLException {
		this(driver);
		connect(dbUrl, create);
	}

	/**
	 * Given a hahsh get the first occurance's record id.
	 * 
	 * @param hash
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordId(String hash) throws SQLException {
		fetchRecordId.setString(1, hash);
		ResultSet rs = fetchRecordId.executeQuery();
		while (rs.next()) {
			return rs.getInt("id");
		}
		return -1;
	}

	/**
	 * Insert a new record with the given hash and return the record id.
	 * 
	 * @param hash
	 * @return A record id if it was created correctly, else return -1.
	 * @throws SQLException
	 */
	protected int addRecord(String hash) throws SQLException {
		insertRecord.setString(1, hash);
		insertRecord.execute();
		ResultSet rs = insertRecord.getGeneratedKeys();
		while (rs.next()) {
			System.out.println(hash + " : " + rs.getInt(1));
			return rs.getInt(1);
		}
		return -1;
	}

	/**
	 * Remove records matching a given hash. This will cascade to all
	 * references.
	 * 
	 * @param hash
	 * @throws SQLException
	 */
	protected void removeRecord(String hash) throws SQLException {
		int id = getRecordId(hash);
		if (id > 0) {
			for (PreparedStatement ps : cascadeDeleteOnId) {
				ps.setInt(1, id);
				ps.execute();
			}
		}
	}

	/**
	 * Remove all records matching the records in the given {@link RecordStream}
	 * if it exists.
	 * 
	 * @param recordStream
	 * @throws SQLException
	 * @throws IOException
	 */
	protected void remove(RecordStream recordStream) throws SQLException,
			IOException {
		while (recordStream.hasNext()) {
			VictimsRecord vr = recordStream.getNext();
			deleteRecordHash.setString(1, vr.hash);
		}
		deleteRecordHash.executeBatch();
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
	protected void update(RecordStream recordStream) throws SQLException,
			IOException {

		while (recordStream.hasNext()) {
			VictimsRecord vr = recordStream.getNext();
			String hash = vr.hash.trim();

			// remove if already present
			removeRecord(hash);

			// add the new/updated hash
			int id = addRecord(hash);

			// insert file hahes
			for (String filehash : vr.getHashes(Algorithms.SHA512).keySet()) {
				insertFileHash.setInt(1, id);
				insertFileHash.setString(2, filehash.trim());
				insertFileHash.addBatch();
			}

			// insert metadata key-value pairs
			HashMap<String, String> md = vr.getFlattenedMetaData();
			for (String key : md.keySet()) {
				insertMeta.setInt(1, id);
				insertMeta.setString(2, key);
				insertMeta.setString(3, md.get(key));
				insertMeta.addBatch();
			}

			// insert cves
			for (String cve : vr.cves) {
				insertCVE.setInt(1, id);
				insertCVE.setString(2, cve.trim());
				insertCVE.addBatch();
			}

		}

		insertFileHash.executeBatch();
		insertMeta.executeBatch();
		insertCVE.executeBatch();
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
			Savepoint savepoint = this.connection.setSavepoint();

			try {
				VictimsService service = new VictimsService();
				Date since = lastUpdated();

				remove(service.removed(since));
				update(service.updates(since));

				setLastUpdate(new Date());
				this.connection.releaseSavepoint(savepoint);
				this.connection.commit();

				// reset cache
				cachedCount = null;
			} catch (IOException e) {
				throwable = e;
			} catch (SQLException e) {
				throwable = e;
			}

			this.connection.rollback(savepoint);
			this.connection.commit();
		} catch (SQLException e) {
			throwable = e;
		}

		throw new VictimsException("Failed to sync database", throwable);
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
		fetchCVES.setInt(1, recordId);
		ResultSet matches = fetchCVES.executeQuery();
		while (matches.next()) {
			cves.add(matches.getString(1));
		}
		return cves;
	}

	public HashSet<String> getVulnerabilities(String sha512)
			throws VictimsException {
		try {
			HashSet<String> cves = new HashSet<String>();
			fetchRecordId.setString(1, sha512);
			ResultSet rs = fetchRecordId.executeQuery();
			while (rs.next()) {
				cves.addAll(getVulnerabilities(rs.getInt(1)));
			}
			return cves;
		} catch (SQLException e) {
			throw new VictimsException(
					"Could not determine vulnerabilities for hash: " + sha512,
					e);
		}
	}

	public HashSet<String> getVulnerabilities(HashMap<String, String> props)
			throws VictimsException {
		try {
			HashSet<String> cves = new HashSet<String>();

			int requiredMinCount = props.size();
			HashMap<Integer, MutableInteger> matchedPropCount = new HashMap<Integer, MutableInteger>();

			ResultSet rs;
			for (String key : props.keySet()) {
				String value = props.get(key);
				matchProperty.setString(1, key);
				matchProperty.setString(2, value);
				// TODO: Look at batching this
				rs = matchProperty.executeQuery();
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
			}
			return cves;
		} catch (SQLException e) {
			throw new VictimsException("Failed to search on properties", e);
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
		HashMap<Integer, Integer> hashCount = new HashMap<Integer, Integer>();

		/*
		 * FIXME: The current h2 jdbc implementation does not do this correctly.
		 * This is done by hand now instead.
		 * 
		 * countMatchedFileHash.setObject(1,
		 * vr.getHashes(Algorithms.SHA512).keySet().toArray());
		 * System.out.println(countMatchedFileHash.toString().replace(",",
		 * "\n")); rs = countMatchedFileHash.executeQuery();
		 */
		Set<String> hashes = vr.getHashes(Algorithms.SHA512).keySet();

		if (hashes.size() <= 0) {
			return cves;
		}

		// Temporary statement construction
		String sql = Query.FILEHASH_MATCHES_PER_RECORD.replace("?", "%s");
		String contents = "'";
		for (String content : hashes) {
			contents += content + "', '";
		}
		// chop of the last 3 charectors ", '"
		contents = contents.substring(0, contents.length() - 3);

		sql = String.format(sql, contents);
		Statement stmt = connection.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		while (rs.next()) {
			hashCount.put(rs.getInt(1), rs.getInt(2));
		}

		if (hashCount.size() > 0) {
			if (cachedCount == null) {
				// populate cache if not available
				cachedCount = new HashMap<Integer, Integer>();
				rs = countFileHashes.executeQuery();
				while (rs.next()) {
					cachedCount.put(rs.getInt("record"), rs.getInt(2));
				}
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
					"Could not determine vulnerabilities for VictimsRecord with hash: "
							+ vr.hash, e);
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
				+ "id BIGINT AUTO_INCREMENT, " + "hash VARCHAR(128)" + ")";
		protected final static String CREATE_TABLE_FILEHASHES = "CREATE TABLE filehashes ("
				+ "record BIGINT, "
				+ "filehash VARCHAR(128), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";
		protected final static String CREATE_TABLE_META = "CREATE TABLE meta ("
				+ "record BIGINT, " + "key VARCHAR(255), "
				+ "value VARCHAR(255), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";
		protected final static String CREATE_TABLE_CVES = "CREATE TABLE cves ("
				+ "record BIGINT, " + "cve VARCHAR(32), "
				+ "FOREIGN KEY(record) REFERENCES records(id) "
				+ "ON DELETE CASCADE" + ")";

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
				+ "WHERE filehash IN (?) " + "GROUP BY record";
		protected final static String FILEHASH_COUNT_PER_RECORD = 
				"SELECT record, count(*) FROM filehashes GROUP BY record";
		protected final static String PROPERTY_MATCH =
				"SELECT record FROM meta WHERE key = ? AND value = ?";
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
