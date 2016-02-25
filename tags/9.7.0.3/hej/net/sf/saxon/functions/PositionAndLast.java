////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LastCompiler;
import com.saxonica.ee.bytecode.PositionCompiler;
import com.saxonica.ee.stream.adjunct.LastAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.FocusIterator;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;


public abstract class PositionAndLast extends ContextAccessorFunction {

    private boolean contextPossiblyUndefined = true;

    /**
     * Bind a context item to appear as part of the function's closure. If this method
     * has been called, the supplied context item will be used in preference to the
     * context item at the point where the function is actually called.
     *
     * @param context the context to which the function applies. Must not be null.
     */
    @Override
    public Function bindContext(XPathContext context) throws XPathException {
        Int64Value value;
        try {
            value = evaluateItem(context);
        } catch (final XPathException e) {
            // This happens when we do a dynamic lookup of position() or last() when there is no context item
            SymbolicName name = new SymbolicName(StandardNames.XSL_FUNCTION, getFunctionName(), getArity());
            Callable callable = new Callable() {
                public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                    throw e;
                }
            };
            return new CallableFunction(name, callable, getFunctionItemType());
        }
        ConstantFunction fn = new ConstantFunction(value);
        fn.setDetails(getDetails());
        fn.setRetainedStaticContext(getRetainedStaticContext());
        return fn;
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
        return new IntegerValue[]{Int64Value.PLUS_ONE, Expression.MAX_SEQUENCE_LENGTH};
    }

    @Override
    public void supplyTypeInformation(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, Expression[] arguments) throws XPathException {
        super.supplyTypeInformation(visitor, contextInfo, arguments);
        if (contextInfo.getItemType() == ErrorType.getInstance()) {
            XPathException err = new XPathException("The context item is absent at this point");
            err.setErrorCode("XPDY0002");
            throw err;
        } else {
            contextPossiblyUndefined = contextInfo.isPossiblyAbsent();
        }
    }

    /**
     * Ask whether the context item may possibly be undefined
     *
     * @return true if it might be undefined
     */

    public boolean isContextPossiblyUndefined() {
        return contextPossiblyUndefined;
    }

    /**
     * Evaluate in a general context
     */

    public abstract Int64Value evaluateItem(XPathContext c) throws XPathException;

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
        return evaluateItem(context);
    }


    public static class Position extends PositionAndLast {
        public Int64Value evaluateItem(XPathContext c) throws XPathException {
            FocusIterator currentIterator = c.getCurrentIterator();
            if (currentIterator == null) {
                XPathException e = new XPathException("The context item is absent, so position() is undefined");
                e.setXPathContext(c);
                e.setErrorCode("XPDY0002");
                throw e;
            }
            return Int64Value.makeIntegerValue(currentIterator.position());
        }

        //#ifdefined BYTECODE

        /**
         * Return the compiler of the Position expression
         *
         * @return the relevant ExpressionCompiler
         */
        @Override
        public ExpressionCompiler getExpressionCompiler() {
            return new PositionCompiler();
        }
//#endif

    }

    public static class Last extends PositionAndLast {
        public Int64Value evaluateItem(XPathContext c) throws XPathException {
            return Int64Value.makeIntegerValue(c.getLast());
        }

        //#ifdefined BYTECODE

        /**
         * Return the compiler of the Position expression
         *
         * @return the relevant ExpressionCompiler
         */
        @Override
        public ExpressionCompiler getExpressionCompiler() {
            return new LastCompiler();
        }
//#endif

        //#ifdefined STREAM


        /**
         * Get a class that supports streamed evaluation of this expression
         *
         * @return the relevant StreamingAdjunct, or null if none is available
         */
        @Override
        public StreamingAdjunct getStreamingAdjunct() {
            return new LastAdjunct();
        }
        //#endif
    }
}

