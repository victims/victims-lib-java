package com.redhat.victims.fingerprint;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;

/**
 * Implements handing of Archive files for fingerprinting.
 * 
 * @author abn
 * 
 */
public class JarFile extends AbstractFile {
	/**
	 * Indicates if archive contents get processed. Default is true.
	 */
	public static final boolean RECURSIVE = true;
	private static final int BUFFER = 2048;

	protected ArrayList<Object> contents;
	protected ArrayList<Object> embedded;
	protected HashMap<String, Metadata> metadata;
	protected JarInputStream jis;

	/**
	 * Synchronized method of adding metadata
	 * 
	 * @param filename
	 * @param md
	 */
	protected synchronized void putMetadata(String filename, Metadata md) {
		metadata.put(filename, md);
	}

	/**
	 * Synchronized method for adding a new conent
	 * 
	 * @param record
	 * @param filename
	 */
	protected synchronized void addContent(Artifact record, String filename) {
		if (record != null) {
			if (filename.endsWith(".jar")) {
				// this is an embedded archive
				embedded.add(record);
			} else {
				contents.add(record);
			}
		}
	}

	/**
	 * Threadable task for processing a given {@link Content} file.
	 * 
	 * @param file
	 */
	protected void processContent(Content file) {
		// Handle metadata/special cases
		String lowerCaseFileName = file.name.toLowerCase();
		if (lowerCaseFileName.endsWith("pom.properties")) {
			// handle pom properties files
			InputStream is = new ByteArrayInputStream(file.bytes);
			Metadata md = Metadata.fromPomProperties(is);
			putMetadata(file.name, md);
		}

		// This is separate as we may or may not want to fingerprint
		// all files.
		if (RECURSIVE) {
			Artifact record = Processor.process(file.bytes, file.name, true);
			addContent(record, file.name);
		}
	}

	/**
	 * Helper method to sumit a new threaded tast to a given executor.
	 * 
	 * @param executor
	 * @param file
	 */
	protected void submitJob(ExecutorService executor, Content file) {
		// lifted from http://stackoverflow.com/a/5853198/1874604
		class OneShotTask implements Runnable {
			Content file;

			OneShotTask(Content file) {
				this.file = file;
			}

			public void run() {
				processContent(file);
			}
		}
		// we do not care about Future
		executor.submit(new OneShotTask(file));
	}

	/**
	 * 
	 * @param bytes
	 *            A byte array containing the bytes of the file
	 * @param fileName
	 *            Name of the file being provided as bytes
	 * @throws IOException
	 */
	public JarFile(byte[] bytes, String fileName) throws IOException {
		this.contents = new ArrayList<Object>();
		this.embedded = new ArrayList<Object>();
		this.metadata = new HashMap<String, Metadata>();
		this.fileName = fileName;
		this.jis = new JarInputStream(new ByteArrayInputStream(bytes));

		// process contents
		Content file;
		ExecutorService executor = Executors.newCachedThreadPool();
		while ((file = getNextFile()) != null) {
			submitJob(executor, file);
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// probably a bad idea wrap as an IO Exception for now
			throw new IOException(
					"There was an issue while waiting for the ExecutorService "
							+ "to terminate.", e);
		}

		// Process the metadata from the manifest if available
		Manifest mf = jis.getManifest();
		if (mf != null) {
			metadata.put("MANIFEST.MF", Metadata.fromManifest(mf));
		}

		this.fingerprint = Processor.fingerprint(bytes);
		jis.close();
	}

	/**
	 * 
	 * @param fileName
	 *            Name of the file being process, expected as path on disk.
	 * @throws IOException
	 */
	public JarFile(String fileName) throws IOException {
		this(new FileInputStream(fileName), fileName);
	}

	/**
	 * 
	 * @param is
	 *            The file as an input stream.
	 * @param fileName
	 *            The name of the file provided by the stream.
	 * @throws IOException
	 */
	public JarFile(InputStream is, String fileName) throws IOException {
		this(IOUtils.toByteArray(is), fileName);
	}

	public Artifact getRecord() {
		Artifact result = super.getRecord();
		result.put(Key.CONTENT, contents);
		result.put(Key.METADATA, metadata);
		result.put(Key.EMBEDDED, embedded);
		return result;
	}

	/**
	 * Get the next file as a {@link Content} in this archive.
	 * 
	 * @return
	 * @throws IOException
	 */
	protected Content getNextFile() throws IOException {
		JarEntry entry;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            if ((entry = jis.getNextJarEntry()) != null) {

                byte[] data = new byte[BUFFER];
                int size;
                while ((size = jis.read(data, 0, data.length)) != -1) {
                    bos.write(data, 0, size);
                }
                Content file = new Content(entry.getName(), bos.toByteArray());
                bos.close();
                return file;
            }
        }
        catch (IOException io) {
            io.printStackTrace();
            bos.close();
            System.out.println(new String(bos.toByteArray()));
        }
		return null;
	}

	/**
	 * Content -- Inner class for use by {@link ArchiveFile}. This is used to
	 * group name of file extracted in memory and the corresponding bytes that
	 * were read.
	 * 
	 * @author abn
	 * 
	 */
	protected static class Content {
		public String name;
		public byte[] bytes;

		public Content(String name, byte[] bytes) {
			this.name = name;
			this.bytes = bytes;
		}
	}
}
