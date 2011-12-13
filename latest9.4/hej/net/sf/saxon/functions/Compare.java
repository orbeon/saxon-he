package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

/**
* XSLT 2.0 compare() function
*/

// Supports string comparison using a collation

public class Compare extends CollatingFunction implements CallableExpression  {

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    @Override
    public IntegerValue[] getIntegerBounds() {
        return new IntegerValue[] {Int64Value.MINUS_ONE, Int64Value.PLUS_ONE};
    }

    /**
    * Evaluate the expression
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0==null) {
            return null;
        }

        AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(context);
        if (arg1==null) {
            return null;
        }

        GenericAtomicComparer collator = getAtomicComparer(2, context);

        int result = collator.compareAtomicValues(arg0, arg1);
        if (result < 0) {
            return Int64Value.MINUS_ONE;
        } else if (result > 0) {
            return Int64Value.PLUS_ONE;
        } else {
            return Int64Value.ZERO;
        }
    }

	public SequenceIterator call(SequenceIterator[] arguments,
			XPathContext context) throws XPathException {
		AtomicValue arg0 = (AtomicValue)arguments[0].next();
        if (arg0==null) {
            return null;
        }

        AtomicValue arg1 = (AtomicValue)arguments[1].next();
        if (arg1==null) {
            return null;
        }

        GenericAtomicComparer collator = getAtomicComparer(2, context);

        int result = collator.compareAtomicValues(arg0, arg1);
        if (result < 0) {
            return SingletonIterator.makeIterator(Int64Value.MINUS_ONE);
        } else if (result > 0) {
            return SingletonIterator.makeIterator(Int64Value.PLUS_ONE);
        } else {
            return SingletonIterator.makeIterator(Int64Value.ZERO);
        }
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