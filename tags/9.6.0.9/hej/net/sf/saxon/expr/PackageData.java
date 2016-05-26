////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.LocationMap;

/**
 * Information about a unit of compilation: in XSLT, a package; in XQuery, a module. May also be a single
 * free-standing XPath expression.
 */
public class PackageData {

    private Configuration configuration;
    private int hostLanguage;
    private boolean isAllowXPath30;
    private boolean isSchemaAware;
    private LocationMap locationMap;

    /**
     * Create a PackageData object
     * @param config the Saxon configuration
     */

    public PackageData(Configuration config) {
        if (config == null) {
            throw new NullPointerException();
        }
        this.configuration = config;
    }

    /**
     * Get the Configuration to which this package belongs
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Set the Configuration to which this package belongs
     * @param configuration the Saxon configuration
     */

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get the language in which this package is written
     * @return typically {@link Configuration#XSLT}, {@link Configuration#XQUERY}, or {@link Configuration#XPATH}
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the language in which this package is written
     * @param hostLanguage typically {@link Configuration#XSLT}, {@link Configuration#XQUERY}, or {@link Configuration#XPATH}
     */

    public void setHostLanguage(int hostLanguage) {
        this.hostLanguage = hostLanguage;
    }

    /**
     * Ask whether XPath 3.0 syntax is permitted in this package
     * @return true if XPath 3.0 (and by implication XQuery 3.0, XSLT 3.0) is enabled
     */

    public boolean isAllowXPath30() {
        return isAllowXPath30;
    }

    /**
     * Say whether XPath 3.0 syntax is permitted in this package
     * @param xpath30allowed set to true if XPath 3.0 (and by implication XQuery 3.0, XSLT 3.0) is enabled
     */

    public void setAllowXPath30(boolean xpath30allowed) {
        isAllowXPath30 |= xpath30allowed;
    }

    /**
     * Ask whether the package is schema-aware
     * @return true if the package is schema-aware
     */

    public boolean isSchemaAware() {
        return isSchemaAware;
    }

    /**
     * Say whether the package is schema-aware
     * @param schemaAware set to true if the package is schema-aware
     */

    public void setSchemaAware(boolean schemaAware) {
        isSchemaAware = schemaAware;
    }

    /**
     * Get the location map for this package. This translates location IDs held in individual
     * expression nodes to (module URI, line number) pairs
     * @return the location map for this package
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Set the location map for this package. This translates location IDs held in individual
     * expression nodes to (module URI, line number) pairs
     * @param locationMap the location map for this package
     */

    public void setLocationMap(LocationMap locationMap) {
        this.locationMap = locationMap;
    }
}


