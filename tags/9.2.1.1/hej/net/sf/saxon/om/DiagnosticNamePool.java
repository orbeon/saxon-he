package net.sf.saxon.om;

/**
 *  This class provides a diagnostic wrapper for the real NamePool. There are no formal interfaces
 *  to exploit it, but it can be patched into a system by use of setNamePool() on the Configuration,
 *  and its effect is to trace entry to selected methods, notably those that are synchronized, for
 *  diagnostic analysis.
 */
public class DiagnosticNamePool extends NamePool {

    public synchronized int allocateNamespaceCode(String prefix, String uri) {
        System.err.println("allocate nscode for " + prefix + " = " + uri);
        return super.allocateNamespaceCode(prefix, uri);
    }

    public synchronized short allocateCodeForURI(String uri) {
        System.err.println("allocateCodeForURI");
        return super.allocateCodeForURI(uri);
    }

    public synchronized int allocate(String prefix, String uri, String localName) {
        System.err.println("allocate " + prefix + " : " + uri + " : " + localName);
        return super.allocate(prefix, uri, localName);
    }

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



