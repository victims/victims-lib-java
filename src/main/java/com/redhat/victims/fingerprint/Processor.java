package com.redhat.victims.fingerprint;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

/**
 * The Processor acts as an entry point for handling fingerprinting. This class
 * also hadles dynamic processing of files/byte arrays/input streams.
 * 
 * @author abn
 * 
 */
public class Processor {
	private static DefaultHashMap<String, Class<?>> TYPE_MAP = 
			new DefaultHashMap<String, Class<?>>(File.class);

	// Keys used in records
	public static String CONTENT_KEY = "content";
	public static String CONTENT_FINGERPRINT_KEY = "content-fingerprint";
	public static String FINGERPRINT_KEY = "fingerprint";
	public static String METADATA_KEY = "metadata";
	public static String FILENAME_KEY = "filename";

	// Static Initializations
	static {
		// File Types
		TYPE_MAP.put(".class", ClassFile.class);
		TYPE_MAP.put(".jar", JarFile.class);
	}

	/**
	 * 
	 * @param filetype
	 *            The type of a file to check for. eg: ".class" ".jar"
	 * @return Class to handle the file of the given type.
	 */
	public static Class<?> getProcessor(String filetype) {
		return TYPE_MAP.get(filetype.toLowerCase());
	}

	/**
	 * 
	 * @param fileType
	 *            The type of a file to check for. eg: ".class" ".jar"
	 * @return true if the given file type is configured explicitely, else
	 *         false.
	 */
	public static boolean isKnownType(String fileType) {
		return TYPE_MAP.containsKey(fileType);
	}

	/**
	 * Process the given file (as bytes) and return the information record.
	 * 
	 * @param bytes
	 *            The file to process as a byte array.
	 * @param fileName
	 *            The name of the file being processed.
	 * @return Information record of type {@link Artifact}
	 */
	public static Artifact process(byte[] bytes, String fileName) {
		String fileType = Processor.getFileType(fileName);
		if (Processor.isKnownType(fileType)) {
			// Only handle types we know about eg: .class .jar
			Class<?> cls = Processor.getProcessor(fileType);
			try {
				if (AbstractFile.class.isAssignableFrom(cls)) {
					// TOOD: Maybe find a better way of doing this.
					Constructor<?> ctor = cls.getConstructor(byte[].class,
							String.class);
					Object object = ctor.newInstance(new Object[] { bytes,
							fileName });
					return ((FingerprintInterface) object).getRecord();
				}
			} catch (Exception e) {
				// TODO: Handle bad file
			}
		}
		return null;
	}

	/**
	 * @param is
	 *            The file as an input stream.
	 * @param fileName
	 *            The name of the file provided by the stream.
	 * @return Information record of type {@link Artifact}
	 * @throws IOException
	 */
	public static Artifact process(InputStream is, String fileName)
			throws IOException {
		return process(IOUtils.toByteArray(is), fileName);
	}

	/**
	 * 
	 * @param fileName
	 *            The name of the file provided by the stream.
	 * @return Information record of type {@link Artifact}
	 * @throws IOException
	 */
	public static Artifact process(String fileName) throws IOException {
		FileInputStream fis = new FileInputStream(fileName);
		return process(fis, fileName);
	}

	/**
	 * Simple regex based method to get the extension of a given file name.
	 * NOTE: This only fetches the last extension so foo.bar.ext will return
	 * '.ext' and foobar will return ''.
	 * 
	 * @param name
	 * @return Extension in given file name.
	 */
	protected static String getFileType(String name) {
		// TODO: Handle things like tar.gz ??
		String[] tokens = name.split("\\.(?=[^\\.]+$)");
		if (tokens.length > 1) {
			return "." + tokens[tokens.length - 1].toLowerCase();
		}
		return "";
	}

	/**
	 * Generate a hashmap of fingerprints for a give byte array using all
	 * configured algorithms. Default: SHA1, SHA-512, MD5.
	 * 
	 * @param bytes
	 *            A byte array whose content is to be fingerprinted.
	 * @return Hashmap of the form {algorithm:hash}
	 */
	public static Fingerprint fingerprint(byte[] bytes) {
		Fingerprint fingerprint = new Fingerprint();
		for (Algorithms algorithm : Algorithms.values()) {
			try {
				MessageDigest md = MessageDigest.getInstance(algorithm
						.toString().toUpperCase());
				fingerprint.put(algorithm,
						new String(Hex.encodeHex(md.digest(bytes))));
			} catch (NoSuchAlgorithmException e) {
				// Do nothing just skip
			}
		}
		return fingerprint;
	}

	/**
	 * Inner class that handles default return for non-configured file
	 * extensions. This accepts a default value of type V to return if key is
	 * not in the HashMap when using get.
	 * 
	 * @author abn
	 * 
	 * @param <K>
	 *            Key for type K
	 * @param <V>
	 *            Value of type V
	 */
	protected static class DefaultHashMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 1L;
		protected V defaultValue;

		public DefaultHashMap(V defaultValue) {
			super();
			this.defaultValue = defaultValue;
		}

		@Override
		public V get(Object k) {
			V v = super.get(k);
			return ((v == null) && !this.containsKey(k)) ? this.defaultValue
					: v;
		}
	}
}
