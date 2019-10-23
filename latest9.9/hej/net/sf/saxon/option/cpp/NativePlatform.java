////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2019 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



package net.sf.saxon.option.cpp;


import net.sf.saxon.Configuration;

import net.sf.saxon.java.JavaPlatform;


/**
 * Implementation of the Platform class containing methods specific to the Java platform
 * (as distinct from .NET) for Saxon-PE and Saxon-EE.
 * This is a singleton class with no instance data.
 * The key differences to the JavaPlatformPE is that we have no reference to registerAllBuiltInObjectModels and the
 * environment variable for Saxon/C we use SAXONC_HOME
 */
public class NativePlatform extends JavaPlatform {



    /**
     * The constructor is called during the static initialization of the Configuration
     */

    public NativePlatform() {
    }



    public String getPlatformSuffix() {
            return "C";
        }



    /**
     * Return the name of the directory in which the software is installed (if available)
     *
     * @param edition The edition of the software that is loaded ("HE", "PE", or "EE")
     * @param config  the Saxon configuration
     * @return the name of the directory in which Saxon/C is installed, if available, or null otherwise
     */

    public String getInstallationDirectory(String edition, Configuration config) {
        return System.getenv("SAXONC_HOME");
    }




}

// Copyright (c) 2009-2017 Saxonica Limited.


