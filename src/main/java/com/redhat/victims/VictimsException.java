package com.redhat.victims;

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
 * Generic wrapper for all exceptions that are thrown within this plug-in.
 * 
 * @author gmurphy
 */
@SuppressWarnings("serial")
public class VictimsException extends Exception {

    /**
     * Create a new exception with the supplied error.
     * 
     * @param message
     *            The message to associate with this exception.
     */
    public VictimsException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the supplied message and cause.
     * 
     * @param message
     *            The message to associate with this error.
     * @param e
     *            The underlying cause of the exception.
     */
    public VictimsException(String message, Throwable e) {
        super(message, e);
    }
}
