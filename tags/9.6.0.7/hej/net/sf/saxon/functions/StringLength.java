////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.StringLengthCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.StringValue;

/**
 * Implement the XPath string-length() function
 */

public class StringLength extends SystemFunctionCall implements Callable {

    boolean contextPossiblyUndefined = true;

    /**
     * Simplify and validate.
     * This is a pure function so it can be simplified in advance if the arguments are known
     *
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        //useContextItemAsDefault();
        return simplifyArguments(visitor);
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
        return new IntegerValue[]{Int64Value.ZERO, MAX_STRING_LENGTH};
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        int d = super.getIntrinsicDependencies();
        if (argument.length == 0) {
            d |= StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
        }
        return d;
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     *
     * @param visitor an expression visitor
     * @return the expression, either unchanged, or pre-evaluated
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral(
                    evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()), getContainer());
        }
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ContextItemStaticInfo contextInfo) throws XPathException {
        if (argument.length == 0 && contextInfo == null) {
            XPathException err = new XPathException("The context item for string-length() is absent");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (contextInfo != null) {
            contextPossiblyUndefined = contextInfo.isPossiblyAbsent();
        }
        return super.typeCheck(visitor, contextInfo);
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

    public Int64Value evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv;
        if (argument.length == 0) {
            final Item contextItem = c.getContextItem();
            if (contextItem == null) {
                dynamicError("The context item for string-length() is not set", "XPDY0002", c);
                return null;
            }
            sv = StringValue.makeStringValue(contextItem.getStringValueCS());
        } else {
            sv = (AtomicValue) argument[0].evaluateItem(c);
        }
        if (sv == null) {
            return Int64Value.ZERO;
        }

        if (sv instanceof StringValue) {
            return Int64Value.makeIntegerValue(((StringValue) sv).getStringLength());
        } else {
            CharSequence s = sv.getStringValueCS();
            return Int64Value.makeIntegerValue(StringValue.getStringLength(s));
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
        AtomicValue sv;
        if (argument.length == 0) {
            final Item contextItem = context.getContextItem();
            if (contextItem == null) {
                dynamicError("The context item for string-length() is not set", "XPDY0002", context);
                return null;
            }
            sv = StringValue.makeStringValue(contextItem.getStringValueCS());
        } else {
            sv = (AtomicValue) arguments[0].head();
        }
        if (sv == null) {
            return Int64Value.ZERO;
        }

        if (sv instanceof StringValue) {
            return Int64Value.makeIntegerValue(((StringValue) sv).getStringLength());
        } else {
            CharSequence s = sv.getStringValueCS();
            return Int64Value.makeIntegerValue(StringValue.getStringLength(s));
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the StringLength expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new StringLengthCompiler();
    }
//#endif

}

