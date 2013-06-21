////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.bytecode.ExpressionCompiler;
import com.saxonica.bytecode.LastCompiler;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

import java.util.List;

/**
* Implement the XPath 2.0 function fn:last()
*/

public class Last extends SystemFunctionCall {

    boolean contextPossiblyUndefined = true;

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
        return new IntegerValue[]{Int64Value.PLUS_ONE, MAX_SEQUENCE_LENGTH};
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (contextItemType == null || contextItemType.itemType == null) {
            XPathException err = new XPathException("The context for last() is absent");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else {
            contextPossiblyUndefined = contextItemType.contextMaybeUndefined;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        return this;
    }

    /**
      * Ask whether the context item may possibly be undefined
      * @return true if it might be undefined
      */

     public boolean isContextPossiblyUndefined() {
         return contextPossiblyUndefined;
     }

//#ifdefined BYTECODE
    @Override
    public int getStreamability(int syntacticContext, boolean allowExtensions, List<String> reasons) {
        if (reasons != null) {
            reasons.add("Call to last() is not streamable");
        }
        return W3C_FREE_RANGING;
    }
//#endif

    /**
    * Evaluate in a general context
    */

    public Int64Value evaluateItem(XPathContext c) throws XPathException {
        return Int64Value.makeIntegerValue(c.getLast());
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_LAST;
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
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        return Int64Value.makeIntegerValue(context.getLast());
    }

//#ifdefined BYTECODE
     /**
     * Return the compiler of the Last expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LastCompiler();
    }
//#endif
}

