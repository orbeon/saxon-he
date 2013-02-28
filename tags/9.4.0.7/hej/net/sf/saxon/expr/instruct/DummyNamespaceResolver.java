package net.sf.saxon.expr.instruct;
import net.sf.saxon.expr.PairIterator;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NamespaceResolver;

import java.io.Serializable;
import java.util.Iterator;

/**
  * A dummy namespace resolver used when validating QName-valued attributes written to
  * the result tree. The namespace node might be created after the initial validation
  * of the attribute, so in the first round of validation we only check the lexical form
  * of the value, and we defer prefix checks until later.
  */

public final class DummyNamespaceResolver implements Serializable, NamespaceResolver {

    private final static DummyNamespaceResolver THE_INSTANCE = new DummyNamespaceResolver();

    /**
     * Return the singular instance of this class
     * @return the singular instance
     */

    public static DummyNamespaceResolver getInstance() {
        return THE_INSTANCE;
    }

    private DummyNamespaceResolver() {}


    /**
    * Get the namespace URI corresponding to a given prefix.
    * @param prefix the namespace prefix
    * @param useDefault true if the default namespace is to be used when the
    * prefix is ""
    * @return the uri for the namespace, or null if the prefix is not in scope
    */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (prefix.length()==0) {
            return NamespaceConstant.NULL;
        } else if ("xml".equals(prefix)) {
            return NamespaceConstant.XML;
        } else {
            // this is a dummy namespace resolver, we don't actually know the URI
            return NamespaceConstant.NULL;
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator<String> iteratePrefixes() {
        return new PairIterator<String>("", "xml");
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//