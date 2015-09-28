////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

/**
 * Implementation of the fn:min() function with 1 or 2 arguments
 */
public class Min extends Minimax {

    public Min() {
        operation = MIN;
    }

    /**
     * Evaluate the function
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public ZeroOrOne<AtomicValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringCollator collator = getCollatorFromLastArgument(arguments, 1, context);
        GenericAtomicComparer comparer = new GenericAtomicComparer(collator, context);
        return new ZeroOrOne<AtomicValue>(minimax(arguments[0].iterate(), MIN, comparer, false, context));
    }
}

