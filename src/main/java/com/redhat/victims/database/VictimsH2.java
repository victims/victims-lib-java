package com.redhat.victims.database;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import com.redhat.victims.VictimsConfig;

public class VictimsH2 implements DatabaseType {

	public String driver() {
		return "org.h2.Driver";
	}

	public String url() {
		String cache = "";
		try {
			cache = VictimsConfig.cache().toString();
		} catch (IOException e) {
			// Ignore and use cwd
		}
		return String.format("jdbc:h2:%s;MVCC=true",
				FilenameUtils.concat(cache, "victims"));
	}

}
