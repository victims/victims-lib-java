package com.redhat.victims.mock;

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

import org.apache.commons.io.FileUtils;

import com.redhat.victims.VictimsConfig;

public class MockEnvironment {
	private static String TEST_CACHE = "victims.test.cache";

	public static void setUp(File updateResponse, File removeResponse) throws IOException {
		deleteCache();
		MockService.start(updateResponse, removeResponse);
		System.setProperty(VictimsConfig.Key.DB_FORCE_UPDATE, "true");
		System.setProperty(VictimsConfig.Key.URI, MockService.uri());
		System.setProperty(VictimsConfig.Key.CACHE, TEST_CACHE);
	}

	public static void tearDown() {
		MockService.stop();
		System.clearProperty(VictimsConfig.Key.DB_FORCE_UPDATE);
		System.clearProperty(VictimsConfig.Key.URI);
		System.clearProperty(VictimsConfig.Key.CACHE);
		deleteCache();
	}

	public static void deleteCache() {
		try {
			FileUtils.deleteDirectory(new File(TEST_CACHE));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
