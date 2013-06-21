package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;

import java.io.Serializable;

/**
 * A Comparer used for comparing nodes in document order. This
 * comparer is used when there is no guarantee that the nodes being compared
 * come from the same document
 *
 * @author Michael H. Kay
 *
 */

public final class GlobalOrderComparer implements ItemOrderComparer, Serializable {

    private static GlobalOrderComparer instance = new GlobalOrderComparer();

    /**
    * Get an instance of a GlobalOrderComparer. The class maintains no state
    * so this returns the same instance every time.
    */

    public static GlobalOrderComparer getInstance() {
        return instance;
    }

    public int compare(Item a, /*@NotNull*/ Item b) {
        if (a==b) {
            return 0;
        }
        long d1 = ((NodeInfo)a).getDocumentNumber();
        long d2 = ((NodeInfo)b).getDocumentNumber();
        if (d1 == d2) {
            return ((NodeInfo)a).compareOrder(((NodeInfo)b));
        }
        return Long.signum(d1 - d2);
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