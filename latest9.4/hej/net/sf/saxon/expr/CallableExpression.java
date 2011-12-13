package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * A generic interface for calling expressions by supplying the values of their subexpressions
 */

public interface CallableExpression {

    /**
     * Get the subexpressions (arguments to this expression)
     * @return the arguments, as an array
     */

    public Expression[] getArguments();

    /**
     * Evaluate the expression
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws XPathException if a dynamic error occurs during the evaluation of the expression
     */
	
	/*@Nullable*/ public SequenceIterator<? extends Item> call(SequenceIterator<? extends Item>[] arguments, XPathContext context) throws XPathException;

}
//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//