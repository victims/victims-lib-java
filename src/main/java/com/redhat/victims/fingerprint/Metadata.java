package com.redhat.victims.fingerprint;

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
