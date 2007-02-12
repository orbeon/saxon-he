package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.RangeIterator;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.io.PrintStream;

/**
 * This class represents a sequence of consecutive ascending integers, for example 1 to 50.
 * The integers must be within the range of a Java long.
 */

public class IntegerRange extends Value {

    public long start;
    public long end;

    /**
     * Construct an integer range expression
     * @param start the first integer in the sequence (inclusive)
     * @param end the last integer in the sequence (inclusive). Must be >= start
     */

    public IntegerRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Determine whether the value is multivalued, that is, whether it is a sequence that
     * potentially contains more than one item
     *
     * @return true if the value might contain more than one item, false if it definitely
     *         contains zero or one items
     */

    public boolean isMultiValued() {
        return start != end;
    }

    /**
     * Get the first integer in the sequence (inclusive)
     * @return the first integer in the sequence (inclusive)
     */

    public long getStart() {
        return start;
    }

   /**
     * Get the last integer in the sequence (inclusive)
     * @return the last integer in the sequence (inclusive)
     */

    public long getEnd() {
        return end;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     */

//    public int getImplementationMethod() {
//        return ITERATE_METHOD;
//    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate() throws XPathException {
        return new RangeIterator(start, end);
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @return AnyItemType (not known)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.INTEGER;
    }

    /**
     * Determine the cardinality
     */

    public int getCardinality() {
        return StaticProperty.ALLOWS_MANY;
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) throws XPathException {
        if (n < 0 || n > (end-start)) {
            return null;
        }
        return Int64Value.makeIntegerValue(start + n);
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (int)(end - start + 1);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
