////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;

import java.io.Serializable;

/**
 * A Comparer used for comparing nodes in document order, or items in merge order
 *
 * @author Michael H. Kay
 */

public interface ItemOrderComparer {

    /**
     * Compare two objects.
     *
     * @return &lt;0 if a&lt;b, 0 if a=b, &gt;0 if a&gt;b
     */

    public int compare(Item a, Item b);

}

