package net.sf.saxon.value;

import net.sf.saxon.type.StringToDouble;

/**
 * Convert a string to a double using the rules of XML Schema 1.1
 */
public class StringToDouble11 extends StringToDouble {

    private static StringToDouble11 THE_INSTANCE = new StringToDouble11();

    /**
     * Get the singleton instance
     * @return the singleton instance of this class
     */

    public static StringToDouble11 getInstance() {
        return THE_INSTANCE;
    }

    protected StringToDouble11() {}

    @Override
    protected double signedPositiveInfinity() {
        return Double.POSITIVE_INFINITY;
    }
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
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



