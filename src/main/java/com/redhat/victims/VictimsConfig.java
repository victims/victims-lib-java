package com.redhat.victims;

import java.nio.charset.Charset;

public class VictimsConfig {
	private static final Charset ENCODING = Charset.forName("UTF-8");
	
	public static Charset charset(){
		return ENCODING;
	}
}
