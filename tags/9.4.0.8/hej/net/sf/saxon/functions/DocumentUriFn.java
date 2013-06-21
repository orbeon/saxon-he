package net.sf.saxon.functions;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.DocumentPool;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AnyURIValue;

/**
* This class supports the document-uri() function
*/

public class DocumentUriFn extends SystemFunction implements CallableExpression {

    /**
     * Simplify and validate.
     * @param visitor an expression visitor
     */

     /*@NotNull*/
     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        return simplifyArguments(visitor);
    }

    /**
    * Evaluate the function in a string context
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            return null;
        }
        return getDocumentURI(node, c);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator<AnyURIValue> call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        NodeInfo node = (NodeInfo)arguments[0].next();
        if (node==null) {
            return EmptyIterator.emptyIterator();
        }
        return SingletonIterator.makeIterator(getDocumentURI(node, context));
    }

    public static AnyURIValue getDocumentURI(NodeInfo node, XPathContext c) {
        if (node.getNodeKind() == Type.DOCUMENT) {
            final Controller controller = c.getController();
            assert controller != null;
            DocumentPool pool = controller.getDocumentPool();
            String docURI = pool.getDocumentURI(node);
            if (docURI == null) {
                docURI = node.getSystemId();
            }
            if (docURI == null) {
                return null;
            } else if ("".equals(docURI)) {
                return null;
            } else {
                return new AnyURIValue(docURI);
            }
        } else {
            return null;
        }
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