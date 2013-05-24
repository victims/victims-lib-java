package com.redhat.victims;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.stream.MalformedJsonException;
import com.redhat.victims.VictimsService.RecordStream;

public class VictimsServiceTest {

	private String enableSNIExtension = "false";

	public VictimsServiceTest() {
		this.enableSNIExtension = System.getProperty("jsse.enableSNIExtension");
	}

	@Before
	public void setUp() throws Exception {
		System.setProperty("jsse.enableSNIExtension", "false");
	}

	@After
	public void tearDown() throws Exception {
		if (enableSNIExtension != null) {
			System.setProperty("jsse.enableSNIExtension",
					enableSNIExtension.toString());
		}
	}

	@Test
	public void testUpdates() throws Exception {
		Date start = new Date();
		try {
			RecordStream rs = new VictimsService().updates(start);
			while (rs.hasNext()) {
				// if there was a response, test it
				try {
					VictimsRecord vr = rs.getNext();
					try {
						assertTrue(vr.date.after(start));
					} catch (Exception e) {
						fail("A response with invalid date was received.");
					}
				} catch (Exception e) {
					fail("Could not receive a record from the serivce!");
				}
				break;
			}
			// we managed to make a valid response, get a valid response.
		} catch (IOException e) {
			// Allow this test to succeed if there was an exception in
			// communicating with the server.
			if (!(e instanceof MalformedJsonException)
					&& !e.getMessage().contains(
							"Server returned HTTP response code")) {
				throw e;
			} else {
				System.out.println("Skipping services update test: "
						+ e.getMessage());
			}
		}
	}
}
