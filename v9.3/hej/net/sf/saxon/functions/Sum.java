package net.sf.saxon.functions;

import net.sf.saxon.expr.ArithmeticExpression;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.Calculator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import javax.xml.transform.SourceLocator;


/**
 * Implementation of the fn:sum function
 */
public class Sum extends Aggregate {

    /**
     * Get implementation method
     * @return a value that indicates this function is capable of being streamed
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }    

    public ItemType getItemType(TypeHierarchy th) {
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            base = BuiltInAtomicType.DOUBLE;
        }
        if (Cardinality.allowsZero(argument[0].getCardinality())) {
            if (argument.length == 1) {
                return Type.getCommonSuperType(base, BuiltInAtomicType.INTEGER, th);
            } else {
                return Type.getCommonSuperType(base, argument[1].getItemType(th), th);
            }
        } else {
            return base;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue sum = total(argument[0].iterate(context), context, this);
        if (sum != null) {
            return sum;
        } else {
            // the sequence was empty
            if (argument.length == 2) {
                return argument[1].evaluateItem(context);
            } else {
                return Int64Value.ZERO;
            }
        }
    }

    /**
     * Calculate the total of a sequence.
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the total, according to the rules of the XPath sum() function, but returning null
     * if the sequence is empty. (It's then up to the caller to decide what the correct result is
     * for an empty sequence.
    */

    public static AtomicValue total(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        ConversionRules rules = context.getConfiguration().getConversionRules();
        AtomicValue sum = (AtomicValue)iter.next();
        if (sum == null) {
            // the sequence is empty
            return null;
        }
        if (sum instanceof UntypedAtomicValue) {
            try {
                sum = sum.convert(BuiltInAtomicType.DOUBLE, true, rules).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (sum instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (next instanceof UntypedAtomicValue) {
                    next = next.convert(BuiltInAtomicType.DOUBLE, true, rules).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                //sum = ((NumericValue)sum).arithmetic(Token.PLUS, (NumericValue)next, context);
                sum = ArithmeticExpression.compute(sum, Calculator.PLUS, next, context);
                if (sum.isNaN() && sum instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return sum;
                }
            }
        } else if (sum instanceof DurationValue) {
            if (!((sum instanceof DayTimeDurationValue) || (sum instanceof YearMonthDurationValue))) {
                XPathException err = new XPathException("Input to sum() contains a duration that is neither a dayTimeDuration nor a yearMonthDuration");
                err.setXPathContext(context);
                err.setErrorCode("FORG0006");
                err.setLocator(location);
                throw err;
            }
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                sum = ((DurationValue)sum).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException(
                    "Input to sum() contains a value of type " +
                            sum.getPrimitiveType().getDisplayName() +
                            " which is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(location);
            throw err;
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
