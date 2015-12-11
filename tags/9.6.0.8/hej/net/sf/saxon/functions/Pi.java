////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.SequenceType;


/**
 * This class implements the function math:pi(), which is a standard function in XPath 3.0 and XQuery 3.0
 */

public class Pi extends ExtensionFunctionDefinition {

    private static final StructuredQName PI = new StructuredQName("math", NamespaceConstant.MATH, "pi");

    public StructuredQName getFunctionQName() {
        return PI;
    }

    public int getMinimumNumberOfArguments() {
        return 0;
    }

    public int getMaximumNumberOfArguments() {
        return 0;
    }

    public boolean trustResultType() {
        return true;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[0];
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_DOUBLE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {

            public Expression rewrite(StaticContext context, Expression[] arguments) throws XPathException {
                return Literal.makeLiteral(new DoubleValue(Math.PI), getContainer());
            }

            public DoubleValue call(XPathContext context, Sequence[] arguments) throws XPathException {
                // Should not be called
                return new DoubleValue(Math.PI);
            }
        };
    }

}

// Copyright (c) 2010 Saxonica Limited. All rights reserved.