package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.Whitespace;

import java.util.ArrayList;
import java.util.Iterator;

/**
* A namespace constructor instruction. (xsl:namespace in XSLT 2.0, or namespace{}{} in XQuery 1.1)
*/

public class NamespaceConstructor extends SimpleNodeConstructor {

    private Expression name;

    /**
     * Create an xsl:namespace instruction for dynamic construction of namespace nodes
     * @param name the expression to evaluate the name of the node (that is, the prefix)
     */

    public NamespaceConstructor(Expression name) {
        this.name = name;
        adoptChildExpression(name);
    }

    /**
    * Set the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NAMESPACE;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        name = visitor.simplify(name);
        return super.simplify(visitor);
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.NAMESPACE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (select != null) {
            select = doPromotion(this, select, offer);
        }
        name = doPromotion(this, name, offer);
        super.promoteInst(offer);
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        visitor.typeCheck(name, contextItemType);
    }

    public Iterator<Expression> iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
        list.add(name);
        return list.iterator();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        NamespaceConstructor exp = new NamespaceConstructor(name.copy());
        try {
            exp.setSelect(select.copy(), getExecutable().getConfiguration());
        } catch (XPathException err) {
            throw new UnsupportedOperationException(err.getMessage());
        }
        return exp;
    }


    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (name == original) {
            name = replacement;
            found = true;
        }
                return found;
    }


    private String evaluatePrefix(XPathContext context) throws XPathException {
        String prefix = Whitespace.trim(name.evaluateAsString(context));
        if (!(prefix.length() == 0 || context.getConfiguration().getNameChecker().isValidNCName(prefix))) {
            XPathException err = new XPathException("Namespace prefix is invalid: " + prefix, this);
            err.setErrorCode(isXSLT() ? "XTDE0920" : "FORG0001");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (prefix.equals("xmlns")) {
            XPathException err = new XPathException("Namespace prefix 'xmlns' is not allowed", this);
            err.setErrorCode(isXSLT() ? "XTDE0920" : "XQDY0101");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }
        return prefix;
    }

    public int evaluateNameCode(XPathContext context) throws XPathException {
        String prefix = evaluatePrefix(context);
        return context.getNamePool().allocate("", "", prefix);
    }

    public void processValue(CharSequence value, XPathContext context) throws XPathException {
        Controller controller = context.getController();
        String prefix = evaluatePrefix(context);
        String uri = value.toString();
        checkPrefixAndUri(prefix, uri, context);

        int nscode = controller.getNamePool().allocateNamespaceCode(prefix, uri);
        SequenceReceiver out = context.getReceiver();
        out.namespace(nscode, ReceiverOptions.REJECT_DUPLICATES);
        //return null;
    }


    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node = (NodeInfo)super.evaluateItem(context);
        String prefix = node.getLocalPart();
        String uri = node.getStringValue();
        checkPrefixAndUri(prefix, uri, context);
        return node;
    }

    private void checkPrefixAndUri(String prefix, String uri, XPathContext context) throws XPathException {
        if (prefix.equals("xml") != uri.equals(NamespaceConstant.XML)) {
            XPathException err = new XPathException("Namespace prefix 'xml' and namespace uri " + NamespaceConstant.XML +
                    " must only be used together", this);
            err.setErrorCode(isXSLT() ? "XTDE0925" : "XQDY0101");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (uri.length() == 0) {
            XPathException err = new XPathException("Namespace URI is an empty string", this);
            err.setErrorCode("XTDE0930");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (!AnyURIValue.isValidURI(uri)) {
            XPathException de = new XPathException("The string value of the constructed namespace node must be a valid URI");
            de.setErrorCode("XTDE0905");
            de.setXPathContext(context);
            de.setLocator(this);
            throw de;
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("namespace");
        out.startSubsidiaryElement("name");
        name.explain(out);
        out.endSubsidiaryElement();
        out.startSubsidiaryElement("select");
        getSelect().explain(out);
        out.endSubsidiaryElement();
        out.endElement();
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
