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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.victims.fingerprint.Algorithms;
import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.Fingerprint;
import com.redhat.victims.fingerprint.Metadata;

/**
 * This is the java class relating to victims records.
 * 
 * @author gcmurphy
 * @author abn
 * 
 */
public class VictimsRecord {
	protected static final String UNKNOWN = Placeholder.UNKNOWN.toString();
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String SCHEMA_VERSION = "2.0";

	// structure info
	public String db_version = SCHEMA_VERSION;

	// record information
	public RecordStatus status;
	public Integer id;
	public Date date;
	public Date submittedon;
	public String submitter;

	// entry information
	public ArrayList<String> cves;
	public String name;
	public String format;
	public String vendor;
	public String version;
	public ArrayList<MetaRecord> meta;
	public String hash;
	public HashRecords hashes;

	/**
	 * Create a {@link VictimsRecord} object when a json string is provided.
	 * 
	 * @param jsonStr
	 * @return
	 */
	public static VictimsRecord fromJSON(String jsonStr) {
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
		return gson.fromJson(jsonStr, VictimsRecord.class);
	}

	/**
	 * 
	 * @return A JSON string representation of this instance.
	 */
	@Override
	public String toString() {
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
		return gson.toJson(this);
	}

	/**
	 * Test if the given {@link VictimsRecord} is equal to this instance. The
	 * comparison is done first on combined hashes and then by testing if all
	 * available file hashes match.
	 * 
	 * @param that
	 * @return
	 */
	@Override
	public boolean equals(Object rhs) {
		if (!(rhs instanceof VictimsRecord)) {
			return false;
		}
		VictimsRecord that = (VictimsRecord) rhs;

		for (Algorithms algorithm : VictimsConfig.algorithms()) {
			// Copying sets as java.util.Set.equals do not seem to work
			// otherwise
			HashSet<String> thatHashes = new HashSet<String>(that.getHashes(
					algorithm).keySet());
			HashSet<String> thisHashes = new HashSet<String>(this.getHashes(
					algorithm).keySet());
			if (!thisHashes.equals(thatHashes)) {
				return false;
			}
		}
		return this.hash.equals(that.hash);
	}

	/**
	 * Test if hashes for the given algorithm is present in this record.
	 * 
	 * @param algorithm
	 * @return
	 */
	public boolean containsAlgorithm(Algorithms algorithm) {
		return hashes.containsKey(normalizeKey(algorithm));
	}

	/**
	 * Test if this instance of {@link VictimsRecord} contains all the file
	 * hashes present in the given instance. Comparison is done on all available
	 * algorithms until a subset match is found. If for an algorithm, either
	 * this or that record is empty, check is skipped.
	 * 
	 * @param that
	 * @return
	 */
	public boolean containsAll(Object o) {
		if (!(o instanceof VictimsRecord)) {
			return false;
		}
		VictimsRecord that = (VictimsRecord) o;

		for (Algorithms algorithm : VictimsConfig.algorithms()) {
			if (!(this.containsAlgorithm(algorithm) && that
					.containsAlgorithm(algorithm))) {
				// skip if both this and that do not have the current algorithm
				continue;
			}
			HashSet<String> thatHashes = new HashSet<String>(that.getHashes(
					algorithm).keySet());
			HashSet<String> thisHashes = new HashSet<String>(this.getHashes(
					algorithm).keySet());

			if (thisHashes.isEmpty() || thatHashes.isEmpty()) {
				// there is no real value in comparing empty sets
				continue;
			}

			if (thisHashes.containsAll(thatHashes)) {
				// we found a subset match
				return true;
			}
		}

		// we have gone through all algorithms without finding a subset match
		return false;
	}

	/**
	 * Processes a given {@link Metadata} object for for Manifest keys to
	 * determine vendor, version and name.
	 * 
	 * @param md
	 */
	private void setFromMetadata(Metadata md) {
		// TODO: add pom.properties support?
		String vendorkey = Attributes.Name.IMPLEMENTATION_VENDOR.toString();
		String versionkey = Attributes.Name.IMPLEMENTATION_VERSION.toString();
		String namekey = Attributes.Name.IMPLEMENTATION_TITLE.toString();
		if (this.vendor.equals(UNKNOWN) && md.containsKey(vendorkey)) {
			this.vendor = md.get(vendorkey);
		}
		if (this.version.equals(UNKNOWN) && md.containsKey(versionkey)) {
			this.version = md.get(versionkey);
		}
		if (this.name.equals(UNKNOWN) && md.containsKey(namekey)) {
			this.name = md.get(namekey);
		}
	}

