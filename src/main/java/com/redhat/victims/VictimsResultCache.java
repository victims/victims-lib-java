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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

/**
 * An implementation facilitating victims scan results. Allows for mapping a key
 * (eg: sha512) to comma separated list of CVEs.
 * 
 * @author abn
 * 
 */
public class VictimsResultCache {
	protected static String CACHE_NAME = "lib.results.cache";
	protected static String location = null;
	protected static boolean purged = false;

	/**
	 * Create the parent caching directory.
	 * 
	 * @param cache
	 * @throws IOException
	 */
	protected void create(File cache) throws IOException {
		FileUtils.forceMkdir(cache);
	}

	/**
	 * Purge the cache. The cache directory is removed and re-recreated.
	 * 
	 * @throws IOException
	 */
	public void purge() throws IOException {
		File cache = new File(location);
		if (cache.exists()) {
			FileUtils.deleteDirectory(cache);
		}
		create(cache);
	}

	public VictimsResultCache() throws IOException {
		location = FilenameUtils.concat(VictimsConfig.home().toString(),
				CACHE_NAME);
		File cache = new File(location);
		if (!cache.exists()) {
			create(cache);
		} else if (VictimsConfig.purgeCache() && !purged) {
			purge();
			// make sure that another instance does not purge again
			purged = true;
		}
	}

	/**
	 * Test if the given key is cached.
	 * 
	 * @param key
	 * @return
	 */
	public boolean exists(String key) {
		return FileUtils.getFile(location, key).exists();
	}

	/**
	 * Delete the cache entry for a given key.
	 * 
	 * @param key
	 * @throws IOException
	 */
	public void delete(String key) throws IOException {
		FileUtils.forceDelete(FileUtils.getFile(location, key));
	}

	/**
	 * Add a new cache entry.
	 * 
	 * @param key
	 * @param cves
	 *            A {@link Collection} of CVE Strings. If null an empty string
	 *            is used.
	 * @throws IOException
	 */
	public void add(String key, Collection<String> cves) throws IOException {
		if (exists(key)) {
			delete(key);
		}

		String result = "";
		if (cves != null) {
			result = StringUtils.join(cves, ",");
		}

		FileOutputStream fos = new FileOutputStream(FileUtils.getFile(location,
				key));
		try {
			fos.write(result.getBytes());
		} finally {
			fos.close();
		}

	}

	/**
	 * Get the CVEs mapped by a key
	 * 
	 * @param key
	 * @return {@link Collection} of CVE strings
	 * @throws IOException
	 */
	public HashSet<String> get(String key) throws IOException {
		String result = FileUtils.readFileToString(
				FileUtils.getFile(location, key)).trim();
		HashSet<String> cves = new HashSet<String>();
		for (String cve : StringUtils.split(result, ",")) {
			cves.add(cve);
		}
		return cves;
	}
}
