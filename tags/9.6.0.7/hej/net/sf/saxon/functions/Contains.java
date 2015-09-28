////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ContainsCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.RuleBasedSubstringMatcher;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.lib.SubstringMatcher;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

import java.text.RuleBasedCollator;

/**
 * Implements the fn:contains() function
 */
public class Contains extends CollatingFunction implements Callable {


    /**
     * Get the argument position (0-based) containing the collation name
     *
     * @return the position of the argument containing the collation URI
     */
    @Override
    protected int getCollationArgument() {
        return 2;
    }

    /**
     * Evaluate the function
     */

    public BooleanValue evaluateItem(/*@NotNull*/ XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        try {
            StringValue arg0 = (StringValue) argument[0].evaluateItem(context);
            StringValue arg1 = (StringValue) argument[1].evaluateItem(context);
            return contains(arg0, arg1, getCollator(context));
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }

    private static boolean contains(StringValue arg0, StringValue arg1, StringCollator collator) throws XPathException {
        if (arg1 == null || arg1.isZeroLength()) {
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
            if (collator instanceof SimpleCollation &&
                    ((SimpleCollation) collator).getCollation() instanceof RuleBasedCollator) {
                collator = new RuleBasedSubstringMatcher((RuleBasedCollator) ((SimpleCollation) collator).getCollation());
            }

            if (collator instanceof SubstringMatcher) {
                return ((SubstringMatcher) collator).contains(s0, s1);
            } else {
                throw new XPathException("The collation requested for fn:contains does not support substring matching", "FOCH0004");
            }
        }
    }


    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        try {
            StringValue s0 = (StringValue) arguments[0].head();
            StringValue s1 = (StringValue) arguments[1].head();
            return BooleanValue.get(contains(s0, s1, getCollatorFromLastArgument(arguments, 2, context)));
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
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

