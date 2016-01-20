////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * This class wraps any SequenceIterator as an AxisIterator. It must only be used if it
 * is known that the underlying SequenceIterator will always return nodes, and will never
 * throw an exception.
 */

public class AxisIteratorOverSequence implements AxisIterator {

    private SequenceIterator base;
    private NodeInfo current;

    public AxisIteratorOverSequence(SequenceIterator base) {
        this.base = base;
    }

    public NodeInfo next() {
        try {
            return current = (NodeInfo)base.next();
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    /*@NotNull*/
    public AxisIterator getAnother() {
        try {
            return new AxisIteratorOverSequence(base.getAnother());
        } catch (XPathException err) {
            throw new AssertionError(err);
        }
    }

    public void close() {
        base.close();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }

}

