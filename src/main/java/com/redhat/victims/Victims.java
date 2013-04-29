/*
 * Copyright (C) 2012 Red Hat Inc.
 *
 * This file is part of enforce-victims-rule for the Maven Enforcer Plugin.
 * enforce-victims-rule is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * enforce-victims-rule is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with enforce-victims-rule.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.redhat.victims;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @author gm
 */
public class Victims {

    static JSONObject fingerprint(final String filename) throws IOException, JSONException {

        // Input
        Jar jarfile = new Jar(filename);

        // Output
        JSONObject out = new JSONObject();
        out.put("filename", filename);

        // sha1
        Fingerprint sha1Visitor = new Fingerprint("SHA-1");
        jarfile.accept(sha1Visitor);
        out.put("sha1", sha1Visitor.results().getJSONObject(0));

        // sha 512
        Fingerprint sha512Visitor = new Fingerprint("SHA-512");
        jarfile.accept(sha512Visitor);
        out.put("sha512", sha512Visitor.results().getJSONObject(0));

        // extract metadata
        Metadata meta = new Metadata();
        jarfile.accept(meta);
        out.put("metadata", meta.results());


        return out;

    }

}
