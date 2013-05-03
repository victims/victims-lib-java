package com.redhat.victims.fingerprint;

public enum Algorithms {
	MD5, SHA1, 
	
	SHA512 {
		public String toString() {
			return "SHA-512";
		}
	}
}
