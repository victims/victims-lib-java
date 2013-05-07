package com.redhat.victims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import com.redhat.victims.fingerprint.Algorithms;

public class VictimsScannerTest {
	private static final String JAR_FILE = "testdata/junit-4.11/junit-4.11.jar";
	private static final String JAR_SHA1 = JAR_FILE + ".sha1";
	private static final String JAR_JSON = JAR_FILE + ".json";
	private static final String POM_FILE = "testdata/junit-4.11/junit-4.11.pom";
	private static final String POM_SHA1 = POM_FILE + ".sha1";
	private static final String POM_JSON = POM_FILE + ".json";

	public void jsonTestStream(String inFile, String jsonFile)
			throws IOException {
		OutputStream os = new ByteArrayOutputStream();
		File test = new File(inFile);
		String expected = FileUtils.readFileToString(new File(jsonFile)).trim();
		try {
			VictimsScanner.scan(test.getAbsolutePath(), os);
			String result = os.toString().trim();
			assertEquals("Scanned json string not equal to expected", expected,
					result);
		} catch (IOException e) {
			fail("Could not scan file: " + inFile);
		}
		os.flush();
		os.close();
	}

	public void jsonTestArray(String inFile, String jsonFile)
			throws IOException {
		ArrayList<VictimsRecord> records = new ArrayList<VictimsRecord>();
		File test = new File(inFile);
		String expected = FileUtils.readFileToString(new File(jsonFile)).trim();
		int expected_count = 1;
		try {
			VictimsScanner.scan(test.getAbsolutePath(), records);
			int result_count = records.size();
			assertTrue(String.format("Expected %d records, found %d.",
					expected_count, result_count), result_count == 1);
			// the output stream write adds a line to split records
			String result = records.get(0).toString();
			assertEquals("Scanned json string not equal to expected", expected,
					result);
		} catch (IOException e) {
			fail("Could not scan file: " + inFile);
		}
	}

	public void valueTest(String inFile, String sha1File, Boolean useStream)
			throws IOException {
		File test = new File(inFile);
		InputStream in = null;
		if (useStream) {
			in = new FileInputStream(test);
		}
		String expected = FileUtils.readFileToString(new File(sha1File)).trim();
		int expected_count = 1;
		try {
			ArrayList<VictimsRecord> records = null;
			if (useStream) {
				records = VictimsScanner.getRecords(in,
						FilenameUtils.getBaseName(inFile));
			} else {
				records = VictimsScanner.getRecords(test.getAbsolutePath());

			}
			int result_count = records.size();
			assertTrue(String.format("Expected %d records, found %d.",
					expected_count, result_count), result_count == 1);
			String result = records.get(0).getHash(Algorithms.SHA1).trim();
			assertEquals("SHA1 mismatch", expected, result);
		} catch (IOException e) {
			fail("Could not scan file: " + inFile);
		}
	}

	@Test
	public void testScanStringOutputStreamJar() throws IOException {
		jsonTestStream(JAR_FILE, JAR_JSON);
		jsonTestStream(POM_FILE, POM_JSON);
	}

	@Test
	public void testScanStringArrayListOfVictimsRecord() throws IOException {
		jsonTestArray(JAR_FILE, JAR_JSON);
		jsonTestArray(POM_FILE, POM_JSON);
	}

	@Test
	public void testGetRecordsString() throws IOException {
		valueTest(JAR_FILE, JAR_SHA1, false);
		valueTest(POM_FILE, POM_SHA1, false);
	}

	@Test
	public void testGetRecordsInputStreamString() throws IOException {
		valueTest(JAR_FILE, JAR_SHA1, true);
		valueTest(POM_FILE, POM_SHA1, true);
	}

}
