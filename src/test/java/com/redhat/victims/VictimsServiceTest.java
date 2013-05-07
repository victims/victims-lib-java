package com.redhat.victims;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.victims.VictimsService.RecordStream;

public class VictimsServiceTest{

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
		if (enableSNIExtension != null){
			System.setProperty("jsse.enableSNIExtension", enableSNIExtension.toString());			
		}
	}
	
	@Test
	public void testUpdates() throws Exception {
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Date start = sdf.parse("01/01/2010");
		RecordStream rs = new VictimsService().updates(start);
		while (rs.hasNext()) {
			try {
				VictimsRecord vr = rs.getNext();				
				try {
					assertTrue(vr.date.after(start));				
				} catch (Exception e) {
					fail("A response with invalid date was received.");
				}
			} catch(Exception e){
				fail("Could not receive a record from the serivce!");
			}
			break;
		}
	}
}