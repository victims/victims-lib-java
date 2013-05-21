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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * 
 * This implements a basic wrapper for the victims web service. Provides basic
 * interactions with the REST interface.
 * 
 * @author gcmurphy
 * @author abn
 * 
 */
public class VictimsService {
	protected String baseURI;
	protected String serviceEntry;

	/**
	 * Create a VictimsService instance with the default uri and entry point.
	 * 
	 * @throws MalformedURLException
	 */
	public VictimsService() throws MalformedURLException {
		this.baseURI = VictimsConfig.uri();
		this.serviceEntry = VictimsConfig.entry();

	}

	/**
	 * 
	 * Get all new records after a given date.
	 * 
	 * @param since
	 *            The date from when updates are required for.
	 * @return
	 * @throws IOException
	 */
	public RecordStream updates(Date since) throws IOException {
		return fetch(since, "update");
	}

	/**
	 * 
	 * Get all records removed after a given date.
	 * 
	 * @param since
	 *            The date from when removed records are required for.
	 * @return
	 * @throws IOException
	 */
	public RecordStream removed(Date since) throws IOException {
		return fetch(since, "remove");
	}

	/**
	 * 
	 * Work horse method that provides a {@link RecordStream} wraped from a
	 * response received from the server.
	 * 
	 * @param since
	 *            The date from when removed records are required for.
	 * @param type
	 *            The service type. To be used as ${base-uri}/${service}/%{type}
	 * @return
	 * @throws IOException
	 */
	protected RecordStream fetch(Date since, String type) throws IOException {
		SimpleDateFormat fmt = new SimpleDateFormat(VictimsRecord.DATE_FORMAT);
		String spec = FileUtils.getFile(serviceEntry, type, fmt.format(since))
				.toString();
		spec = FilenameUtils.normalize(spec, true);
		URL merged = new URL(new URL(baseURI), spec);
		return new RecordStream(merged.toString());
	}

	/**
	 * This provides a simple ObjectStream like implementation for wrapping
	 * streamed responses from the server.
	 * 
	 * @author abn
	 * 
	 */
	public static class RecordStream {
		protected JsonReader json;
		protected InputStream in;
		protected Gson gson;

		/**
		 * Create a record stream from a given URI.
		 * 
		 * @param uri
		 * @throws IOException
		 */
		public RecordStream(String uri) throws IOException {
			this(new URL(uri).openStream());
		}

		/**
		 * Create a record stream from a given input stream. we expect the
		 * stream to contain a json response of the for [{"fields" : {
		 * {@link VictimsRecord} String}}]
		 * 
		 * @param in
		 * @throws IOException
		 */
		public RecordStream(InputStream in) throws IOException {
			this.in = in;
			this.gson = new GsonBuilder().setDateFormat(
					VictimsRecord.DATE_FORMAT).create();
			this.json = new JsonReader(new InputStreamReader(in, "UTF-8"));
			this.json.beginArray();
		}

		/**
		 * 
		 * @return The next available {@link VictimsRecord}. If none available
		 *         returns null.
		 * @throws IOException
		 */
		public VictimsRecord getNext() throws IOException {
			if (hasNext()) {
				json.beginObject();
				json.nextName(); // discard fields
				VictimsRecord v = gson.fromJson(json, VictimsRecord.class);
				json.endObject();
				return v;
			}
			return null;
		}

		/**
		 * Checks if the internal {@link JsonReader} has any more json strings
		 * available. If not we end the json array and close the response
		 * stream.
		 * 
		 * @return <code>true</code> if more records can be read, else returns
		 *         <code>false</code>.
		 * @throws IOException
		 */
		public boolean hasNext() throws IOException {
			if (!json.hasNext()) {
				json.endArray();
				IOUtils.closeQuietly(in);
				return false;
			}
			return true;
		}
	}
}
