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
