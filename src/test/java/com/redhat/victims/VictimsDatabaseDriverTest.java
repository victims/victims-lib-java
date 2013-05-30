package com.redhat.victims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.victims.VictimsService.RecordStream;
import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;
import com.redhat.victims.fingerprint.Algorithms;
import com.redhat.victims.mock.MockEnvironment;

public class VictimsDatabaseDriverTest {
	protected static final String TEST_RESPONSE = "testdata/service/test.response";
	protected static final String TEST_SHA512 = "testdata/service/test.sha512";
	protected static final String TEST_CVE = "testdata/service/test.cve";
	
	protected static final String TEST_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	protected static VictimsDBInterface vdb;
	
	public static String mockDerbyUrl() {
		String cache = "";
		try {
			cache = VictimsConfig.cache().toString();
		} catch (IOException e) {
		}
		return String.format("jdbc:derby:%s;create=true",
				FilenameUtils.concat(cache, "victims"));
	}

	@BeforeClass
	public static void setUp() throws IOException, VictimsException {
		File updateResponse = new File(TEST_RESPONSE);
		MockEnvironment.setUp(updateResponse, null);
		sync();
	}

	@AfterClass
	public static void tearDown() {
		MockEnvironment.tearDown();
	}

	public static void sync() throws VictimsException {
		System.setProperty(VictimsConfig.Key.DB_DRIVER, TEST_DRIVER);
		System.setProperty(VictimsConfig.Key.DB_URL, mockDerbyUrl());
		vdb = VictimsDB.db();
		vdb.synchronize();
	}

	@Test
	public void testSynchronize() throws VictimsException, IOException {
		String sha512 = FileUtils.readFileToString(new File(TEST_SHA512))
				.trim();
		String cve = FileUtils.readFileToString(new File(TEST_CVE)).trim();
		assertTrue("Synchronized DB does not contain expected hash.", vdb
				.getVulnerabilities(sha512).contains(cve));
	}

	private HashSet<String> getVulnerabilities(VictimsRecord vr)
			throws VictimsException {
		return vdb.getVulnerabilities(vr);
	}

	@Test
	public void testVulnerabilities() throws IOException, VictimsException {
		FileInputStream fin = new FileInputStream(TEST_RESPONSE);
		RecordStream rs = new RecordStream(fin);
		VictimsRecord vr;
		while (rs.hasNext()) {
			vr = rs.getNext();
			if (vr.getHashes(Algorithms.SHA512).size() > 0) {
				HashSet<String> cves = getVulnerabilities(vr);
				vr.hash = "0";
				HashSet<String> result = getVulnerabilities(vr);
				assertEquals("Unexpected number of CVEs", cves.size(),
						result.size());
				for (String cve : cves) {
					assertTrue(String.format(
							"%s was expected, but was not found in result.",
							cve), result.contains(cve));
				}
				break;
			}
		}
	}

	@Test
	public void testResync() throws VictimsException {
		VictimsDBInterface vdb = VictimsDB.db();
		vdb.synchronize();
	}

	@Test(expected = VictimsException.class)
	public void testCustomDriverNoUrl() throws VictimsException {
		String old = VictimsConfig.dbUrl();
		System.clearProperty(VictimsConfig.Key.DB_URL);
		System.out.println(old);
		System.out.println(System.getProperty(VictimsConfig.Key.DB_URL));
		try {
			VictimsDBInterface vdb = VictimsDB.db();
		} finally {
			System.setProperty(VictimsConfig.Key.DB_URL, old);
		}

	}
}
