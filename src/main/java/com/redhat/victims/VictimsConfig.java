package com.redhat.victims;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.fingerprint.Algorithms;

/**
 * This class provides system property keys and default values for all available
 * Victims configurations.
 * 
 * @author abn
 * 
 */
public class VictimsConfig {
    protected static String DEFAULT_ALGORITHM_STRING = "SHA512";
    public static final HashMap<String, String> DEFAULT_PROPS = new HashMap<String, String>();

    static {
        DEFAULT_PROPS.put(Key.URI, "http://www.victi.ms/");
        DEFAULT_PROPS.put(Key.ENTRY, "service/");
        DEFAULT_PROPS.put(Key.ENCODING, "UTF-8");
        DEFAULT_PROPS.put(Key.HOME, FilenameUtils.concat(FileUtils
                .getUserDirectory().getAbsolutePath(), ".victims"));
        DEFAULT_PROPS.put(Key.ALGORITHMS, DEFAULT_ALGORITHM_STRING);
        DEFAULT_PROPS.put(Key.DB_DRIVER, VictimsDB.defaultDriver());
        DEFAULT_PROPS.put(Key.DB_USER, "victims");
        DEFAULT_PROPS.put(Key.DB_PASS, "victims");
    }

    public static Algorithms getDefaultAlgorithm() {
        return Algorithms.valueOf(DEFAULT_ALGORITHM_STRING);
    }

    /**
     * Return a configured value, or the default.
     * 
     * @param key
     * @return If configured, return the system property value, else return a
     *         default. If a default is also not available, returns
     *         <code>null</code> if no default is configured.
     */
    private static String getPropertyValue(String key) {
        String env = System.getProperty(key);
        if (env == null) {
            if (DEFAULT_PROPS.containsKey(key)) {
                return DEFAULT_PROPS.get(key);
            } else {
                return null;
            }
        }
        return env;
    }

    /**
     * 
     * @return Default encoding.
     */
    public static Charset charset() {
        String enc = getPropertyValue(Key.ENCODING);
        return Charset.forName(enc);
    }

    /**
     * Get the webservice base URI.
     * 
     * @return Returns the service URI value
     */
    public static String uri() {
        return getPropertyValue(Key.URI);
    }

    /**
     * Get the webservice entry point.
     * 
     * @return Returns the service entry path value
     */
    public static String entry() {
        return getPropertyValue(Key.ENTRY);
    }

    /**
     * Get a complete webservice uri by merging base and entry point.
     * 
     * @return Validated Service URI
     * @throws VictimsException
     */
    public static String serviceURI() throws VictimsException {
        URL merged;
        try {
            merged = new URL(new URL(uri()), entry());
            return merged.toString();
        } catch (MalformedURLException e) {
            throw new VictimsException(
                    "Invalid configuration for service URI.", e);
        }
    }

    /**
     * Get the configured cache directory. If the directory does not exist, it
     * will be created.
     * 
     * @return Validated Home Directory
     * @throws VictimsException if directory cannot be created
     */
    public static File home() throws VictimsException {
        File directory = new File(getPropertyValue(Key.HOME));
        if (!directory.exists()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                throw new VictimsException("Could not create home directory.",
                        e);
            }
        }
        return directory;
    }

    /**
     * 
     * 
     * @return Returns a list of valid algorithms to be used when fingerprinting. If not
     * specified, or if all values are illegal, all available algorithms are
     * used.
     */
    public static ArrayList<Algorithms> algorithms() {
        ArrayList<Algorithms> algorithms = new ArrayList<Algorithms>();
        for (String alg : getPropertyValue(Key.ALGORITHMS).split(",")) {
            alg = alg.trim();
            try {
                algorithms.add(Algorithms.valueOf(alg));
            } catch (Exception e) {
                // skip
            }
        }

        if (!algorithms.contains(getDefaultAlgorithm())) {
            algorithms.add(getDefaultAlgorithm());
        }

        return algorithms;
    }

    /**
     * Get the db driver class string in use.
     * 
     * @return DB Driver class 
     */
    public static String dbDriver() {
        return getPropertyValue(Key.DB_DRIVER);
    }

    /**
     * Get the db connection URL.
     * 
     * @return Validated DB ConnectionURL
     */
    public static String dbUrl() {
        String dbUrl = getPropertyValue(Key.DB_URL);
        if (dbUrl == null) {
            if (VictimsDB.Driver.exists(dbDriver())) {
                return VictimsDB.defaultURL(dbDriver());
            }
            return VictimsDB.defaultURL();
        }
        return dbUrl;
    }

    /**
     * Get the database user configured.
     * 
     * @return Database user value
     */
    public static String dbUser() {
        return getPropertyValue(Key.DB_USER);
    }

    /**
     * Get the database password configured.
     * 
     * @return Database password value
     */
    public static String dbPass() {
        return getPropertyValue(Key.DB_PASS);
    }

    /**
     * Is a force database update required.
     * 
     * @return Database Purge value
     */
    public static boolean forcedUpdate() {
        return Boolean.getBoolean(Key.DB_PURGE);
    }

    /**
     * A client option to check if it's cache has to be purged.
     * 
     * @return Cache purge value
     */
    public static boolean purgeCache() {
        return Boolean.getBoolean(Key.PURGE_CACHE);
    }

    /**
     * This class contains system property keys that are used to configure
     * victims.
     * 
     * @author abn
     * 
     */
    public static class Key {
        public static final String URI = "victims.service.uri";
        public static final String ENTRY = "victims.service.entry";
        public static final String ENCODING = "victims.encoding";
        public static final String HOME = "victims.home";
        public static final String PURGE_CACHE = "victims.cache.purge";
        public static final String ALGORITHMS = "victims.algorithms";
        public static final String DB_DRIVER = "victims.db.driver";
        public static final String DB_URL = "victims.db.url";
        public static final String DB_USER = "victims.db.user";
        public static final String DB_PASS = "victims.db.pass";
        public static final String DB_PURGE = "victims.db.purge";
    }

}
