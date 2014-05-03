package com.redhat.victims.mock;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;

import com.redhat.victims.Resources;

public class MockZipFile extends ZipArchiveOutputStream {

	private final File zipfile;

	private static File createTempFile() throws IOException {
		File file = File.createTempFile("victims.test", ".zip",
				new File(System.getProperty("java.io.tmpdir")));
		return file;
	}

	public MockZipFile(File zipfile) throws IOException {
		super(zipfile);
		this.zipfile = zipfile;

	}

	public MockZipFile() throws IOException {
		this(createTempFile());
	}

	public String getAbsolutePath() {
		return zipfile.getAbsolutePath();
	}

	public void addFile(String entryName, String sourceFile) throws IOException {
		ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
		File file = new File(sourceFile);
		entry.setSize(FileUtils.sizeOf(file));
		this.putArchiveEntry(entry);
		this.write(FileUtils.readFileToByteArray(file));
		this.closeArchiveEntry();
	}

	public void populateDefaults() throws IOException {
		addFile("resource.jar", Resources.JAR_FILE);
		addFile("resource.jar.sha1", Resources.JAR_SHA1);
		addFile("resource.jar.json", Resources.JAR_JSON);
	}
}
