package com.redhat.victims.database;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;

import com.redhat.victims.VictimsConfig;
import com.redhat.victims.VictimsConfig.Key;

/**
 * An H2 DB implementation for {@link VictimsDBInterface}
 * 
 * @author abn
 * 
 */
public class VictimsH2DB extends VictimsSqlDB {

	protected static final String DRIVER_CLASS = "org.h2.Driver";

	public static String driver() {
		return DRIVER_CLASS;
	}

	public VictimsH2DB() throws ClassNotFoundException, IOException,
			SQLException {
		super(driver(), VictimsConfig.dbUrl(), VictimsConfig.dbCreate());
	}

	public static String defaultURL() {
		String cache = "";
		try {
			cache = VictimsConfig.cache().toString();
		} catch (IOException e) {
			// Ignore and use cwd
		}
		String dbFile = FilenameUtils.concat(cache, "victims");
		String dbUrl = String.format("jdbc:h2:%s", dbFile);
		if (!(new File(dbFile + ".h2.db")).exists()) {
			System.setProperty(Key.DB_CREATE, "true");
		} else {
			// this is internal so clear it if not required
			System.clearProperty(Key.DB_CREATE);
		}
		return dbUrl;
	}

}
