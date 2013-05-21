package com.redhat.victims.database;

import java.util.HashMap;

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
	private static final HashMap<String, Class<?>> DRIVER_MAP = new HashMap<String, Class<?>>();

	static {
		DRIVER_MAP.put("org.h2.Driver", VictimsH2DB.class);
	}

	/**
	 * Fetches an instance implementing {@link VictimsDBInterface} using the
	 * configured driver.
	 * 
	 * @return A {@link VictimsDBInterface} implementation.
	 * @throws VictimsException
	 */
	public static VictimsDBInterface db() throws VictimsException {
		String driver = VictimsConfig.dbDriver();
		Throwable throwable = null;
		if (DRIVER_MAP.containsKey(driver)) {
			try {
				return (VictimsDBInterface) DRIVER_MAP.get(driver)
						.newInstance();
			} catch (InstantiationException e) {
				throwable = e;
			} catch (IllegalAccessException e) {
				throwable = e;
			}
		} else {
			throw new VictimsException(String.format("Invalid database driver (%s) configured.",
					driver));
		}
		throw new VictimsException("Failed to get a Victims Database instance.", throwable);
	}

}
