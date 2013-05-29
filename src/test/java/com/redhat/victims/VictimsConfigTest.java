package com.redhat.victims;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import com.redhat.victims.fingerprint.Algorithms;

public class VictimsConfigTest {

	@Test
	public void testAlgorithms() {
		// test defaults
		ArrayList<Algorithms> results = VictimsConfig.algorithms();
		for (Algorithms alg : Algorithms.values()) {
			assertTrue(
					"Default algorithm configuration do not contain all defined algorithms.",
					results.contains(alg));
		}

		// test legal set
		System.setProperty(VictimsConfig.Key.ALGORITHMS, "SHA512");
		results = VictimsConfig.algorithms();
		assertTrue("Algorithms were not set correctly.",
				results.contains(Algorithms.SHA512) && results.size() == 1);

		// test legal with illegal set
		System.setProperty(VictimsConfig.Key.ALGORITHMS, "MD1, SHA512");
		results = VictimsConfig.algorithms();
		assertTrue("Algorithms were not set correctly.",
				results.contains(Algorithms.SHA512) && results.size() == 1);

		// test all invalids
		System.setProperty(VictimsConfig.Key.ALGORITHMS, "MD1,DUMMY");
		results = VictimsConfig.algorithms();
		for (Algorithms alg : Algorithms.values()) {
			assertTrue(
					"All algorithms are invalid case did not return all defined algorithms.",
					results.contains(alg));
		}
	}

}
