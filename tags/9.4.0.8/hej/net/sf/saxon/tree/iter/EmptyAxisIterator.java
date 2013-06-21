package net.sf.saxon.tree.iter;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;

/**
* An AxisIterator over an empty sequence
*/
public class EmptyAxisIterator<T extends NodeInfo>
        extends EmptyIterator<T> implements AxisIterator<T> {

    /*@NotNull*/
    private static EmptyAxisIterator<NodeInfo> theInstance =
            new EmptyAxisIterator<NodeInfo>();


    public static <T extends NodeInfo> net.sf.saxon.tree.iter.EmptyAxisIterator<T> emptyAxisIterator() {
        return (EmptyAxisIterator<T>) theInstance;
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link net.sf.saxon.om.Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */
    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        throw new NullPointerException();
    }

    /**
     * Get another iterator over the same items, positioned at the start.
     *
     * @return another iterator over an empty sequence (in practice, it
     *         returns the same iterator each time)
     */
    /*@NotNull*/
    @Override
    public net.sf.saxon.tree.iter.EmptyAxisIterator<T> getAnother() {
        return this;
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return false;
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