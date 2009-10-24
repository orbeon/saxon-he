package net.sf.saxon.expr;

import net.sf.saxon.StandardErrorListener;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
 * Supplied parameter reference: this is an internal expression used to refer to
 * the value of the n'th parameter supplied on a template call (apply-templates).
 * It is used within a type-checking expression designed to check the consistency
 * of the supplied value with the required type. This type checking is all done
 * at run-time, because the binding of apply-templates to actual template rules
 * is entirely dynamic.
 */

public class SuppliedParameterReference extends Expression {

    int slotNumber;
    SequenceType type;

    /**
    * Constructor
    * @param slot identifies this parameter
    */

    public SuppliedParameterReference(int slot) {
        slotNumber = slot;
    }

    /**
     * Set the type of the supplied value if known
     * @param type of the supplied value
     */

    public void setSuppliedType(SequenceType type) {
        this.type = type;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible.
    * @return Type.ITEM, because we don't know the type of the supplied value
    * in advance.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (type != null) {
            return type.getPrimaryType();
        } else {
            return AnyItemType.getInstance();
        }
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
    }

    /**
    * Get the static cardinality
     * @return ZERO_OR_MORE, unless we know the type of the supplied value
     * in advance.
    */

    public int computeCardinality() {
        if (type != null) {
            return type.getCardinality();
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new SuppliedParameterReference(slotNumber);
    }

    /**
    * Get the value of this expression in a given context.
    * @param c the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public ValueRepresentation evaluateVariable(XPathContext c) throws XPathException {
        try {
            return c.evaluateLocalVariable(slotNumber);
        } catch (AssertionError e) {
            StandardErrorListener.printStackTrace(System.err, c);
            throw new AssertionError(e.getMessage() + ". No value has been set for parameter " + slotNumber);
        }
    }

    /**
    * Get the value of this expression in a given context.
    * @param c the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        try {
            return Value.getIterator(c.evaluateLocalVariable(slotNumber));
        } catch (AssertionError e) {
            StandardErrorListener.printStackTrace(System.err, c);
            throw new AssertionError(e.getMessage() + ". No value has been set for parameter " + slotNumber);
        }
    }

    /**
      * Evaluate an expression as a single item. This always returns either a single Item or
      * null (denoting the empty sequence). No conversion is done. This method should not be
      * used unless the static type of the expression is a subtype of "item" or "item?": that is,
      * it should not be called if the expression may return a sequence. There is no guarantee that
      * this condition will be detected.
      *
      * @param context The context in which the expression is to be evaluated
      * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
      *     expression
      * @return the node or atomic value that results from evaluating the
      *     expression; or null to indicate that the result is an empty
      *     sequence
      */

    public Item evaluateItem(XPathContext context) throws XPathException {
        ValueRepresentation actual = context.evaluateLocalVariable(slotNumber);
        return Value.asItem(actual);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("suppliedParam");
        destination.emitAttribute("slot", slotNumber+"");
        destination.endElement();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     * @return a representation of the expression as a string
     */

    public String toString() {
        return "suppliedParam(" + slotNumber + ")";
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
// Contributor(s):
//
