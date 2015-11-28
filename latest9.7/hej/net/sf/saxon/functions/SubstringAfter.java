////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SubstringAfterCompiler;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.RuleBasedSubstringMatcher;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.lib.SubstringMatcher;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import java.text.RuleBasedCollator;

/**
 * Implements the fn:substring-after() function with the collation already known
 */
public class SubstringAfter extends CollatingFunctionFixed {

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue arg1 = (StringValue) arguments[0].head();
        StringValue arg2 = (StringValue) arguments[1].head();
        return substringAfter(arg1, arg2, getStringCollator());
    }

    private static StringValue substringAfter(StringValue arg1, StringValue arg2, StringCollator collator) throws XPathException {
        if (arg1 == null) {
            arg1 = StringValue.EMPTY_STRING;
        }
        if (arg2 == null) {
            arg2 = StringValue.EMPTY_STRING;
        }
        if (arg2.isZeroLength()) {
            return arg1;
        }
        if (arg1.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        String s1 = arg1.getStringValue();
        String s2 = arg2.getStringValue();

        String result = null;
        if (collator instanceof CodepointCollator) {
            // fast path for this common case
            int i = s1.indexOf(s2);
            if (i < 0) {
                result = "";
            } else {
                result = s1.substring(i + s2.length());
            }

        } else {
            if (collator instanceof SimpleCollation &&
                    ((SimpleCollation) collator).getComparator() instanceof RuleBasedCollator) {
                collator = new RuleBasedSubstringMatcher(collator.getCollationURI(), (RuleBasedCollator) ((SimpleCollation) collator).getComparator());
            }

            if (collator instanceof SubstringMatcher) {
                result = ((SubstringMatcher) collator).substringAfter(s1, s2);
            } else {
                throw new XPathException("The collation requested for fn:substring-after" +
                        " does not support substring matching", "FOCH0004");
            }
        }
        StringValue s = StringValue.makeStringValue(result);
        if (arg1.isKnownToContainNoSurrogates()) {
            s.setContainsNoSurrogates();
        }
        return s;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the SubstringAfter expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SubstringAfterCompiler();
    }
//#endif

}