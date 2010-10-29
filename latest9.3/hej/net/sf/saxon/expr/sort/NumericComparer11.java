package net.sf.saxon.expr.sort;

import net.sf.saxon.value.StringToDouble11;

/**
 * A Comparer used for comparing sort keys when data-type="number". The items to be
 * compared are converted to numbers, and the numbers are then compared directly. NaN values
 * compare equal to each other, and equal to an empty sequence, but less than anything else.
 * <p/>
 * This class is used in XSLT only, so there is no need to handle XQuery's "empty least" vs
 * "empty greatest" options.
 *
 * @author Michael H. Kay
 *
 */

public class NumericComparer11 extends NumericComparer {

    private static NumericComparer11 THE_INSTANCE = new NumericComparer11();

    public static NumericComparer getInstance() {
        return THE_INSTANCE;
    }

    protected NumericComparer11() {
        converter = StringToDouble11.getInstance();
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
// The Original Code is: all this file.
//
// The Initial Developer of this module is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//