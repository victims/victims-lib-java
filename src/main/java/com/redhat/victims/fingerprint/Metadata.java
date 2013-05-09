package com.redhat.victims.fingerprint;

/*
 * #%L
 * This file is part of the victims-lib.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * 
 * @author gcmurphy
 * 
 */
@SuppressWarnings("serial")
public class Metadata extends HashMap<String, String> {

	/**
	 * Attempts to parse a pom.xml file.
	 * 
	 * @param is
	 *            An input stream containing the extracted POM file.
	 */
	public static Metadata fromPomProperties(InputStream is) {
		Metadata metadata = new Metadata();
		BufferedReader input = new BufferedReader(new InputStreamReader(is));
		try {
			String line;
			while ((line = input.readLine()) != null) {
				if (line.startsWith("#"))
					continue;
				String[] property = line.trim().split("=");
				if (property.length == 2)
					metadata.put(property[0], property[1]);
			}
		} catch (IOException e) {
			// Problems? Too bad!
		}
		return metadata;
	}

	/**
	 * Attempts to parse a MANIFEST.MF file from an input stream.
	 * 
	 * @param is
	 *            An input stream containing the extracted manifest file.
	 * @return HashMap of the type {atribute name : attribute value}.
	 * 
	 */
	public static Metadata fromManifest(InputStream is) {
		try {
			Manifest mf = new Manifest(is);
			return fromManifest(mf);

		} catch (IOException e) {
			// Problems? Too bad!
		}
		return new Metadata();
	}

	/**
	 * Extracts required attributes and their values from a {@link Manifest}
	 * object.
	 * 
	 * @param mf
	 *            A Manifest file.
	 * @return HashMap of the type {atribute name : attribute value}.
	 */
	public static Metadata fromManifest(Manifest mf) {
		Metadata metadata = new Metadata();
		final Attributes.Name[] attribs = { Attributes.Name.MANIFEST_VERSION,
				Attributes.Name.IMPLEMENTATION_TITLE,
				Attributes.Name.IMPLEMENTATION_URL,
				Attributes.Name.IMPLEMENTATION_VENDOR,
				Attributes.Name.IMPLEMENTATION_VENDOR_ID,
				Attributes.Name.IMPLEMENTATION_VERSION,
				Attributes.Name.MAIN_CLASS };
		for (Attributes.Name attrib : attribs) {
			Object o = mf.getMainAttributes().get(attrib);
			if (o != null) {
				metadata.put(attrib.toString(), o.toString());
			}
		}
		return metadata;
	}
}
