package com.redhat.victims.database;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;

/**
 * An H2 DB implementation for {@link VictimsDBInterface}
 * 
 * @author abn
 * 
 */
public class VictimsH2DB extends VictimsSqlDB {
	protected static final String DB_FILE_NAME = "victims";

	public VictimsH2DB() throws ClassNotFoundException, IOException,
			SQLException {
		super("org.h2.Driver");
		String dbFile = FilenameUtils.concat(cache, DB_FILE_NAME);
		String dbUrl = String.format("jdbc:h2:%s", dbFile);
		boolean init = !(new File(dbFile + ".h2.db")).exists();
		connect(dbUrl, init);
	}
}
