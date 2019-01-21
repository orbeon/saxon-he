////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;


/**
 * A Sortable is an object that can be sorted using the QuickSort method.
 *
 * @author Michael H. Kay
 */

public interface Sortable {

    /**
     * Compare two objects within this Sortable, identified by their position.
     *
     * @return &lt;0 if obj[a]&lt;obj[b], 0 if obj[a]=obj[b], &gt;0 if obj[a]&gt;obj[b]
     */

    public int compare(int a, int b);

    /**
     * Swap two objects within this Sortable, identified by their position.
     */

    public void swap(int a, int b);

}

