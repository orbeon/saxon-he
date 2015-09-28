////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.event.LocationProvider;

/**
 * A LocationMap allocates integer codes to (systemId, lineNumber) pairs. The integer
 * codes are held inside an Expression object to track the location of the expression
 * in the source code
 */

public class LocationMap implements LocationProvider {

    private String[] modules = new String[10];
    private int numberOfModules = 0;

    /**
     * Create a location map
     */

    public LocationMap() {
    }

    /**
     * Allocate a location identifier to an expression
     *
     * @param module     the URI (system identifier) of the module
     * @param lineNumber the line number of the expression within the module
     * @return the corresponding location identifier
     */

    public int allocateLocationId(/*@Nullable*/ String module, int lineNumber) {
        if (module == null) {
            // the module has no base URI
            module = "*module with no systemId*";
        }
        int mod = -1;
        for (int m = numberOfModules - 1; m >= 0; m--) {
            if (modules[m].equals(module)) {
                mod = m;
                break;
            }
        }
        if (mod == -1) {
            if (numberOfModules >= modules.length) {
                String[] m2 = new String[numberOfModules * 2];
                System.arraycopy(modules, 0, m2, 0, numberOfModules);
                modules = m2;
            }
            mod = numberOfModules;
            modules[numberOfModules++] = module;
        }
        if (mod >= 1024) {
            mod = 1023;
            modules[mod] = "*unknown module uri*";
        }
        if (lineNumber > 999999) {
            lineNumber = 999999;
        }
        return (mod << 20) + lineNumber;
    }

    /**
     * Get the system identifier corresponding to a locationId
     *
     * @param locationId the location identifier
     * @return the corresponding system identifier
     */

    public String getSystemId(int locationId) {
        int m = locationId >> 20;
        if (m < 0 || m >= numberOfModules) {
            return null;
        }
        return modules[m];
    }

    /**
     * Get the line number corresponding to a locationId
     *
     * @param locationId the location identifier
     * @return the corresponding line number
     */

    public int getLineNumber(int locationId) {
        return 0xfffff & locationId;
    }

    public int getColumnNumber(int locationId) {
        return -1;
    }

}

