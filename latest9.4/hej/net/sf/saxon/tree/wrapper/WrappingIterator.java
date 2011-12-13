package net.sf.saxon.tree.wrapper;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.value.Value;

/**
 * A WrappingIterator delivers wrappers for the nodes delivered
 * by its underlying iterator. It is used when no whitespace stripping
 * is actually needed, e.g. for the attribute axis. But we still need to
 * create wrappers, so that further iteration remains in the virtual layer
 * rather than switching to the real nodes.
 */

public class WrappingIterator implements AxisIterator {

    AxisIterator base;
    VirtualNode parent;
    /*@Nullable*/ NodeInfo current;
    boolean atomizing = false;
    WrappingFunction wrappingFunction;

    /**
     * Create a WrappingIterator
     *
     * @param base   The underlying iterator
     * @param parent If all the nodes to be wrapped have the same parent,
     *               it can be specified here. Otherwise specify null.
     */

    public WrappingIterator(AxisIterator base, WrappingFunction function, VirtualNode parent) {
        this.base = base;
        this.wrappingFunction = function;
        this.parent = parent;
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return (next() != null);
    }


    /*@Nullable*/ public NodeInfo next() {
        Item n = base.next();
        if (n instanceof NodeInfo && !atomizing) {
            current = wrappingFunction.makeWrapper((NodeInfo) n, parent);
        } else {
            current = (NodeInfo) n;
        }
        return current;
    }

    /*@Nullable*/ public NodeInfo current() {
        return current;
    }

    public int position() {
        return base.position();
    }

    public void close() {
        base.close();
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
        return current.iterateAxis(axis, test);
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        return current.atomize();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        return current.getStringValueCS();
    }

    /*@NotNull*/ public AxisIterator getAnother() {
        return new WrappingIterator(base.getAnother(), wrappingFunction, parent);
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