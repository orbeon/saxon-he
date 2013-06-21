package net.sf.saxon.tree.iter;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;

import java.util.Iterator;

/**
 * An AxisIterator that wraps a Java Iterator. This is an abstract class, because the Java
 * iterator does not hold enough information to support the getAnother() method, needed to
 * implement the XPath last() function
*/

public abstract class NodeWrappingAxisIterator<T extends NodeInfo, B>
        implements AxisIterator<T>, LookaheadIterator<T>, UnfailingIterator<T> {


    private int position = 0;
    /*@Nullable*/ private T current = null;
    private Iterator<B> base;
    private NodeWrappingFunction<B, T> wrappingFunction ;


    /**
     * Create a SequenceIterator over a given iterator
     * @param base the base Iterator
     * @param wrappingFunction a function that wraps objects of type B in a Saxon NodeInfo
     */

    public NodeWrappingAxisIterator(Iterator<B> base, NodeWrappingFunction<B, T> wrappingFunction) {
        this.base = base;
        position = 0;
        current = null;
        this.wrappingFunction = wrappingFunction;
    }



    public boolean moveNext() {
        return next() != null;
    }

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        return current.iterateAxis(axis, test);
    }

    public Value atomize() throws XPathException {
        return current.atomize();
    }

    public CharSequence getStringValue() {
        return current.getStringValue();
    }



    public boolean hasNext() {
        return base.hasNext();
    }

    /*@Nullable*/ public T next() {
        if (base.hasNext()) {
            current = wrappingFunction.wrap(base.next());
            position++;
        } else {
            current = null;
            position = -1;
        } 
        return current;
    }

    /*@Nullable*/ public T current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
    }

    /*@NotNull*/ public abstract AxisIterator<T> getAnother();

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
        return LOOKAHEAD;
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