////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.NamespaceUriFnCompiler;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

/**
 * This class supports the namespace-uri() function
 */

public class NamespaceUriFn extends SystemFunctionCall {

    /**
     * Simplify and validate.
     *
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        return simplifyArguments(visitor);
    }

    @Override
    public int getIntrinsicDependencies() {
        if (getNumberOfArguments() == 0) {
            return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
        } else {
            return super.getIntrinsicDependencies();
        }
    }


    /**
     * Evaluate the function in a string context
     */

    public AnyURIValue evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo) argument[0].evaluateItem(c);
        if (node == null) {
            return AnyURIValue.EMPTY_URI;
        }
        return new AnyURIValue(node.getURI());
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
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo node = arguments.length == 0 ? getContextNode(context) : (NodeInfo) arguments[0].head();
        if (node == null) {
            return AnyURIValue.EMPTY_URI;
        } else {
            return new AnyURIValue(node.getURI());
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the NamespaceUriFn expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new NamespaceUriFnCompiler();
    }
//#endif
}

