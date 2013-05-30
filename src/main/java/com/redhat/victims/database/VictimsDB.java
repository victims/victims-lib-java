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

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FilenameUtils;

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
	/**
	 * The default driver class to use.
	 * 
	 * @return
	 */
	public static String defaultDriver() {
		return "org.h2.Driver";
	}

	/**
	 * The default url for the default driver.
	 * 
	 * @return
	 */
	public static String defaultURL() {
		String cache = "";
		try {
			cache = VictimsConfig.cache().toString();
		} catch (IOException e) {
			// Ignore and use cwd
		}
		return String.format("jdbc:h2:%s;MVCC=true",
				FilenameUtils.concat(cache, "victims"));
	}

	/**
	 * Fetches an instance implementing {@link VictimsDBInterface} using the
	 * configured driver.
	 * 
	 * @return A {@link VictimsDBInterface} implementation.
	 * @throws VictimsException
	 */
	public static VictimsDBInterface db() throws VictimsException {
		if (!VictimsConfig.dbDriver().equals(defaultDriver())
				&& VictimsConfig.dbUrl().equals(defaultURL())) {
			// Custom drivers require custom urls
			throw new VictimsException(
					"A custome JDBC driver was specified without setting "
							+ VictimsConfig.Key.DB_URL);
		}
		Throwable throwable = null;
		try {
			return (VictimsDBInterface) new VictimsSqlDB();
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
