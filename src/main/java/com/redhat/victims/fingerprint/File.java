package com.redhat.victims.fingerprint;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Implements handing of simple files for fingerprinting.
 * 
 * @author abn
 * 
 */
public class File extends AbstractFile {

	/**
	 * 
	 * @param bytes
	 *            Input file as a byte array.
	 * @param fileName
	 *            The name of the file being processed.
	 */
	public File(byte[] bytes, String fileName) {
		this.fileName = fileName;
		this.fingerprint = Processor.fingerprint(bytes);
	}

	/**
	 * 
	 * @param is
	 *            The file as an input stream.
	 * @param fileName
	 *            The name of the file provided by the stream.
	 * @throws IOException
	 */
	public File(InputStream is, String fileName) throws IOException {
		this(IOUtils.toByteArray(is), fileName);
	}

}
