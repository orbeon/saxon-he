////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SumCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import javax.xml.transform.SourceLocator;


/**
 * Implementation of the fn:sum function
 */
public class Sum extends FoldingFunction {

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


    /**
     * Get implementation method
     *
     * @return a value that indicates this function is capable of being streamed
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }

    /*@NotNull*/
    public ItemType getItemType() {
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            base = BuiltInAtomicType.DOUBLE;
        }
        if (Cardinality.allowsZero(argument[0].getCardinality())) {
            if (argument.length == 1) {
                return Type.getCommonSuperType(base, BuiltInAtomicType.INTEGER, th);
            } else {
                return Type.getCommonSuperType(base, argument[1].getItemType(), th);
            }
        } else {
            return base.getPrimitiveItemType();
        }
    }

    public int computeCardinality() {
        if (argument.length == 1 || argument[1].getCardinality() == 1) {
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
        if (additionalArguments.length > 0) {
            AtomicValue z = (AtomicValue)additionalArguments[0].head();
            return new SumFold(context, z);
        } else {
            return new SumFold(context, Int64Value.ZERO);
        }
    }

    /**
     * Static method to compute a total, invoked from compiled bytecode
     * @param in the sequence of items to be summed
     * @param context dynamic context for evaluation
     * @param locator expression location for diagnostics
     * @return null if the input is empty, otherwise the total as defined by the semantics of the sum() function
     * @throws XPathException if a dynamic error occurs
     */

    public static AtomicValue total(SequenceIterator in, XPathContext context, SourceLocator locator) throws XPathException {
        try {
            SumFold fold = new SumFold(context, null);
            Item item;
            while ((item = in.next()) != null) {
                fold.processItem(item);
            }
            return (AtomicValue)fold.result().head();
        } catch (XPathException e) {
            e.maybeSetLocation(locator);
            throw e;
        }
    }

    /**
     * Implementation of Fold class to do the summation in push mode
     */

    private static class SumFold implements Fold {
        private XPathContext context;
        private AtomicValue zeroValue; // null means empty sequence
        private AtomicValue data;
        private boolean atStart = true;
        private ConversionRules rules;
        private StringConverter toDouble;

        public SumFold(XPathContext context, AtomicValue zeroValue) {
            this.context = context;
            this.zeroValue = zeroValue;
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
            if (atStart) {
                atStart = false;
                if (next instanceof UntypedAtomicValue) {
                    data = toDouble.convert(next).asAtomic();
                    return;
                } else if (next instanceof NumericValue || next instanceof DayTimeDurationValue || next instanceof YearMonthDurationValue) {
                    data = next;
                    return;
                } else {
                    XPathException err = new XPathException(
                        "Input to sum() contains a value of type " +
                                next.getPrimitiveType().getDisplayName() +
                                " which is neither numeric, nor a duration");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    throw err;
                }
            }

            if (data instanceof NumericValue) {
                if (next instanceof UntypedAtomicValue) {
                    next = toDouble.convert(next).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    throw err;
                }
                data = ArithmeticExpression.compute(data, Calculator.PLUS, next, context);
            } else if (data instanceof DurationValue) {
                if (!((data instanceof DayTimeDurationValue) || (data instanceof YearMonthDurationValue))) {
                    XPathException err = new XPathException("Input to sum() contains a duration that is neither a dayTimeDuration nor a yearMonthDuration");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    throw err;
                }
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    throw err;
                }
                data = ((DurationValue) data).add((DurationValue) next);
            } else {
                XPathException err = new XPathException(
                        "Input to sum() contains a value of type " +
                                data.getPrimitiveType().getDisplayName() +
                                " which is neither numeric, nor a duration");
                err.setXPathContext(context);
                err.setErrorCode("FORG0006");
                throw err;
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
                return zeroValue == null ? EmptySequence.getInstance() : zeroValue;
            } else {
                return data;
            }
        }
    }


//#ifdefined BYTECODE

    /**
     * Return the compiler of the Sum expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SumCompiler();
    }
//#endif

}

