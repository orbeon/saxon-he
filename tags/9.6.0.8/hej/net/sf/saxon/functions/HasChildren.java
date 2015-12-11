////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

/**
 * This class implements the function fn:has-children($node), which is a standard function in XPath 3.0
 */

public class HasChildren extends SystemFunctionCall {

    /**
     * Simplify and validate.
     *
     * @param visitor an expression visitor
     */

    public Expression simplify(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        return simplifyArguments(visitor);
    }

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        NodeInfo arg = (NodeInfo) argument[0].evaluateItem(context);
        if (arg == null) {
            return BooleanValue.FALSE;
        }
        return BooleanValue.get(arg.hasChildNodes());
    }

    /**
     * Evaluate the function dynamically
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo arg = arguments.length == 0 ? getContextNode(context) : (NodeInfo) arguments[0].head();
        if (arg == null) {
            return BooleanValue.FALSE;
        }
        return BooleanValue.get(arg.hasChildNodes());
    }
}

// Copyright (c) 2012 Saxonica Limited. All rights reserved.