////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.EmptyCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.EmptyAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.value.BooleanValue;


/**
 * Implementation of the fn:exists function
 */
public class Empty extends Aggregate implements Negatable {

    public int getImplementationMethod() {
        return super.getImplementationMethod() | WATCH_METHOD;
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Return the negation of the expression
     *
     * @return the negation of the expression
     */

    public Expression negate() {
        FunctionCall fc = SystemFunctionCall.makeSystemFunction("exists", getArguments());
        ExpressionTool.copyLocationInfo(this, fc);
        return fc;
    }

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

    /*@NotNull*/
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // See if we can deduce the answer from the cardinality
        int c = argument[0].getCardinality();
        if (c == StaticProperty.ALLOWS_ONE_OR_MORE) {
            return Literal.makeLiteral(BooleanValue.FALSE, getContainer());
        } else if (c == StaticProperty.ALLOWS_ZERO) {
            return Literal.makeLiteral(BooleanValue.TRUE, getContainer());
        }
        argument[0] = argument[0].unordered(false, false);
        // Rewrite
        //    empty(A|B) => empty(A) and empty(B)
        if (argument[0] instanceof VennExpression) {
            VennExpression v = (VennExpression) argument[0];
            if (v.getOperator() == Token.UNION && !visitor.isOptimizeForStreaming()) {
                FunctionCall e0 = SystemFunctionCall.makeSystemFunction("empty", new Expression[]{v.getOperands()[0]});
                FunctionCall e1 = SystemFunctionCall.makeSystemFunction("empty", new Expression[]{v.getOperands()[1]});
                return new AndExpression(e0, e1).optimize(visitor, contextItemType);
            }
        }
        return this;
    }

    /**
     * Evaluate the function
     */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Evaluate the function in a boolean context
     */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        SequenceIterator iter = argument[0].iterate(c);
        boolean result;
        if ((iter.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            result = !((LookaheadIterator) iter).hasNext();
        } else {
            result = iter.next() == null;
        }
        iter.close();
        return result;
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
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        return BooleanValue.get(arguments[0].head() == null);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Empty expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new EmptyCompiler();
    }

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public EmptyAdjunct getStreamingAdjunct() {
        return new EmptyAdjunct();
    }

    //#endif

}

