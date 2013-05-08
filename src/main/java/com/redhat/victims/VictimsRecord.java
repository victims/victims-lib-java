package com.redhat.victims;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.jar.Attributes;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedHashTreeMap;
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
	public String toString() {
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
		return gson.toJson(this);
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
		if (this.vendor == null) {
			this.vendor = md.get(vendorkey);
		}
		if (this.version == null) {
			this.version = md.get(versionkey);
		}
		if (this.name == null) {
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
		if (this.name == null) {
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
		} else {

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
	public HashMap<String, String> getHashes(Algorithms alg) {
		String key = normalizeKey(alg);
		if (hashes.containsKey(key)) {
			Record record = hashes.get(key);
			if (record.containsKey(FieldName.FILE_HASHES)) {
				return (HashMap<String, String>) record
						.get(FieldName.FILE_HASHES);
			}
		}
		return new HashMap<String, String>();
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
			PERMITTED_VALUE_TYPES.add(LinkedHashTreeMap.class);
		}

		@Override
		public Object put(String key, Object value) {
			if (!PERMITTED_VALUE_TYPES.contains(value.getClass())) {
				System.out.println(key.toString());
				throw new IllegalArgumentException(String.format(
						"Values of class type <%s> are not permitted in <%s>",
						value.getClass().getName(), this.getClass().getName()));
			}
			return super.put(key, value);
		}
	}
}
