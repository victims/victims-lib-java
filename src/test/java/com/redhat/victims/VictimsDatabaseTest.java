package com.redhat.victims;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;
import com.redhat.victims.mock.MockEnvironment;

public class VictimsDatabaseTest {
	private static final String TEST_SHA512 = "testdata/service/test.sha512";
	private static final String TEST_CVE = "testdata/service/test.cve";

	@BeforeClass
	public static void setUp() throws IOException {
		MockEnvironment.setUp();
	}

	@AfterClass
	public static void tearDown() {
		MockEnvironment.tearDown();
	}

	@Test
	public void testSynchronize() throws VictimsException, IOException {
		VictimsDBInterface vdb = VictimsDB.db();
		vdb.synchronize();
		String sha512 = FileUtils.readFileToString(new File(TEST_SHA512))
				.trim();
		String cve = FileUtils.readFileToString(new File(TEST_CVE)).trim();
		assertTrue("Synchronized DB does not contain expected hash.", vdb
				.getVulnerabilities(sha512).contains(cve));
	}
}
