package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;


/**
* Error expression: this expression is generated when the supplied expression cannot be
* parsed, and the containing element enables forwards-compatible processing. It defers
* the generation of an error message until an attempt is made to evaluate the expression
*/

public class ErrorExpression extends Expression {

    private XPathException exception;     // the error found when parsing this expression

    /**
    * Constructor
    * @param exception the error found when parsing this expression
    */

    public ErrorExpression(XPathException exception) {
        this.exception = exception;
        exception.setLocator(this); // to remove any links to the compile-time stylesheet objects
    };

    /**
     * Get the wrapped exception
     */

    public XPathException getException() {
        return exception;
    }

    /**
    * Type-check the expression.
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Evaluate the expression. This always throws the exception registered when the expression
    * was first parsed.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // copy the exception for thread-safety, because we want to add context information
        XPathException err = new XPathException(exception.getMessage());
        err.setLocator(this);
        err.setErrorCodeQName(exception.getErrorCodeQName());
        err.setXPathContext(context);
        throw err;
    }

    /**
    * Iterate over the expression. This always throws the exception registered when the expression
    * was first parsed.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        evaluateItem(context);
        return null;    // to fool the compiler
    }

    /**
    * Determine the data type of the expression, if possible
    * @return Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
        // we return a liberal value, so that we never get a type error reported
        // statically
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new ErrorExpression(exception);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("error");
        destination.emitAttribute("message", exception.getMessage());
        destination.endElement();
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
