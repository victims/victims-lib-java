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

/**
 * This class provides system property keys and default values for all available
 * Victims configurations.
 * 
 * @author abn
 * 
 */
public class VictimsConfig {
	private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

	public static final String URI_KEY = "victims.service.uri";
	public static final String ENTRY_KEY = "victims.service.entry";
	public static final String CACHE_KEY = "victims.local.cache";
	public static final String DB_DRIVER_KEY = "victims.local.db.driver";

	public static final HashMap<String, String> DEFAULT_PROPS = new HashMap<String, String>();

	static {
		DEFAULT_PROPS.put(URI_KEY, "http://www.victi.ms/");
		DEFAULT_PROPS.put(ENTRY_KEY, "service/");
		DEFAULT_PROPS.put(CACHE_KEY, FilenameUtils.concat(
				System.getProperty("user.home"), ".victims"));
		DEFAULT_PROPS.put(DB_DRIVER_KEY, "org.h2.Driver");
	}

	/**
	 * 
	 * @return Default encoding.
	 */
	public static Charset charset() {
		return DEFAULT_ENCODING;
	}

	/**
	 * Return a configured value, or the default.
	 * 
	 * @param key
	 * @return If configured, return the system property value, else return a
	 *         default. If a default is also not available, returns an empty
	 *         {@link String}.
	 */
	private static String getPropertyValue(String key) {
		String env = System.getProperty(key);
		if (env == null) {
			if (DEFAULT_PROPS.containsKey(key)) {
				return DEFAULT_PROPS.get(key);
			} else {
				return "";
			}
		}
		return env;
	}

	/**
	 * Get the webservice base URI.
	 * 
	 * @return
	 */
	public static String uri() {
		return getPropertyValue(URI_KEY);
	}

	/**
	 * Get the webservice entry point.
	 * 
	 * @return
	 */
	public static String entry() {
		return getPropertyValue(ENTRY_KEY);
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
		File directory = new File(getPropertyValue(CACHE_KEY));
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
		return getPropertyValue(DB_DRIVER_KEY);
	}

}
