package net.sf.saxon.s9api;

import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * An value in the XDM data model. A value is a sequence of zero or more items,
 * each item being either an atomic value or a node.
 *
 * <p>An XdmValue is immutable.</p>
 *
 * <p>A sequence consisting of a single item may be represented as an instance of {@link XdmItem},
 * which is a subtype of XdmValue. However, there is no guarantee that a sequence of length one
 * will always be an instance of XdmItem.</p>
 *
 * <p>Similarly, a zero-length sequence may be represented as an instance of {@link XdmEmptySequence},
 * but there is no guarantee that every sequence of length zero will always be an instance of
 * XdmEmptySequence.</p>
 * 
 * @since 9.0
 */

public class XdmValue implements Iterable<XdmItem> {

    private ValueRepresentation value;    // this must be a materialized value

    protected XdmValue() {
        // must be followed by setValue()
    }

    /**
     * Create an XdmValue as a sequence of XdmItem objects
     * @param items a sequence of XdmItem objects. Note that if this is supplied as a list or similar
     * collection, subsequent changes to the list/collection will have no effect on the XdmValue.
     * @since 9.0.0.4
     */

    public XdmValue(Iterable<XdmItem> items) {
        List values = new ArrayList();
        for (XdmItem item : items) {
            values.add(item.getUnderlyingValue());
        }
        value = new SequenceExtent(values);
    }

    protected XdmValue(ValueRepresentation value) {
        this.value = value;
    }

    protected void setValue(ValueRepresentation value) {
        this.value = value;
    }

    protected static XdmValue wrap(ValueRepresentation value) {
        if (value == null) {
            return null;
        } else if (value instanceof NodeInfo) {
            return new XdmNode((NodeInfo)value);
        } else if (value instanceof AtomicValue) {
            return new XdmAtomicValue((AtomicValue)value);
        } else if (value instanceof EmptySequence) {
            return XdmEmptySequence.getInstance();
        } else {
            return new XdmValue(value);
        }
    }

    /**
     * Create a new XdmValue by concatenating the contents of this XdmValue and another
     * XdmValue. The two input XdmValue objects are unaffected by this operation
     * @param otherValue the value to be appended
     * @return a new XdmValue containing the concatenation of the two input XdmValue objects
     * @since 9.2
     */

    public XdmValue append(XdmValue otherValue) {
        List values = new ArrayList();
        for (XdmItem item : this) {
            values.add(item.getUnderlyingValue());
        }
        for (XdmItem item : otherValue) {
            values.add(item.getUnderlyingValue());
        }
        return new XdmValue(new SequenceExtent(values));
    }

    /**
     * Get the number of items in the sequence
     * @return the number of items in the value
     * @throws SaxonApiUncheckedException if the value is lazily evaluated and the delayed
     * evaluation fails with a dynamic error.
     */

    public int size() {
        try {
            if (value instanceof Value) {
                return ((Value)value).getLength();
            } else {
                return 1;
            }
        } catch (XPathException err) {
            throw new SaxonApiUncheckedException(err);
        }
    }

    /**
     * Get the n'th item in the value, counting from zero.
     * @param n the item that is required, counting the first item in the sequence as item zero
     * @return the n'th item in the sequence making up the value, counting from zero
     * @throws IndexOutOfBoundsException if n is less than zero or greater than or equal to the number
     * of items in the value
     * @throws SaxonApiUncheckedException if the value is lazily evaluated and the delayed
     * evaluation fails with a dynamic error.
     */

    public XdmItem itemAt(int n) throws IndexOutOfBoundsException, SaxonApiUncheckedException {
        if (n < 0 || n >= size()) {
            throw new IndexOutOfBoundsException(""+n);
        }
        if (value instanceof Value) {
            try {
                return (XdmItem)XdmItem.wrap(((Value)value).itemAt(n));
            } catch (XPathException e) {
                throw new SaxonApiUncheckedException(e);
            }
        } else {
            return (XdmNode)XdmNode.wrap((NodeInfo)value);
        }
    }

    /**
     * Get an iterator over the items in this value.
     * @return an Iterator over the items in this value.
     * @throws SaxonApiUncheckedException if the value is lazily evaluated and the delayed
     * evaluation fails with a dynamic error.
     */
    public XdmSequenceIterator iterator() throws SaxonApiUncheckedException {
        try {
            ValueRepresentation v = getUnderlyingValue();
            if (v instanceof Value) {
                return new XdmSequenceIterator(((Value)v).iterate());
            } else {
                return new XdmSequenceIterator(SingletonIterator.makeIterator((NodeInfo)v));
            }
        } catch (XPathException e) {
            throw new SaxonApiUncheckedException(e);
        }
    }

    /**
     * Get the underlying implementation object representing the value. This method allows
     * access to lower-level Saxon functionality, including classes and methods that offer
     * no guarantee of stability across releases.
     * @return the underlying implementation object representing the value
     */

    public ValueRepresentation getUnderlyingValue() {
        return value;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

