package com.redhat.victims.database;

import java.io.IOException;
import java.sql.SQLException;

import com.redhat.victims.VictimsConfig;
import com.redhat.victims.VictimsException;

/**
 * A class providing easy instantiation of DB implementation based on the
 * configured driver.
 * 
 * @author abn
 * 
 */
public class VictimsDB {

	public static String defaultDriver() {
		return VictimsH2DB.driver();
	}

	public static String defaultURL() {
		return VictimsH2DB.defaultURL();
	}

	/**
	 * Fetches an instance implementing {@link VictimsDBInterface} using the
	 * configured driver.
	 * 
	 * @return A {@link VictimsDBInterface} implementation.
	 * @throws VictimsException
	 */
	public static VictimsDBInterface db() throws VictimsException {
		Throwable throwable = null;
		try {
			return (VictimsDBInterface) new VictimsSqlDB(
					VictimsConfig.dbDriver(), VictimsConfig.dbUrl(),
					VictimsConfig.dbCreate());
		} catch (SQLException e) {
			throwable = e;
		} catch (ClassNotFoundException e) {
			throwable = e;
		} catch (IOException e) {
			throwable = e;
		}
		throw new VictimsException(
				"Failed to get a Victims Database instance.", throwable);
	}

}
