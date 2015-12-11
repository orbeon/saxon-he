////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.CountCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.CountAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

/**
 * Implementation of the fn:count function
 */
public class Count extends FoldingFunction {

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
        return super.getImplementationMethod() | WATCH_METHOD;
    }

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
        return new IntegerValue[]{Int64Value.ZERO, MAX_SEQUENCE_LENGTH};
    }

    /**
     * Create the Fold object which actually performs the evaluation. Must be implemented
     * in every subclass.
     *
     * @param context             the dynamic evaluation context
     * @param additionalArguments the values of all arguments other than the first.
     * @return the Fold object used to compute the function
     */
    @Override
    public Fold getFold(XPathContext context, Sequence... additionalArguments) throws XPathException {
        return new CountFold(); // TODO: not yet used
    }

    private class CountFold implements Fold {

        private int count = 0;

        public CountFold() {
        }

        /**
         * Process one item in the input sequence, returning a new copy of the working data
         *
         * @param item the item to be processed from the input sequence
         * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs
         */
        public void processItem(Item item) throws XPathException {
            count++;
        }

        /**
         * Ask whether the computation has completed. A function that can deliver its final
         * result without reading the whole input should return true; this will be followed
         * by a call on result() to deliver the final result.
         * @return true if the result of the function is now available even though not all
         * items in the sequence have been processed
         */
        public boolean isFinished() {
            return false;
        }

        /**
         * Compute the final result of the function, when all the input has been processed
         *
         * @return the result of the function
         * @throws net.sf.saxon.trans.XPathException
         *          if a dynamic error occurs
         */
        public IntegerValue result() throws XPathException {
            return Int64Value.makeIntegerValue(count);
        }
    }

    /**
     * Evaluate the function
     */

    public Int64Value evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = argument[0].iterate(context);
        return new Int64Value(count(iter));
    }

    /**
     * Get the number of items in a sequence identified by a SequenceIterator
     *
     * @param iter The SequenceIterator. This method moves the current position
     *             of the supplied iterator; if this isn't safe, make a copy of the iterator
     *             first by calling getAnother(). The supplied iterator must be positioned
     *             before the first item (there must have been no call on next()).
     * @return the number of items in the underlying sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a failure occurs reading the input sequence
     */

    public static int count(/*@NotNull*/ SequenceIterator iter) throws XPathException {
        if ((iter.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            return ((LastPositionFinder) iter).getLength();
        } else {
            int n = 0;
            while (iter.next() != null) {
                n++;
            }
            return n;
        }
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public IntegerValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        return new Int64Value(count(arguments[0].iterate()));
    }

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public CountAdjunct getStreamingAdjunct() {
        return new CountAdjunct();
    }
//#endif
//#ifdefined BYTECODE

    /**
     * Return the compiler of the Sum expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new CountCompiler();
    }
//#endif


}

