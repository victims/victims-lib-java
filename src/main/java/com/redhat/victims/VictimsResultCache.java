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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
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
	 * @throws VictimsException
	 */
	protected void create(File cache) throws VictimsException {
		try {
			FileUtils.forceMkdir(cache);
		} catch (IOException e) {
			throw new VictimsException("Could not create an on disk cache", e);
		}
	}

	/**
	 * Purge the cache. The cache directory is removed and re-recreated.
	 * 
	 * @throws VictimsException
	 */
	public void purge() throws VictimsException {
		try {
			File cache = new File(location);
			if (cache.exists()) {
				FileUtils.deleteDirectory(cache);
			}
			create(cache);
		} catch (IOException e) {
			throw new VictimsException("Could not purge on disk cache.", e);
		}
	}

	public VictimsResultCache() throws VictimsException {
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
	 * The hashing function used by the Cache.
	 * 
	 * @param key
	 * @return
	 * @throws VictimsException
	 */
	protected String hash(String key) throws VictimsException {
		try {
			MessageDigest mda = MessageDigest
					.getInstance(MessageDigestAlgorithms.SHA_256);
			return Hex.encodeHexString(mda.digest(key.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			throw new VictimsException(String.format("Could not hash key: %s",
					key), e);
		}
	}

	/**
	 * Test if the given key is cached.
	 * 
	 * @param key
	 * @return
	 */
	public boolean exists(String key) {
		try {
			key = hash(key);
			return FileUtils.getFile(location, key).exists();
		} catch (VictimsException e) {
			return false;
		}
	}

	/**
	 * Delete the cache entry for a given key.
	 * 
	 * @param key
	 * @throws VictimsException
	 */
	public void delete(String key) throws VictimsException {
		key = hash(key);
		try {
			FileUtils.forceDelete(FileUtils.getFile(location, key));
		} catch (IOException e) {
			throw new VictimsException(String.format(
					"Could not delete the cached entry from disk: %s", key), e);
		}
	}

	/**
	 * Add a new cache entry.
	 * 
	 * @param key
	 * @param cves
	 *            A {@link Collection} of CVE Strings. If null an empty string
	 *            is used.
	 * @throws VictimsException
	 */
	public void add(String key, Collection<String> cves)
			throws VictimsException {
		key = hash(key);

		if (exists(key)) {
			delete(key);
		}

		String result = "";
		if (cves != null) {
			result = StringUtils.join(cves, ",");
		}

		try {
			FileOutputStream fos = new FileOutputStream(FileUtils.getFile(
					location, key));
			try {
				fos.write(result.getBytes());
			} finally {
				fos.close();
			}
		} catch (IOException e) {
			throw new VictimsException(String.format(
					"Could not add disk entry for key: %s", key), e);
		}

	}

	/**
	 * Get the CVEs mapped by a key
	 * 
	 * @param key
	 * @return {@link Collection} of CVE strings
	 * @throws VictimsException
	 */
	public HashSet<String> get(String key) throws VictimsException {
		key = hash(key);
		try {
			HashSet<String> cves = new HashSet<String>();
			String result = FileUtils.readFileToString(
					FileUtils.getFile(location, key)).trim();
			for (String cve : StringUtils.split(result, ",")) {
				cves.add(cve);
			}
			return cves;
		} catch (IOException e) {
			throw new VictimsException(String.format(
					"Could not read contents of entry with key: %s", key), e);
		}
	}
}
