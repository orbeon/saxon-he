package net.sf.saxon.expr;

import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * Interface implemented by an expression that takes a single sequence as input and
 * returns a sequence as output; this method delivers an iterator that maps the input to the
 * output.
 */
public interface MappingExpression {

    /**
     * Return an iterator over the results of the expression, given an iterator over the principal
     * operand to the expression
     * @param base an interator over the input to the expression
     * @param context dynamic evaluation context
     * @return an iterator over the results of the expression
     * @throws XPathException
     */

    public SequenceIterator getMappingIterator(SequenceIterator base, XPathContext context) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

