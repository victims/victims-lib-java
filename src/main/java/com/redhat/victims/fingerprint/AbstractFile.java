package com.redhat.victims.fingerprint;

/**
 * Provides an abstract class for all file types that can be fingerprinted.
 * 
 * @author abn
 * 
 */
public abstract class AbstractFile implements FingerprintInterface {
	protected Fingerprint fingerprint = null;
	protected String fileName = null;

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return the fingerprints
	 */
	public Fingerprint getFingerprint() {
		return fingerprint;
	}

	public Artifact getRecord() {
		Artifact result = new Artifact();
		result.put(Key.FILENAME, fileName);
		result.put(Key.FILETYPE, Processor.getFileType(fileName));
		result.put(Key.FINGERPRINT, fingerprint);
		return result;
	}
}
