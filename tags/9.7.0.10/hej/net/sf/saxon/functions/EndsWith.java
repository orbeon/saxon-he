////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.StartsWithCompiler;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.lib.SubstringMatcher;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

/**
 * Implements the fn:ends-with() function, with the collation already fixed
 */
public class EndsWith extends CollatingFunctionFixed {

    public static boolean endsWith(StringValue arg0, StringValue arg1, StringCollator collator) throws XPathException {
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
            return s0.endsWith(s1);
        } else {
            if (collator instanceof SimpleCollation) {
                collator = ((SimpleCollation) collator).getSubstringMatcher();
            }

            if (collator instanceof SubstringMatcher) {
                return ((SubstringMatcher) collator).endsWith(s0, s1);
            } else {
                throw new XPathException("The collation requested for ends-with() does not support substring matching", "FOCH0004");
            }
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
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue s0 = (StringValue) arguments[0].head();
        StringValue s1 = (StringValue) arguments[1].head();
        return BooleanValue.get(endsWith(s0, s1, getStringCollator()));
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the EndsWith expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new StartsWithCompiler();
    }
//#endif

}

