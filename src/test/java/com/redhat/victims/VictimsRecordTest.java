package com.redhat.victims;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.redhat.victims.fingerprint.Artifact;
import com.redhat.victims.fingerprint.Key;

public class VictimsRecordTest {
	@Test
	public void testEquals() throws IOException {
		String jstr = FileUtils.readFileToString(new File(Resources.JAR_JSON))
				.trim();
		VictimsRecord vr = VictimsRecord.fromJSON(jstr);
		assertTrue("Equality check for Victims Record failed.", vr.equals(vr));
	}

	@Test
	public void testContainsAll() throws IOException {
		String jstr = FileUtils.readFileToString(new File(Resources.JAR_JSON))
				.trim();
		VictimsRecord rec1 = VictimsRecord.fromJSON(jstr);
		VictimsRecord rec2 = VictimsRecord.fromJSON(jstr);

		// Before the correct @Override this wouldn't even work..
		ArrayList<VictimsRecord> a = new ArrayList<VictimsRecord>(); 
		a.add(rec1);

		ArrayList<VictimsRecord> b = new ArrayList<VictimsRecord>();
		b.add(rec2);

		assertTrue("Contains all works at basic level", a.containsAll(b));

		String jsonData = FileUtils.readFileToString(new File(Resources.TEST_JAR));
		VictimsRecord extra = VictimsRecord.fromJSON(jsonData);
		b.add(extra);
		assertTrue("Negative case with invalid record data", !a.containsAll(b));

	
	}

}
