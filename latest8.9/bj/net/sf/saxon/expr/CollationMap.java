package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.StringCollator;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This object maps collation URIs to collations. Logically this function is part of the static
 * context, but it is often needed dynamically, so it is defined as a separate component that can
 * safely be retained at run-time.
 */
public class CollationMap implements Serializable {

    private Configuration config;
    private String defaultCollationName;
    private HashMap map;

    public CollationMap(Configuration config) {
        this.config = config;
        this.defaultCollationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    public CollationMap(CollationMap in) {
        if (in.map != null) {
            map = new HashMap(in.map);
        }
        config = in.config;
        defaultCollationName = in.defaultCollationName;
    }

    public void setDefaultCollationName(String name) {
        defaultCollationName = name;
    }

    public String getDefaultCollationName() {
        return defaultCollationName;
    }

    public StringCollator getDefaultCollation() {
        return getNamedCollation(defaultCollationName);
    }

    public void setNamedCollation(String absoluteURI, StringCollator comparator) {
        if (map == null) {
            map = new HashMap();
        }
        map.put(absoluteURI, comparator);
    }

    public StringCollator getNamedCollation(String name) {
        if (name.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            return CodepointCollator.getInstance();
        }
        if (map != null) {
            StringCollator c = (StringCollator)map.get(name);
            if (c != null) {
                return c;
            }
        }
        return config.getCollationURIResolver().resolve(name, null, config);
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

