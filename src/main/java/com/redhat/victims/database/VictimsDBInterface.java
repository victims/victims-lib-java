package com.redhat.victims.database;

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

    /**
     * Returns the number of records that exist within the Victims database.
     *
     * @return The number of records in the Victims Database
     * @throws VictimsException
     */
    public int getRecordCount() throws VictimsException;
}
