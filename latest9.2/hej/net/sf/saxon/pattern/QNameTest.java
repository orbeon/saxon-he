package net.sf.saxon.pattern;

import net.sf.saxon.om.StructuredQName;

import java.io.Serializable;

/**
 * Interface for tests against a QName. This is implemented by a subset of NodeTests, specifically
 * those that are only concerned with testing the name of the node
 */

public interface QNameTest extends Serializable {

    /**
     * Test whether the QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    boolean matches(StructuredQName qname);
}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

