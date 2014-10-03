////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;


import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.value.NestedIntegerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackageLibrary {

    Map<String, List<StylesheetPackage>> packageMap = new HashMap<String, List<StylesheetPackage>>();

    public void addPackage(String name, StylesheetPackage packagei) {
        List<StylesheetPackage> list = packageMap.get(name);
        if (list == null) {
            list = new ArrayList<StylesheetPackage>();
            list.add(packagei);
            packageMap.put(name, list);
        } else {
            for (StylesheetPackage p : list) {
                if (p.getPackageVersion().equals(packagei.getPackageVersion())) {
                    list.remove(p);
                    list.add(packagei);
                    return;
                }
            }
            list.add(packagei);

        }

    }

    public StylesheetPackage getPackage(String name, NestedIntegerValue ver) {
        List<StylesheetPackage> list = packageMap.get(name);
        for (StylesheetPackage p : list) {
            if (p.getPackageVersion().equals(ver)) {
                return p;
            }
        }
        return null;
    }

    public StylesheetPackage getPackage(String name) {
        NestedIntegerValue latestVersion = null;
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

    public StylesheetPackage getPackage(String name, RegularExpression versionRegEx) {
        List<StylesheetPackage> list = packageMap.get(name);
        if (list != null) {
            for (StylesheetPackage p : list) {
                if (versionRegEx.matches(p.getPackageVersion().getStringValueCS())) {
                    return p;
                }
            }
        }
        return null;
    }

    public int size(String name) {
        return packageMap.get(name).size();
    }
}
