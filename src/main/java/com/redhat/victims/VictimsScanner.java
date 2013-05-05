package com.redhat.victims;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.Processor;

public class VictimsScanner {

	private static void scanArtifact(Artifact artifact, OutputStream os)
			throws IOException {
		ArrayList<Artifact> embedded = artifact.embedded();
		if (embedded != null) {
			for (Artifact eartifact : artifact.embedded()) {
				scanArtifact(eartifact, os);
			}
		}
		VictimsRecord vr = new VictimsRecord(artifact);
		String line = vr.toString();
		line += "\n";
		os.write(line.getBytes());
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
	private static void scanFile(File file, OutputStream os) throws IOException {
		File f = file;
		String path = f.getAbsolutePath();
		Artifact artifact = Processor.process(path);
		scanArtifact(artifact, os);
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
	private static void scanDir(File dir, OutputStream os) throws IOException {
		Collection<File> files = FileUtils.listFiles(dir, new RegexFileFilter(
				"^(.*?)\\.jar"), DirectoryFileFilter.DIRECTORY);
		Iterator<File> fi = files.iterator();
		while (fi.hasNext()) {
			scanFile(fi.next(), os);
		}
	}

	/**
	 * 
	 * Iteratively finds all jar files if source is a directory and scans them
	 * or if a file , scan it, producing {@link VictimsRecord}. The string
	 * values of the resulting records will be written to the specified output
	 * stream.
	 * 
	 * @param source
	 * @param os
	 * @throws IOException
	 */
	public static void scan(String source, OutputStream os) throws IOException {
		File f = new File(source);
		if (f.isDirectory()) {
			scanDir(f, os);
		} else if (f.isFile()) {
			scanFile(f, os);
		}
	}
}
