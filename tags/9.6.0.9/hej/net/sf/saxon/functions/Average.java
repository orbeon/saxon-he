////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.StringConverter;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;


/**
 * Implementation of the fn:avg function
 */
public class Average extends FoldingFunction {

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */
    @Override
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            argument[0] = argument[0].unordered(true, visitor.isOptimizeForStreaming());
            // we don't care about the order of the results, but we do care about how many nodes there are
        }
        return e;
    }

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }


    /**
     * Determine the item type of the value returned by the function
     */

    /*@NotNull*/
    public ItemType getItemType() {
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
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
     * Create the Fold object which actually performs the evaluation.
     *
     * @param context             the dynamic evaluation context
     * @param additionalArguments the values of all arguments other than the first.
     * @return the Fold object used to compute the function
     */
    @Override
    public Fold getFold(XPathContext context, Sequence... additionalArguments) throws XPathException {
        return new AverageFold(context);
    }

    private class AverageFold implements Fold {
        private XPathContext context;
        private AtomicValue data;
        private boolean atStart = true;
        private ConversionRules rules;
        private StringConverter toDouble;
        private int count = 0;

        public AverageFold(XPathContext context) {
            this.context = context;
            this.rules = context.getConfiguration().getConversionRules();
            this.toDouble = BuiltInAtomicType.DOUBLE.getStringConverter(rules);
        }

        /**
         * Process one item in the input sequence, returning a new copy of the working data
         *
         * @param item the item to be processed from the input sequence
         * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs
         */
        public void processItem(Item item) throws XPathException {
            AtomicValue next = (AtomicValue)item;
            if (next instanceof UntypedAtomicValue) {
                next = toDouble.convert(next).asAtomic();
            }
            count++;
            if (atStart) {
                if (next instanceof NumericValue || next instanceof DayTimeDurationValue || next instanceof YearMonthDurationValue) {
                    data = next;
                    atStart = false;
                } else {
                    throw new XPathException(
                        "Input to avg() contains a value that is neither numeric, nor a duration", "FORG0006");
                }
            } else {
                if (data instanceof NumericValue) {
                    if (!(next instanceof NumericValue)) {
                        throw new XPathException(
                            "Input to avg() contains a mix of numeric and non-numeric values", "FORG0006");
                    }
                    data = ArithmeticExpression.compute(data, Calculator.PLUS, next, context);
                } else if (data instanceof DurationValue) {
                    if (!(next instanceof DurationValue)) {
                        throw new XPathException(
                            "Input to avg() contains a mix of duration and non-duration values", "FORG0006");
                    }
                    data = ((DurationValue) data).add((DurationValue) next);
                } else {
                    throw new XPathException(
                        "Input to avg() contains a value that is neither numeric, nor a duration", "FORG0006");
                }
            }
        }

        /**
         * Ask whether the computation has completed. A function that can deliver its final
         * result without reading the whole input should return true; this will be followed
         * by a call on result() to deliver the final result.
         * @return true if the result of the function is now available even though not all
         * items in the sequence have been processed
         */
        public boolean isFinished() {
            return data instanceof DoubleValue && data.isNaN();
        }

        /**
         * Compute the final result of the function, when all the input has been processed
         *
         * @return the result of the function
         * @throws net.sf.saxon.trans.XPathException
         *          if a dynamic error occurs
         */
        public Sequence result() throws XPathException {
            if (atStart) {
                return EmptySequence.getInstance();
            } else if (data instanceof NumericValue) {
                return ArithmeticExpression.compute(data, Calculator.DIV, new Int64Value(count), context);
            } else {
                return ((DurationValue) data).multiply(1.0 / count);
            }
        }
    }



}

