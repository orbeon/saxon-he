package net.sf.saxon.pattern;

import net.sf.saxon.om.StructuredQName;

import java.util.Iterator;
import java.util.List;

/**
 * A QNameTest that is the union of a number of supplied QNameTests
 */
public class UnionQNameTest implements QNameTest {

    List<QNameTest> tests;

    public UnionQNameTest(List<QNameTest> tests) {
        this.tests = tests;
    }

    /**
     * Test whether the QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        for (Iterator<QNameTest> iter = tests.iterator(); iter.hasNext();) {
            if (iter.next().matches(qname)) {
                return true;
            }
        }
        return false;
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



