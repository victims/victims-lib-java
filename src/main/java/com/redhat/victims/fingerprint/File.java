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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Implements handing of simple files for fingerprinting.
 * 
 * @author abn
 * 
 */
public class File extends AbstractFile {

    /**
     * 
     * @param bytes
     *            Input file as a byte array.
     * @param fileName
     *            The name of the file being processed.
     */
    public File(byte[] bytes, String fileName) {
        this.fileName = fileName;
        this.fingerprint = Processor.fingerprint(bytes);
    }

    /**
     * 
     * @param is
     *            The file as an input stream.
     * @param fileName
     *            The name of the file provided by the stream.
     * @throws IOException
     */
    public File(InputStream is, String fileName) throws IOException {
        this(IOUtils.toByteArray(is), fileName);
    }

}
