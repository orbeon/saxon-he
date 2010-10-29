package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import javax.xml.transform.SourceLocator;


/**
 * Implementation of the fn:avg function
 */
public class Average extends Aggregate {

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }
    

    /**
     * Determine the item type of the value returned by the function
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            return BuiltInAtomicType.DOUBLE;
        } else if (base.getPrimitiveType() == StandardNames.XS_INTEGER) {
            return BuiltInAtomicType.DECIMAL;
        } else {
            return base;
        }
    }

    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        if (!Cardinality.allowsZero(argument[0].getCardinality())) {
            return StaticProperty.EXACTLY_ONE;
        } else {
            return super.computeCardinality();
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return average(argument[0].iterate(context), context, this);
    }


    /**
     * Calculate average
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the average of the values
    */

    public static AtomicValue average(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        ConversionRules rules = context.getConfiguration().getConversionRules();
        int count = 0;
        AtomicValue item = (AtomicValue)iter.next();
        if (item == null) {
            // the sequence is empty
            return null;
        }
        count++;
        if (item instanceof UntypedAtomicValue) {
            try {
                item = item.convert(BuiltInAtomicType.DOUBLE, true, rules).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (item instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    //return ((NumericValue)item).arithmetic(Token.DIV, new Int64Value(count), context);
                    return ArithmeticExpression.compute(item, Calculator.DIV, new Int64Value(count), context);
                }
                count++;
                if (next instanceof UntypedAtomicValue) {
                    try {
                        next = next.convert(BuiltInAtomicType.DOUBLE, true, rules).asAtomic();
                    } catch (XPathException e) {
                        e.maybeSetLocation(location);
                        throw e;
                    }
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to avg() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                //item = ((NumericValue)item).arithmetic(Token.PLUS, (NumericValue)next, context);
                item = ArithmeticExpression.compute(item, Calculator.PLUS, next, context);
                if (item.isNaN() && item instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return item;
                }
            }
        } else if (item instanceof DurationValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return ((DurationValue)item).multiply(1.0/count);
                }
                count++;
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to avg() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                item = ((DurationValue)item).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException("Input to avg() contains a value that is neither numeric, nor a duration");
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

