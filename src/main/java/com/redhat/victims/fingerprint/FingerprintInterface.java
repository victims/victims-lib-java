package com.redhat.victims.fingerprint;

/*
 * #%L
 * This file is part of the victims-lib.
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

/**
 * The interface implemented by all implementations for handling fingerprinting.
 * 
 * @author abn
 * 
 */
public interface FingerprintInterface {

	/**
	 * Gets the hashmap of fingerprints
	 * 
	 * @return A hashmap of the for {algorithm:hash}
	 */
	public Fingerprint getFingerprint();

	/**
	 * Creates a 'record' with available info for the processed file. This
	 * includes fingerprints (all available algorithms), records of contents (if
	 * this is an archive), metadata (if available) etc.
	 * 
	 * @return A information record correspoding to the file processed.
	 */
	public Artifact getRecord();

}
