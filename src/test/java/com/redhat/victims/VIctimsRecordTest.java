package com.redhat.victims;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class VIctimsRecordTest {
	@Test
	public void testEquals() throws IOException {
		String jstr = FileUtils.readFileToString(new File(Resources.JAR_JSON))
				.trim();
		VictimsRecord vr = VictimsRecord.fromJSON(jstr);
		assertTrue("Equality check for Victims Record failed.", vr.equals(vr));
	}

}
