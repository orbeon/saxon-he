package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionLocation;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

/**
* Implement XPath function fn:error()
*/

public class Error extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Evaluation of the expression always throws an error
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        QualifiedNameValue qname = null;
        if (argument.length > 0) {
            qname = (QualifiedNameValue)argument[0].evaluateItem(context);
        }
        if (qname == null) {
            qname = new QNameValue("err", NamespaceConstant.ERR,
                    (argument.length == 1 ? "FOTY0004" : "FOER0000"),
                    BuiltInAtomicType.QNAME, null);
        }
        String description;
        if (argument.length > 1) {
            description = argument[1].evaluateItem(context).getStringValue();
        } else {
            description = "Error signalled by application call on error()";
        }
        XPathException e = new XPathException(description);
        e.setErrorCode(qname.getNamespaceURI(), qname.getLocalName());
        e.setXPathContext(context);
        e.setLocator(this);
        if (argument.length > 2) {
            Value errorObject = ((Value)SequenceExtent.makeSequenceExtent(argument[2].iterate(context))).reduce();
            if (errorObject instanceof SingletonNode) {
                NodeInfo root = ((SingletonNode)errorObject).getNode();
                if (root.getNodeKind() == Type.DOCUMENT) {
                    XPathEvaluator xpath = new XPathEvaluator();
                    XPathExpression exp = xpath.createExpression("/error/@module");
                    NodeInfo moduleAtt = (NodeInfo)exp.evaluateSingle(root);
                    String module = (moduleAtt == null ? null : moduleAtt.getStringValue());
                    exp = xpath.createExpression("/error/@line");
                    NodeInfo lineAtt = (NodeInfo)exp.evaluateSingle(root);
                    int line = (lineAtt == null ? -1 : Integer.parseInt(lineAtt.getStringValue()));
                    exp = xpath.createExpression("/error/@column");
                    NodeInfo columnAtt = (NodeInfo)exp.evaluateSingle(root);
                    int column = (columnAtt == null ? -1 : Integer.parseInt(columnAtt.getStringValue()));
                    ExpressionLocation locator = new ExpressionLocation();
                    locator.setSystemId(module);
                    locator.setLineNumber(line);
                    locator.setColumnNumber(column);
                    e.setLocator(locator);
                }
            }
            e.setErrorObject(errorObject);
        }
        throw e;
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
