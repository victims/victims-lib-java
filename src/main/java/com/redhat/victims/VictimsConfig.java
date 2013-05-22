package com.redhat.victims;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.redhat.victims.database.VictimsH2DB;

/**
 * This class provides system property keys and default values for all available
 * Victims configurations.
 * 
 * @author abn
 * 
 */
public class VictimsConfig {
	public static final HashMap<String, String> DEFAULT_PROPS = new HashMap<String, String>();

	static {
		DEFAULT_PROPS.put(Key.URI, "http://www.victi.ms/");
		DEFAULT_PROPS.put(Key.ENTRY, "service/");
		DEFAULT_PROPS.put(Key.ENCODING, "UTF-8");
		DEFAULT_PROPS.put(Key.CACHE, FilenameUtils.concat(FileUtils
				.getUserDirectory().getAbsolutePath(), ".victims"));
		DEFAULT_PROPS.put(Key.DB_DRIVER, VictimsH2DB.DRIVER_CLASS);
		DEFAULT_PROPS.put(Key.DB_USER, "victims");
		DEFAULT_PROPS.put(Key.DB_PASS, "victims");
	}

	/**
	 * Return a configured value, or the default.
	 * 
	 * @param key
	 * @return If configured, return the system property value, else return a
	 *         default. If a default is also not available, returns
	 *         <code>null</code> if no default is configured.
	 */
	private static String getPropertyValue(String key) {
		String env = System.getProperty(key);
		if (env == null) {
			if (DEFAULT_PROPS.containsKey(key)) {
				return DEFAULT_PROPS.get(key);
			} else {
				return null;
			}
		}
		return env;
	}

	/**
	 * 
	 * @return Default encoding.
	 */
	public static Charset charset() {
		String enc = getPropertyValue(Key.ENCODING);
		return Charset.forName(enc);
	}

	/**
	 * Get the webservice base URI.
	 * 
	 * @return
	 */
	public static String uri() {
		return getPropertyValue(Key.URI);
	}

	/**
	 * Get the webservice entry point.
	 * 
	 * @return
	 */
	public static String entry() {
		return getPropertyValue(Key.ENTRY);
	}

	/**
	 * Get a complete webservice uri by merging base and entry point.
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public static String serviceURI() throws MalformedURLException {
		URL merged = new URL(new URL(uri()), entry());
		return merged.toString();
	}

	/**
	 * Get the configured cache directory. If the directory does not exist, it
	 * will be created.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static File cache() throws IOException {
		File directory = new File(getPropertyValue(Key.CACHE));
		if (!directory.exists()) {
			FileUtils.forceMkdir(directory);
		}
		return directory;
	}

	/**
	 * Get the db driver class string in use.
	 * 
	 * @return
	 */
	public static String dbDriver() {
		return getPropertyValue(Key.DB_DRIVER);
	}

	/**
	 * Get the db connection URL.
	 * 
	 * @return
	 */
	public static String dbUrl() {
		String dbUrl = getPropertyValue(Key.DB_URL);
		if (dbUrl == null) {
			dbUrl = VictimsH2DB.defaultURL();
		}
		return dbUrl;
	}

	/**
	 * Should the database be created? Useful only for internal h2 db.
	 * 
	 * @return
	 */
	public static boolean dbCreate() {
		return Boolean.getBoolean(Key.DB_CREATE);
	}

	/**
	 * Get the database user configured.
	 * 
	 * @return
	 */
	public static String dbUser() {
		return getPropertyValue(Key.DB_USER);
	}

	/**
	 * Get the database password configured.
	 * 
	 * @return
	 */
	public static String dbPass() {
		return getPropertyValue(Key.DB_PASS);
	}

	/**
	 * Is a force database update required.
	 * 
	 * @return
	 */
	public static boolean forcedUpdate() {
		return Boolean.getBoolean(Key.DB_FORCE_UPDATE);
	}

	public static class Key {
		public static final String URI = "victims.service.uri";
		public static final String ENTRY = "victims.service.entry";
		public static final String ENCODING = "victims.encoding";
		public static final String CACHE = "victims.cache";
		public static final String DB_DRIVER = "victims.db.driver";
		public static final String DB_URL = "victims.db.url";
		public static final String DB_CREATE = "victims.db.create";
		public static final String DB_USER = "victims.db.user";
		public static final String DB_PASS = "victims.db.pass";
		public static final String DB_FORCE_UPDATE = "victims.db.force";
	}

}
