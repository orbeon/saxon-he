package net.sf.saxon.tree.util;

import net.sf.saxon.om.NamePool;

import java.io.PrintStream;

/**
 *  This class provides a diagnostic wrapper for the real NamePool. There are no formal interfaces
 *  to exploit it, but it can be patched into a system by use of setNamePool() on the Configuration,
 *  and its effect is to trace entry to selected methods, notably those that are synchronized, for
 *  diagnostic analysis.
 */
public class DiagnosticNamePool extends NamePool {

    public PrintStream printStream = System.err;

    public synchronized short allocateCodeForURI(String uri) {
        printStream.println("allocateCodeForURI");
        return super.allocateCodeForURI(uri);
    }

    public synchronized int allocate(String prefix, String uri, String localName) {
        printStream.println("allocate " + prefix + " : " + uri + " : " + localName);
        return super.allocate(prefix, uri, localName);
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