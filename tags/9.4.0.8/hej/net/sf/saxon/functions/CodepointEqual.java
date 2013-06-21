package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;

/**
 * XPath 2.0 codepoint-equal() function.
 * Compares two strings using the unicode codepoint collation. (The function was introduced
 * specifically to allow URI comparison: URIs are promoted to strings when necessary.)
 */

public class CodepointEqual extends SystemFunction implements CallableExpression {

	/**
	 * Evaluate the expression
	 */

	/*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
		AtomicValue op1 = (AtomicValue)argument[0].evaluateItem(context);
		if (op1 == null) {
			return null;
		}
		AtomicValue op2 = (AtomicValue)argument[1].evaluateItem(context);
		if (op2 == null) {
			return null;
		}

		return BooleanValue.get(op1.getStringValue().equals(op2.getStringValue()));
	}

	public SequenceIterator call(SequenceIterator[] arguments,
			XPathContext context) throws XPathException {

		AtomicValue op1 = (AtomicValue)arguments[0].next();
		if (op1 == null) {
			return null;
		}
		AtomicValue op2 = (AtomicValue)arguments[1].next();
		if (op2 == null) {
			return null;
		}
		BooleanValue bValue = BooleanValue.get(op1.getStringValue().equals(op2.getStringValue()));
		
		return SingletonIterator.makeIterator(bValue);
	}

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