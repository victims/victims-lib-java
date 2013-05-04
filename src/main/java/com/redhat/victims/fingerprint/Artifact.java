package com.redhat.victims.fingerprint;

import java.util.ArrayList;
import java.util.HashMap;

import com.redhat.victims.fingerprint.Fingerprint;
import com.redhat.victims.fingerprint.Metadata;

/**
 * The main container for a hash map data structure used to store victims record
 * information.
 * 
 * @author abn
 * 
 */
@SuppressWarnings("serial")
public class Artifact extends HashMap<Key, Object> {
	/**
	 * Maintains a list of value types that can be added to the record.
	 */
	protected static ArrayList<Class<?>> PERMITTED_VALUE_TYPES = 
			new ArrayList<Class<?>>();
	static {
		PERMITTED_VALUE_TYPES.add(Artifact.class);
		PERMITTED_VALUE_TYPES.add(String.class);
		PERMITTED_VALUE_TYPES.add(ArrayList.class);
		PERMITTED_VALUE_TYPES.add(Fingerprint.class);
		PERMITTED_VALUE_TYPES.add(Metadata.class);
		PERMITTED_VALUE_TYPES.add(HashMap.class);
	};

	public Artifact() {
		super();
	}

	@Override
	public Object put(Key key, Object value) throws IllegalArgumentException {
		if (!PERMITTED_VALUE_TYPES.contains(value.getClass())) {
			throw new IllegalArgumentException(String.format(
					"Values of class type <%s> are not permitted in <%s>",
					value.getClass().getName(), this.getClass().getName()));
		}
		return super.put(key, value);
	}

	private String getString(Key key) {
		Object value = get(key);
		if (value != null) {
			return (String) value;
		}
		return null;
	}

	public String filename() {
		return getString(Key.FILENAME);
	}

	public String filetype() {
		return getString(Key.FILETYPE);

	}

	@SuppressWarnings("unchecked")
	public ArrayList<Artifact> contents() {
		Object value = get(Key.CONTENT);
		if (value != null) {
			return (ArrayList<Artifact>) value;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, Metadata> metadata() {
		Object value = get(Key.METADATA);
		if (value != null) {
			return (HashMap<String, Metadata>) value;
		}
		return null;
	}

	public Fingerprint fingerprint() {
		Object value = get(Key.FINGERPRINT);
		if (value != null) {
			return (Fingerprint) value;
		}
		return null;
	}

	public Fingerprint contentOnlyFingerprint() {
		Object value = get(Key.CONTENT_FINGERPRINT);
		if (value != null) {
			return (Fingerprint) value;
		}
		return null;
	}
}
