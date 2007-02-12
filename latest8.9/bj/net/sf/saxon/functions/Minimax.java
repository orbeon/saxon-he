package net.sf.saxon.functions;
import net.sf.saxon.Err;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.DescendingComparer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.*;

/**
* This class implements the min() and max() functions
*/

public class Minimax extends CollatingFunction {

    public static final int MIN = 2;
    public static final int MAX = 3;

    private boolean ignoreNaN = false;

    /**
     * Indicate whether NaN values should be ignored. For the external min() and max() function, a
     * NaN value in the input causes the result to be NaN. Internally, however, min() and max() are also
     * used in such a way that NaN values should be ignored.
     * @param ignore true if NaN values are to be ignored when computing the min or max.
     */

    public void setIgnoreNaN(boolean ignore) {
        ignoreNaN = ignore;
    }

    /**
     * Test whether NaN values are to be ignored
     */

    public boolean isIgnoreNaN() {
        return ignoreNaN;
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
    }

    /**
     * Determine the cardinality of theV function.
     */

    public int computeCardinality() {
        int c = super.computeCardinality();
        if (!Cardinality.allowsZero(argument[0].getCardinality())) {
            c = StaticProperty.EXACTLY_ONE;
        }
        return c;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(opt, env, contextItemType);
        if (e != this) {
            return e;
        }
        if (getNumberOfArguments() == 1) {
            // test for a singlton: this often happens after (A<B) is rewritten as (min(A) lt max(B))
            int card = argument[0].getCardinality();
            TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            if (!Cardinality.allowsMany(card) && th.isSubType(argument[0].getItemType(th), BuiltInAtomicType.NUMERIC)) {
                Expression.setParentExpression(argument[0], getParentExpression());
                return argument[0];
            }
        }
        return this;
    }

    /**
     * Determine the item type of the value returned by the function
     *
     * @param th the type hierarchy cache
     * @return the statically inferred type of the expression
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType t = argument[0].getItemType(th);
        if (t.getPrimitiveType() == Type.UNTYPED_ATOMIC) {
            return BuiltInAtomicType.DOUBLE;
        } else {
            return t;
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicComparer collator = getAtomicComparer(1, context);
        SequenceIterator iter = argument[0].iterate(context);
        try {
            return minimax(iter, operation, collator, ignoreNaN, context);
        } catch (XPathException err) {
            err.setLocator(this);
            throw err;
        }
    }
  
    /**
     * Static method to evaluate the minimum or maximum of a sequence
     * @param iter Iterator over the input sequence
     * @param operation either {@link #MIN} or {@link #MAX}
     * @param atomicComparer an AtomicComparer used to compare values
     * @param ignoreNaN true if NaN values are to be ignored
     * @param context dynamic evaluation context
     * @return the min or max value in the sequence, according to the rules of the fn:min() or fn:max() functions
     * @throws XPathException typically if non-comparable values are found in the sequence
     */
    public static AtomicValue minimax(SequenceIterator iter, int operation,
                                      AtomicComparer atomicComparer, boolean ignoreNaN, XPathContext context)
            throws XPathException {

        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        boolean foundDouble = false;
        boolean foundFloat = false;
        boolean foundNaN = false;

        // For the max function, reverse the collator
        if (operation == MAX) {
            atomicComparer = new DescendingComparer(atomicComparer);
        }

        // Process the sequence, retaining the min (or max) so far. This will be an actual value found
        // in the sequence. At the same time, remember if a double and/or float has been encountered
        // anywhere in the sequence, and if so, convert the min/max to double/float at the end. This is
        // done to avoid problems if a decimal is converted first to a float and then to a double.

        // Get the first value in the sequence, ignoring any NaN values
        AtomicValue min;
        AtomicValue prim;
        while (true) {
            min = (AtomicValue)iter.next();
            if (min == null) {
                return null;
            }
            prim = min;
            if (min instanceof UntypedAtomicValue) {
                try {
                    min = new DoubleValue(Value.stringToNumber(min.getStringValueCS()));
                    prim = min;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    DynamicError de = new DynamicError(
                            "Failure converting " + Err.wrap(min.getStringValueCS()) + " to a number");
                    de.setErrorCode("FORG0001");
                    de.setXPathContext(context);
                    throw de;
                }
            } else {
                if (prim instanceof DoubleValue) {
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            if (prim instanceof NumericValue && ((NumericValue)prim).isNaN()) {
                // if there's a NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    continue;   // ignore the NaN and treat the next item as the first real one
                } else if (prim instanceof DoubleValue) {
                    return min; // return double NaN
                } else {
                    // we can't ignore a float NaN, because we might need to promote it to a double NaN
                    foundNaN = true;
                    min = FloatValue.NaN;
                    break;
                }
            } else {
                if (!prim.getPrimitiveType().isOrdered()) {
                    DynamicError de = new DynamicError(
                            "Type " + prim.getPrimitiveType() + " is not an ordered type");
                    de.setErrorCode("FORG0006");
                    de.setIsTypeError(true);
                    de.setXPathContext(context);
                    throw de;
                }
                break;          // process the rest of the sequence
            }
        }

        while (true) {
            AtomicValue test = (AtomicValue)iter.next();
            if (test==null) {
                break;
            }
            AtomicValue test2 = test;
            prim = test2;
            if (test instanceof UntypedAtomicValue) {
                try {
                    test2 = new DoubleValue(Value.stringToNumber(test.getStringValueCS()));
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    prim = test2;
                    foundDouble = true;
                } catch (NumberFormatException e) {
                    DynamicError de = new DynamicError(
                            "Failure converting " + Err.wrap(test.getStringValueCS()) + " to a number");
                    de.setErrorCode("FORG0001");
                    de.setXPathContext(context);
                    throw de;
                }
            } else {
                if (prim instanceof DoubleValue) {
                    if (foundNaN) {
                        return DoubleValue.NaN;
                    }
                    foundDouble = true;
                } else if (prim instanceof FloatValue) {
                    foundFloat = true;
                }
            }
            if (prim instanceof NumericValue && ((NumericValue)prim).isNaN()) {
                // if there's a double NaN in the sequence, return NaN, unless ignoreNaN is set
                if (ignoreNaN) {
                    continue;
                } else if (foundDouble) {
                    return DoubleValue.NaN;
                } else {
                    // can't return float NaN until we know whether to promote it
                    foundNaN = true;
                }
            } else {
                try {
                    if (atomicComparer.compareAtomicValues(prim, min) < 0) {
                        min = test2;
                    }
                } catch (ClassCastException err) {
                    DynamicError de = new DynamicError(
                            "Cannot compare " + min.getItemType(th) + " with " + test2.getItemType(th));
                    de.setErrorCode("FORG0006");
                    de.setIsTypeError(true);
                    de.setXPathContext(context);
                    throw de;
                }
            }
        }
        if (foundNaN) {
            return FloatValue.NaN;
        }
        if (foundDouble) {
            if (!(min instanceof DoubleValue)) {
                min = min.convert(BuiltInAtomicType.DOUBLE, context);
            }
        } else if (foundFloat) {
            if (!(min instanceof FloatValue)) {
                min = min.convert(BuiltInAtomicType.FLOAT, context);
            }    
        }
        return min;
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
