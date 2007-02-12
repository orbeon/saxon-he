package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
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

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
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
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
     * Evaluate the expression
     * @param context
     * @return the result of evaluating the expression (a BooleanValue)
     * @throws net.sf.saxon.trans.XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        // suppress all error messages while attempting to fetch the document
        ErrorListener old = controller.getErrorListener();
        controller.setErrorListener(new ErrorListener() {
            public void warning(TransformerException exception) {}
            public void error(TransformerException exception) {}
            public void fatalError(TransformerException exception) {}
        });
        boolean b = docAvailable(context);
        controller.setErrorListener(old);
        return BooleanValue.get(b);
    }

    private boolean docAvailable(XPathContext context) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return false;
        }
        String href = hrefVal.getStringValue();
        try {
            Item item = Document.makeDoc(href, expressionBaseURI, context, this);
            if (item==null) {
                return false;
            }
            return true;
        } catch (Exception err) {
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
