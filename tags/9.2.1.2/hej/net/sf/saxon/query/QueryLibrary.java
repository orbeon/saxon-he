package net.sf.saxon.query;

import net.sf.saxon.trans.XPathException;

/**
 *  A QueryLibrary represents an independently compiled set of query modules that does not include a
 *  main module. Such a library can be compiled once, and then linked to different main modules without
 *  recompilation. The library contains one top-level module (itself a library module) together with the tree
 *  of modules that it imports; it is identified by the module URI of the top-level module.
 *
 *  <p>This is an abstract class; the concrete implementation is in Saxon-EE.</p>
 */
public abstract class QueryLibrary extends QueryModule {

    public QueryLibrary(StaticQueryContext sqc) throws XPathException {
        super(sqc);
    }

    /**
     * Link this library module to a module that imports it
     * @param importer the importing module (a user of the library)
     */

    public abstract void link(QueryModule importer) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



