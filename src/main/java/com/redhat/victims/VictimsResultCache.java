package com.redhat.victims;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

public class VictimsResultCache {
	protected static String CACHE_NAME = "lib.results.cache";
	protected static String location = null;
	protected static boolean purged = false;

	protected void create(File cache) throws IOException {
		FileUtils.forceMkdir(cache);
	}

	public void purge() throws IOException {
		File cache = new File(location);
		if (cache.exists()) {
			FileUtils.deleteDirectory(cache);
		}
		create(cache);
	}

	public VictimsResultCache() throws IOException {
		location = FilenameUtils.concat(VictimsConfig.home().toString(),
				CACHE_NAME);
		File cache = new File(location);
		if (!cache.exists()) {
			create(cache);
		} else if (VictimsConfig.purgeCache() && !purged) {
			purge();
			purged = true;
		}
	}

	public boolean exists(String hash) {
		return FileUtils.getFile(location, hash).exists();
	}

	public void delete(String hash) throws IOException {
		FileUtils.forceDelete(FileUtils.getFile(location, hash));
	}

	public void add(String hash, Collection<String> cves) throws IOException {
		if (exists(hash)) {
			delete(hash);
		}

		String result = "";
		if (cves != null) {
			result = StringUtils.join(cves, ",");
		}

		FileOutputStream fos = new FileOutputStream(FileUtils.getFile(location,
				hash));
		try {
			fos.write(result.getBytes());			
		} finally {
			fos.close();			
		}

	}

	public HashSet<String> get(String hash) throws IOException {
		String result = FileUtils.readFileToString(
				FileUtils.getFile(location, hash)).trim();
		HashSet<String> cves = new HashSet<String>();
		for(String cve : StringUtils.split(result, ",")) {
			cves.add(cve);
		}
		return cves;
	}

}
