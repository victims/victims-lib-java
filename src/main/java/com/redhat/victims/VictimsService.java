package com.redhat.victims;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
	protected static final String DEFAULT_URI = "https://victims-websec.rhcloud.com";
	protected static final String DEFAULT_ENTRY_POINT = "/service/v2";
	protected String uri;
	protected String entry_point;

	/**
	 * Create a VictimsService instance with the default uri and entry point.
	 */
	public VictimsService() {
		this.uri = DEFAULT_URI;
		this.entry_point = DEFAULT_ENTRY_POINT;

	}

	/**
	 * Create a VictimsService instance with the specified uri and entry point.
	 * 
	 * @param uri
	 *            The base uri pointint to a victims-web service.
	 * @param entry_point
	 *            The entry point to be used for this service.
	 */
	public VictimsService(String uri, String entry_point) {
		this.uri = uri;
		this.entry_point = entry_point;
	}

	/**
	 * Create a VictimsService instance with the specified uri and default entry
	 * point.
	 * 
	 * @param uri
	 *            The base uri pointint to a victims-web service.
	 */
	public VictimsService(String uri) {
		this(uri, DEFAULT_ENTRY_POINT);
	}

	/**
	 * 
	 * @return
	 */
	public String getURI() {
		return this.uri;
	}

	/**
	 * 
	 * @return
	 */
	public String getEntryPoint() {
		return this.entry_point;
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
	protected RecordStream fetch(Date since, String type)
			throws IOException {
		SimpleDateFormat fmt = new SimpleDateFormat(VictimsRecord.DATE_FORMAT);
		String uri = String.format("%s/%s/%s/%s", this.uri, this.entry_point,
				type, fmt.format(since));
		return new RecordStream(uri);
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
				System.out.println(v.toString());
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

	public static void main(String[] args) throws IOException, ParseException {
		// DEBUG CODE
		// jdk 1.7 does not like name errors
		System.setProperty("jsse.enableSNIExtension", "false");
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		RecordStream rs = new VictimsService().updates(sdf.parse("01/01/2010"));
		while (rs.hasNext()) {
			System.out.println(rs.getNext());
		}

	}

}
