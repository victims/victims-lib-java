package com.redhat.victims.database;

import java.util.ArrayList;
import java.util.Date;

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
	public ArrayList<String> getVulnerabilities(VictimsRecord vr)
			throws VictimsException;

	/**
	 * 
	 * @param sha512
	 * @return
	 * @throws VictimsException
	 */
	public ArrayList<String> getVulnerabilities(String sha512)
			throws VictimsException;

}
