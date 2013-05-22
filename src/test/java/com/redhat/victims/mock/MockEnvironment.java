package com.redhat.victims.mock;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.redhat.victims.VictimsConfig;

public class MockEnvironment {
	private static String TEST_CACHE = "victims.test.cache";

	public static void setUp() throws IOException {
		MockService.start();
		System.setProperty(VictimsConfig.Key.DB_FORCE_UPDATE, "true");
		System.setProperty(VictimsConfig.Key.URI, MockService.uri());
		System.setProperty(VictimsConfig.Key.CACHE, TEST_CACHE);
	}

	public static void tearDown() {
		MockService.stop();
		System.clearProperty(VictimsConfig.Key.DB_FORCE_UPDATE);
		System.clearProperty(VictimsConfig.Key.URI);
		System.clearProperty(VictimsConfig.Key.CACHE);
		
		try {
			FileUtils.deleteDirectory(new File(TEST_CACHE));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
