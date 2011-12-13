package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LocalVariableReference;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ExternalObjectType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A tuple expression is an expression that returns a tuple. Specifically,
 * it is a list of slot numbers of local variables; it returns a Tuple item
 * containg the current value of these variables.
 *
 */
public class TupleExpression extends Expression {

    LocalVariableReference[] slots;

    public TupleExpression() {
        slots = new LocalVariableReference[0]; // temporarily
    }

    public void setVariables(List<LocalVariableReference> refs) {
        slots = new LocalVariableReference[refs.size()];
        slots = refs.toArray(slots);
    }

    public LocalVariableReference[] getSlots() {
        return slots;
    }

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return new ExternalObjectType(Object.class, th.getConfiguration());
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */
    /*@NotNull*/
    @Override
    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((Expression[])slots).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */
    @Override
    public boolean replaceSubExpression(Expression original, Expression replacement) {
        for (int i=0; i<slots.length; i++) {
            if (original == slots[i]) {
                slots[i] = (LocalVariableReference)replacement;
                return true;
            }
        }
        return false;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        LocalVariableReference[] refs2 = new LocalVariableReference[slots.length];
        for (int i=0; i<slots.length; i++) {
            refs2[i] = (LocalVariableReference)slots[i].copy();
        }
        TupleExpression t2 = new TupleExpression();
        t2.slots = refs2;
        return t2;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("tuple");
        for (int i=0; i<slots.length; i++) {
            slots[i].explain(out);
        }
        out.endElement();
    }


    public Tuple evaluateItem(XPathContext context) throws XPathException {
        ValueRepresentation[] tuple = new ValueRepresentation[slots.length];
        for (int i=0; i<slots.length; i++) {
            tuple[i] = slots[i].evaluateVariable(context);
        }
        return new Tuple(tuple);
    }

    /**
     * Set the local variables in the current stack frame to values corresponding to a supplied tuple
     * @param context identifies the stack frame to be modified
     * @param tuple the tuple containing the current values
     */

    public void setCurrentTuple(XPathContext context, Tuple tuple) {
        ValueRepresentation[] members = tuple.getMembers();
        for (int i=0; i<slots.length; i++) {
            context.setLocalVariable(slots[i].getBinding().getLocalSlotNumber(), members[i]);
        }
    }

    /**
     * Get the cardinality of the expression. This is exactly one, in the sense
     * that evaluating the TupleExpression returns a single tuple.
     * @return the static cardinality - EXACTLY_ONE
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public int getIntrinsicDependencies() {
        return 0;
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