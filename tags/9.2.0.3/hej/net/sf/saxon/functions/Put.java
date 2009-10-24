package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.trans.Err;
import net.sf.saxon.Platform;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.PendingUpdateList;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implements the fn:put() function in XQuery Update 1.0.
 */
public class Put extends SystemFunction {

    String expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
            if (expressionBaseURI == null && argument.length == 1) {
                XPathException de = new XPathException("Base URI in static context of resolve-uri() is unknown");
                de.setErrorCode("FONS0005");
                throw de;
            }
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        Put d = (Put)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        return d;
    }

    /**
     * Determine whether this is an updating expression as defined in the XQuery update specification
     * @return true if this is an updating expression
     */

    public boolean isUpdatingExpression() {
       return true;
    }

    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(context);
        int kind = node.getNodeKind();
        if (kind != Type.ELEMENT && kind != Type.DOCUMENT) {
            dynamicError("Node in put() must be a document or element node",
                    "FOUP0001", context);
        }
        String relative = argument[1].evaluateItem(context).getStringValue();
        Platform platform = Configuration.getPlatform();
        String abs;
        try {
            URI resolved = ResolveURI.makeAbsolute(relative,  expressionBaseURI);
            abs = resolved.toString();
        } catch (URISyntaxException err) {
            dynamicError("Base URI " + Err.wrap(expressionBaseURI) + " is invalid: " + err.getMessage(),
                    "FOUP0002", context);
            abs = null;
        }
        pul.addPutAction(node, abs, this);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

