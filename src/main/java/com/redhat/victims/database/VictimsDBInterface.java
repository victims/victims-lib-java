package com.redhat.victims.database;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.redhat.victims.VictimsException;
import com.redhat.victims.VictimsRecord;

public interface VictimsDBInterface {

	/**
	 * Returns when the database was successfully updated
	 * 
	 * @return A {@link Date} object indicating when the last update was
	 *         performed
	 * @throws VictimsException
	 */
	public Date lastUpdated() throws VictimsException;

	/**
	 * Synchronizes the database with the changes fetched from the victi.ms
	 * service.
	 * 
	 * @throws VictimsException
	 */
	public void synchronize() throws VictimsException;

	/**
	 * Given a {@link VictimsRecord}, finds all CVEs that the artifact is
	 * vulnerable to.
	 * 
	 * @param vr
	 * @return
	 * @throws VictimsException
	 */
	public HashSet<String> getVulnerabilities(VictimsRecord vr)
			throws VictimsException;

	/**
	 * 
	 * @param sha512
	 * @return
	 * @throws VictimsException
	 */
	public HashSet<String> getVulnerabilities(String sha512)
			throws VictimsException;

	/**
	 * For a given set of properties match all CVEs that match.
	 * 
	 * @param props
	 *            A set of key/value pairs representing all meta properties to
	 *            be matched.
	 * @return
	 * @throws VictimsException
	 */
	public HashSet<String> getVulnerabilities(HashMap<String, String> props)
			throws VictimsException;

}
