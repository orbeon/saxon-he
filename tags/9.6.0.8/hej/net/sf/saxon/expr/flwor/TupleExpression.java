////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.flwor;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.TupleExpressionCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.JavaExternalObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * A tuple expression is an expression that returns a tuple. Specifically,
 * it is a list of slot numbers of local variables; it returns a Tuple item
 * containg the current value of these variables.
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
    public ItemType getItemType() {
        return new JavaExternalObjectType(Object.class, getConfiguration());
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor,
                                /*@Nullable*/ ContextItemStaticInfo contextInfo)
            throws XPathException {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = (LocalVariableReference) visitor.typeCheck(slots[i], contextInfo);
        }
        return this;
    }

    @Override
    public Iterable<Operand> operands() {
        List<Operand> list = new ArrayList<Operand>();
        for (Expression e : slots) {
            list.add(new Operand(e, OperandRole.SAME_FOCUS_ACTION));
        }
        return list;
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */
    @Override
    public boolean replaceOperand(Expression original, Expression replacement) {
        for (int i = 0; i < slots.length; i++) {
            if (original == slots[i]) {
                slots[i] = (LocalVariableReference) replacement;
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
        for (int i = 0; i < slots.length; i++) {
            refs2[i] = (LocalVariableReference) slots[i].copy();
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
        for (LocalVariableReference slot : slots) {
            slot.explain(out);
        }
        out.endElement();
    }


    public Tuple evaluateItem(XPathContext context) throws XPathException {
        Sequence[] tuple = new Sequence[slots.length];
        for (int i = 0; i < slots.length; i++) {
            tuple[i] = slots[i].evaluateVariable(context);
        }
        return new Tuple(tuple);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Tuple expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new TupleExpressionCompiler();
    }
//#endif

    /**
     * Set the local variables in the current stack frame to values corresponding to a supplied tuple
     *
     * @param context identifies the stack frame to be modified
     * @param tuple   the tuple containing the current values
     */

    public void setCurrentTuple(XPathContext context, Tuple tuple) {
        Sequence[] members = tuple.getMembers();
        for (int i = 0; i < slots.length; i++) {
            context.setLocalVariable(slots[i].getBinding().getLocalSlotNumber(), members[i]);
        }
    }

    /**
     * Get the cardinality of the expression. This is exactly one, in the sense
     * that evaluating the TupleExpression returns a single tuple.
     *
     * @return the static cardinality - EXACTLY_ONE
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public int getIntrinsicDependencies() {
        return 0;
    }

}

