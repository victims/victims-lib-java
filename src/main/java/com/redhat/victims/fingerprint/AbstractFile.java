package com.redhat.victims.fingerprint;

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

/**
 * Provides an abstract class for all file types that can be fingerprinted.
 * 
 * @author abn
 * 
 */
public abstract class AbstractFile implements FingerprintInterface {
    protected Fingerprint fingerprint = null;
    protected String fileName = null;

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return the fingerprints
     */
    public Fingerprint getFingerprint() {
        return fingerprint;
    }

    public Artifact getRecord() {
        Artifact result = new Artifact();
        result.put(Key.FILENAME, fileName);
        result.put(Key.FILETYPE, Processor.getFileType(fileName));
        result.put(Key.FINGERPRINT, fingerprint);
        return result;
    }
}
