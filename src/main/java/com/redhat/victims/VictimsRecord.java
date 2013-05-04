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

	public static VictimsRecord fromJSON(String jsonStr) {
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
		return gson.fromJson(jsonStr, VictimsRecord.class);
	}

	public String toString() {
		Gson gson = new GsonBuilder().setDateFormat(DATE_FORMAT).create();
		return gson.toJson(this);
	}

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

		if (this.name == null) {
			// If metadata did not provide a name then use filename
			this.name = FilenameUtils.getBaseName(artifact.filename());
		}

		// Reorganize hashes if available
		for (Artifact file : artifact.contents()) {
			if (file.filetype().equals(".class")) {
				Fingerprint fingerprint = file.fingerprint();
				if (fingerprint != null) {
					for (Algorithms alg : fingerprint.keySet()) {
						String key = normalizeKey(alg);
						if (!this.hashes.containsKey(key)) {
							Record hashRecord = new Record();
							hashRecord.put(FieldName.FILE_HASHES,
									new StringMap());
							hashRecord.put(FieldName.COMBINED_HASH, artifact
									.fingerprint().get(alg));
							this.hashes.put(key, hashRecord);
						}
						((StringMap) this.hashes.get(key).get(
								FieldName.FILE_HASHES)).put(
								fingerprint.get(alg), file.filename());
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

	public static enum RecordStatus {
		SUBMITTED, RELEASED, NEW
	};

	public static class FieldName {
		public static final String FILE_HASHES = "files";
		public static final String COMBINED_HASH = "combined";
		public static final String SHA512 = "sha512";
		public static final String META_PROPERTIES = "properties";
		public static final String META_FILENAME = "filename";
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
