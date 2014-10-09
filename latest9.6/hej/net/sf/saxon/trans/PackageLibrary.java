////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;


import net.sf.saxon.style.PackageVersion;
import net.sf.saxon.style.PackageVersionRanges;
import net.sf.saxon.style.StylesheetPackage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageLibrary {

    Map<String, List<StylesheetPackage>> packageMap = new HashMap<String, List<StylesheetPackage>>();

    /**
     * Add a package to the current library with the given name
     *
     * <p>Packages which have the same name <b>and version</b> to one already within the library are considered
     * to be duplicates and are not added.</p>
     *
     * @param name   The name to use for this package
     * @param packageIn  The stylesheet package to be added
     */
    public void addPackage(String name, StylesheetPackage packageIn) {
        List<StylesheetPackage> list = packageMap.get(name);
        if (list == null) {
            list = new ArrayList<StylesheetPackage>();
            list.add(packageIn);
            packageMap.put(name, list);
        } else {
            for (StylesheetPackage p : list) {
                if (p.getPackageVersion().equals(packageIn.getPackageVersion())) {
                    list.remove(p);
                    list.add(packageIn);
                    return;
                }
            }
            list.add(packageIn);
        }
    }

    /**
     * Return the first package from the library that has the given name and whose version lies in the given ranges
     *
     * @param name  The name of the package
     * @param ranges  The ranges of versions of that package that are acceptable
     * @return The first package that meets the criteria, or null if none can be found
     */
    public StylesheetPackage getPackage(String name, PackageVersionRanges ranges) {
        List<StylesheetPackage> list = packageMap.get(name);
        for (StylesheetPackage p : list) {
            if (ranges.contains(p.getPackageVersion())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Return the latest version of a package of the given name from the library.
     *
     * <p>The definition of 'latest' is determined by partial ordering of the {@link PackageVersion} class</p>
     *
     * @param name The name of the package to be returned
     * @return  The version of the named package whose version orders highest, or null if no such package exists
     */
    public StylesheetPackage getPackage(String name) {
        PackageVersion latestVersion = null;
        StylesheetPackage latestPackage = null;
        List<StylesheetPackage> list = packageMap.get(name);
        if (list == null) {
            return null;
        }
        for (StylesheetPackage p : list) {
            if (latestVersion == null || latestVersion.compareTo(p.getPackageVersion()) == -1) {
                latestVersion = p.getPackageVersion();
                latestPackage = p;
            }

        }
        return latestPackage;
    }

    public int size(String name) {
        return packageMap.get(name).size();
    }
}
