package net.sf.saxon.functions;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.DocumentPool;
import net.sf.saxon.om.DocumentURI;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * Implement the fn:doc-available() function
 */

public class DocAvailable extends SystemFunction {

    private String expressionBaseURI = null;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
     * Get the static base URI of the expression
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
     * Evaluate the expression
     * @param context
     * @return the result of evaluating the expression (a BooleanValue)
     * @throws net.sf.saxon.trans.XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return BooleanValue.FALSE;
        }
        String href = hrefVal.getStringValue();

        // suppress all error messages while attempting to fetch the document
        Controller controller = context.getController();
        ErrorListener old = controller.getErrorListener();
        controller.setErrorListener(ErrorDiscarder.THE_INSTANCE);
        boolean b = docAvailable(href, context);
        controller.setErrorListener(old);
        return BooleanValue.get(b);
    }

    private static class ErrorDiscarder implements ErrorListener {
        public static ErrorDiscarder THE_INSTANCE = new ErrorDiscarder();
        public void warning(TransformerException exception) {}
        public void error(TransformerException exception) {}
        public void fatalError(TransformerException exception) {}

    }

    private boolean docAvailable(String href, XPathContext context) throws XPathException {
        try {
            DocumentURI documentKey = DocumentFn.computeDocumentKey(href, expressionBaseURI, context);
            DocumentPool pool = context.getController().getDocumentPool();
            if (pool.isMarkedUnavailable(documentKey)) {
                return false;
            }
            DocumentInfo doc = pool.find(documentKey);
            if (doc != null) {
                return true;
            }
            Item item = DocumentFn.makeDoc(href, expressionBaseURI, context, this);
            if (item != null) {
                return true;
            } else {
                // The document does not exist; ensure that this remains the case
                pool.markUnavailable(documentKey);
                return false;
            }
        } catch (XPathException e) {
            return false;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
