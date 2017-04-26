////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ContainsCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SystemFunctionCall;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.lib.SubstringMatcher;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

/**
 * Implements the fn:contains() function, with the collation already known
 */
public class Contains extends CollatingFunctionFixed {

    /**
     * Allow the function to create an optimized call based on the values of the actual arguments
     *
     * @param visitor     the expression visitor
     * @param contextInfo information about the context item
     * @param arguments   the supplied arguments to the function call. Note: modifying the contents
     *                    of this array should not be attempted, it is likely to have no effect.
     * @return either a function call on this function, or an expression that delivers
     * the same result, or null indicating that no optimization has taken place
     * @throws XPathException if an error is detected
     */
    @Override
    public Expression makeOptimizedFunctionCall(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, final Expression... arguments) throws XPathException {
        if (getStringCollator() == CodepointCollator.getInstance()) {
            // Performance fast path: bug 3209
            return new SystemFunctionCall(this, arguments) {
                @Override
                public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
                    String s0 = getArg(0).evaluateAsString(context).toString();
                    String s1 = getArg(1).evaluateAsString(context).toString();
                    return s0.contains(s1);
                }

                @Override
                public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
                    return this; // prevent infinite optimization loops
                }
            };
        } else {
            return super.makeOptimizedFunctionCall(visitor, contextInfo, arguments);
        }
    }

    private static boolean contains(StringValue arg0, StringValue arg1, StringCollator collator) throws XPathException {
        if (arg1 == null || arg1.isZeroLength() || collator.comparesEqual(arg1.getPrimitiveStringValue(), "")) {
            return true;
        }
        if (arg0 == null || arg0.isZeroLength()) {
            return false;
        }
        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();
        if (collator instanceof CodepointCollator) {
            // fast path for this common case
            return s0.indexOf(s1, 0) >= 0;
        } else {
            if (collator instanceof SimpleCollation) {
                collator = ((SimpleCollation)collator).getSubstringMatcher();
            }

            if (collator instanceof SubstringMatcher) {
                return ((SubstringMatcher) collator).contains(s0, s1);
            } else {
                throw new XPathException("The collation requested for fn:contains does not support substring matching", "FOCH0004");
            }
        }
    }

    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue s0 = (StringValue) arguments[0].head();
        StringValue s1 = (StringValue) arguments[1].head();
        return BooleanValue.get(contains(s0, s1, getStringCollator()));
    }


    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Contains expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ContainsCompiler();
    }
//#endif

}