	/**
	 * Constructor for making a {@link VictimsRecord} object from an
	 * {@link Artifact}
	 * 
	 * @param artifact
	 */
	public VictimsRecord(Artifact artifact) {
		this.name = UNKNOWN;
		this.vendor = UNKNOWN;
		this.version = UNKNOWN;
		this.status = RecordStatus.NEW;
		this.meta = new ArrayList<MetaRecord>();
		this.hashes = new HashRecords();
		this.cves = new ArrayList<String>();

		// Process the metadatas if available
		HashMap<String, Metadata> metadatas = artifact.metadata();
		if (metadatas != null) {
			for (Object key : metadatas.keySet()) {
				Metadata md = metadatas.get(key);
				setFromMetadata(md);
				MetaRecord mr = new MetaRecord();
				mr.put(FieldName.META_FILENAME, key);
				mr.put(FieldName.META_PROPERTIES, md);
				meta.add(mr);
			}
		}

		this.format = FormatMap.mapType(artifact.filetype());
		if (this.name.equals(UNKNOWN)) {
			// If metadata did not provide a name then use filename
			this.name = FilenameUtils.getBaseName(artifact.filename());
		}

		// initiate hashes with cobined hashes and empty files hashes.
		Fingerprint fingerprint = artifact.fingerprint();
		if (fingerprint != null) {
			for (Algorithms alg : fingerprint.keySet()) {
				String key = normalizeKey(alg);
				if (!this.hashes.containsKey(key)) {
					Record hashRecord = new Record();
					hashRecord.put(FieldName.FILE_HASHES, new StringMap());
					hashRecord.put(FieldName.COMBINED_HASH, artifact
							.fingerprint().get(alg));
					this.hashes.put(key, hashRecord);
				}
			}
		}

		// Reorganize hashes if available
		ArrayList<Artifact> artifacts = artifact.contents();
		if (artifacts != null) {
			for (Artifact file : artifacts) {
				if (file.filetype().equals(".class")) {
					fingerprint = file.fingerprint();
					if (fingerprint != null) {
						for (Algorithms alg : fingerprint.keySet()) {
							String key = normalizeKey(alg);
							((StringMap) this.hashes.get(key).get(
									FieldName.FILE_HASHES)).put(
									fingerprint.get(alg), file.filename());
						}
					}
				}
			}
		}

		// Get the hash for the file
		this.hash = artifact.fingerprint().get(Algorithms.SHA512);
	}

	/**
	 * Handles difference in the keys used for algorithms
	 * 
	 * @param alg
	 * @return
	 */
	public static String normalizeKey(Algorithms alg) {
		if (alg.equals(Algorithms.SHA512)) {
			return FieldName.SHA512;
		}
		return alg.toString().toLowerCase();
	}

	/**
	 * Get the combined/file hash for this record.
	 * 
	 * @param alg
	 *            The hashing algorithm.
	 * @return The hash(alg,file), if the hash for the provided algorithm exists
	 *         else returns ""
	 */
	public String getHash(Algorithms alg) {
		String key = normalizeKey(alg);
		if (hashes.containsKey(key)) {
			Record record = hashes.get(key);
			if (record.containsKey(FieldName.COMBINED_HASH)) {
				return (String) record.get(FieldName.COMBINED_HASH);
			}
		}
		return "";
	}

	/**
	 * Return a map of {fingerprint:file} of the contents (if any) for the given
	 * algorithm.
	 * 
	 * @param alg
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getHashes(Algorithms alg) {
		String key = normalizeKey(alg);
		if (hashes.containsKey(key)) {
			Record record = hashes.get(key);
			if (record.containsKey(FieldName.FILE_HASHES)) {
				return (Map<String, String>) record.get(FieldName.FILE_HASHES);
			}
		}
		return new HashMap<String, String>();
	}

	/**
	 * Return a flattenned key/value map of all available properties from all
	 * {@link MetaRecord}s
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public HashMap<String, String> getFlattenedMetaData() {
		HashMap<String, String> result = new HashMap<String, String>();
		for (MetaRecord mr : meta) {
			result.putAll((Map<String, String>) mr
					.get(FieldName.META_PROPERTIES));
		}
		return result;
	}

	/**
	 * Enumeration containing all possible values of the "status" field.
	 * 
	 * @author abn
	 * 
	 */
	public static enum RecordStatus {
		SUBMITTED, RELEASED, NEW
	};

	/**
	 * Enumeration containing all possible placeholders.
	 * 
	 * @author abn
	 * 
	 */
	public static enum Placeholder {
		UNKNOWN
	}

	/**
	 * A class containing all keys that are record specific.
	 * 
	 * @author abn
	 * 
	 */
	public static class FieldName {
		public static final String FILE_HASHES = "files";
		public static final String COMBINED_HASH = "combined";
		public static final String SHA512 = "sha512";
		public static final String META_PROPERTIES = "properties";
		public static final String META_FILENAME = "filename";
	}

	/**
	 * Contains a mapping of file types to format strings expected by the
	 * server.
	 * 
	 * @author abn
	 * 
	 */
	public static class FormatMap {
		protected static final HashMap<String, String> MAP = new HashMap<String, String>();
		static {
			MAP.put(".jar", "Jar");
			MAP.put(".class", "Class");
			MAP.put(".pom", "Pom");
		}

		/**
		 * Maps a file type to its respective format name.
		 * 
		 * @param filetype
		 * @return Format name if available, else returns "Unknown"
		 */
		public static String mapType(String filetype) {
			if (MAP.containsKey(filetype.toLowerCase())) {
				return MAP.get(filetype.toLowerCase());
			}
			return "Unknown";
		}
	}

	@SuppressWarnings("serial")
	public static class StringMap extends HashMap<String, String> {
	}

	@SuppressWarnings("serial")
	public static class Record extends HashMap<String, Object> {
	}

	@SuppressWarnings("serial")
	public static class HashRecords extends HashMap<String, Record> {
	}

	@SuppressWarnings("serial")
	public static class MetaRecord extends Record {
		public static final ArrayList<Class<?>> PERMITTED_VALUE_TYPES = new ArrayList<Class<?>>();
		static {
			PERMITTED_VALUE_TYPES.add(Metadata.class);
			PERMITTED_VALUE_TYPES.add(String.class);
			PERMITTED_VALUE_TYPES.add(Map.class);
		}

		@Override
		public Object put(String key, Object value) {
			for (Class<?> candidate : PERMITTED_VALUE_TYPES) {
				if (candidate.isAssignableFrom(value.getClass())) {
					return super.put(key, value);
				}
			}
			System.out.println(key.toString());
			throw new IllegalArgumentException(String.format(
					"Values of class type <%s> are not permitted in <%s>",
					value.getClass().getName(), this.getClass().getName()));
		}
	}
}
