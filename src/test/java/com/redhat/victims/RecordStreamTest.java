package com.redhat.victims;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.redhat.victims.VictimsService.RecordStream;
import com.redhat.victims.fingerprint.Algorithms;

public class RecordStreamTest {
	private static final String TEST_RESPONSE = "testdata/service/test.response";
	private static final String TEST_SHA512 = "testdata/service/test.sha512";
	private static final String TEST_CVE = "testdata/service/test.cve";

	@Test
	public void testRecordStreamInputStream() throws IOException {
		FileInputStream fis = new FileInputStream(TEST_RESPONSE);
		String sha512 = FileUtils.readFileToString(new File(TEST_SHA512))
				.trim();
		String cve = FileUtils.readFileToString(new File(TEST_CVE)).trim();
		RecordStream rs = new RecordStream(fis);
		boolean hashFound = false;
		while (rs.hasNext()) {
			VictimsRecord vr = rs.getNext();
			if (vr.getHash(Algorithms.SHA512).equals(sha512)) {
				hashFound = true;
				boolean cveFound = false;
				for (String s : vr.cves) {
					if (cve.equals(s)) {
						cveFound = true;
						break;
					}
				}
				assertTrue(
						"Test hash was matched, but test cve was not found in record",
						cveFound);
				break;
			}
		}
		assertTrue(
				"Test hash was not found in any records generated from test response",
				hashFound);
	}
}
