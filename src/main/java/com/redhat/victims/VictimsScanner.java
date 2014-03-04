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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.Processor;

public class VictimsScanner {

	/**
	 * Scans a provided {@link Artifact} and writes the resulting
	 * {@link VictimsRecord} to the provided {@link VictimsOutputStream}.
	 * 
	 * @param artifact
	 * @param os
	 * @throws IOException
	 */
	private static void scanArtifact(Artifact artifact, VictimsOutputStream vos)
			throws IOException {
		ArrayList<Artifact> embedded = artifact.embedded();
		if (embedded != null) {
			for (Artifact eartifact : artifact.embedded()) {
				scanArtifact(eartifact, vos);
			}
		}
		VictimsRecord record = new VictimsRecord(artifact);
		vos.write(record);
	}

	/**
	 * 
	 * Scans a provided {@link File} producing {@link VictimsRecord}. The string
	 * values of the resulting records will be written to the specified output
	 * stream.
	 * 
	 * @param file
	 * @param os
	 * @throws IOException
	 */
	private static void scanFile(File file, VictimsOutputStream vos)
			throws IOException {
		File f = file;
		String path = f.getAbsolutePath();
		Artifact artifact = Processor.process(path);
		scanArtifact(artifact, vos);
	}

	/**
	 * 
	 * Iteratively finds all jar files in a given directory and scans them,
	 * producing {@link VictimsRecord}. The string values of the resulting
	 * records will be written to the specified output stream.
	 * 
	 * @param dir
	 * @param os
	 * @throws IOException
	 */
	private static void scanDir(File dir, VictimsOutputStream vos)
			throws IOException {
		Collection<File> files = FileUtils.listFiles(dir, new RegexFileFilter(
				"^(.*?)\\.jar"), DirectoryFileFilter.DIRECTORY);
		Iterator<File> fi = files.iterator();
		while (fi.hasNext()) {
			scanFile(fi.next(), vos);
		}
	}

	/**
	 * 
	 * Iteratively finds all jar files if source is a directory and scans them
	 * or if a file , scan it, producing {@link VictimsRecord}. This is then
	 * written to the provided {@link VictimsOutputStream}. Embedded jars are a
	 * record on their own.
	 * 
	 * @param source
	 * @param os
	 * @throws IOException
	 */
	private static void scanSource(String source, VictimsOutputStream vos)
			throws IOException {
		File f = new File(FilenameUtils.getFullPath(source));
		if (f.isDirectory()) {
			scanDir(f, vos);
		} else if (f.isFile()) {
			scanFile(f, vos);
		}
	}

	/**
	 * Iteratively finds all jar files if source is a directory and scans them
	 * or if a file , scan it. The string values of the resulting records will
	 * be written to the specified output stream. Embedded jars are a record on
	 * their own.
	 * 
	 * @param source
	 * @param os
	 * @throws IOException
	 */
	public static void scan(String source, OutputStream os) throws IOException {
		scanSource(source, new StringOutputStream(os));
	}

	/**
	 * Iteratively finds all jar files if source is a directory and scans them
	 * or if a file , scan it. The {@link VictimsRecord}s produced are added
	 * into the provided {@link ArrayList}.Embedded jars are a record on their
	 * own.
	 * 
	 * @param source
	 * @param results
	 * @throws IOException
	 */
	public static void scan(String source, ArrayList<VictimsRecord> results)
			throws IOException {
		scanSource(source, new ArrayOutputStream(results));
	}

	/**
	 * Iteratively finds all jar files if source is a directory and scans them
	 * or if a file , scan it.
	 * 
	 * @param source
	 * @return An {@link ArrayList} of {@link VictimsRecord}s derrived from the
	 *         source. Embedded jars are a record on their own.
	 * @throws IOException
	 */
	public static ArrayList<VictimsRecord> getRecords(String source)
			throws IOException {
		ArrayList<VictimsRecord> records = new ArrayList<VictimsRecord>();
		scan(source, records);
		return records;

	}

	/**
	 * Scan a file from a given {@link InputStream}.
	 * 
	 * @param in
	 * @param filename
	 * @return An {@link ArrayList} of {@link VictimsRecord}s derrived from the
	 *         source. Embedded jars are a record on their own.
	 * @throws IOException
	 */
	public static ArrayList<VictimsRecord> getRecords(InputStream in,
			String filename) throws IOException {
		ArrayList<VictimsRecord> records = new ArrayList<VictimsRecord>();
		Artifact artifact = Processor.process(in, filename);
		scanArtifact(artifact, new ArrayOutputStream(records));
		return records;

	}

	/**
	 * Inner class to abstract away {@link OutputStream} and {@link ArrayList}
	 * for the purposes of the scanner.
	 * 
	 * @author abn
	 * 
	 */
	private abstract static class VictimsOutputStream {
		public abstract void write(VictimsRecord record) throws IOException;
	}

	/**
	 * Extends {@link VictimsOutputStream} to handle {@link ArrayList} of
	 * {@link VictimsRecord}.
	 * 
	 * @author abn
	 * 
	 */
	private static class ArrayOutputStream extends VictimsOutputStream {

		private ArrayList<VictimsRecord> records;

		public ArrayOutputStream(ArrayList<VictimsRecord> records) {
			this.records = records;
		}

		public void write(VictimsRecord record) {
			this.records.add(record);
		}
	}

	/**
	 * Extends {@link VictimsOutputStream} to handle {@link OutputStream}s.
	 * 
	 * @author abn
	 */
	private static class StringOutputStream extends VictimsOutputStream {

		private OutputStream os;

		public StringOutputStream(OutputStream os) {
			this.os = os;
		}

		public void write(VictimsRecord record) throws IOException {
			String line = record.toString();
			line += "\n";
			os.write(line.getBytes(VictimsConfig.charset()));
		}
	}
}
