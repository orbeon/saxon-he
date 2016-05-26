////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.regex.ARegularExpression;
import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Whitespace;

import java.util.ArrayList;


/**
 * This class implements the single-argument tokenize() function introduced in XPath 3.1
 */

public class Tokenize_1 extends SystemFunction {

    private static RegularExpression WHITESPACE_SEP;

    static {
        try {
            WHITESPACE_SEP = new ARegularExpression(" ", "", "XP30", new ArrayList<String>());
        } catch (XPathException e) {
            throw new AssertionError(e);
        }
    }

    public static SequenceIterator splitOnWhitespace(CharSequence input) {
        input = Whitespace.collapseWhitespace(input);
        if (input.length() == 0) {
            return EmptyIterator.emptyIterator();
        }
        return WHITESPACE_SEP.tokenize(input);
    }

    /**
     * Evaluate the expression dynamically
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        AtomicValue sv = (AtomicValue) arguments[0].head();
        if (sv == null) {
            return EmptySequence.getInstance();
        }
        CharSequence input = sv.getStringValueCS();
        return SequenceTool.toLazySequence(splitOnWhitespace(input));
    }
}

