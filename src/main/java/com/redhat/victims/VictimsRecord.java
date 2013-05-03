package com.redhat.victims;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
		String namekey = Attributes.Name.IMPLEMENTATION_VERSION.toString();
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
		this.hash = artifact.fingerprint().get(Algorithms.SHA512);
		this.hashes = new HashRecords();
		this.cves = new ArrayList<String>();

		// Process the metadatas if available
		ArrayList<Metadata> metadatas = artifact.metadata();
		if (metadatas != null) {
			for (Metadata md : artifact.metadata()) {
				setFromMetadata(md);
				MetaRecord mr = new MetaRecord();
				mr.put("properties", md);
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
						// TODO: This might be a problem later on
						String key = alg.toString().toLowerCase()
								.replace("-", "");
						if (!this.hashes.containsKey(key)) {
							this.hashes.put(key, new HashRecord());
						}
						this.hashes.get(key).put(fingerprint.get(alg),
								file.filename());
					}
				}
			}
		}
	}

	public static enum RecordStatus {
		SUBMITTED, RELEASED, NEW
	};

	@SuppressWarnings("serial")
	public static class StringMap extends HashMap<String, String> {
	}

	@SuppressWarnings("serial")
	public static class HashRecord extends HashMap<String, Object> {
	}

	@SuppressWarnings("serial")
	public static class HashRecords extends HashMap<String, HashRecord> {
	}

	@SuppressWarnings("serial")
	public static class MetaRecord extends HashMap<String, Metadata> {
	}
}
