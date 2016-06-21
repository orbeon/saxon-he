////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import com.saxonica.ee.bytecode.ArrayBlockCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.BlockAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.oper.OperandArray;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.ArrayList;
import java.util.List;


/**
 * An expression that delivers a fixed size array whose members are the result of evaluating
 * corresponding expressions: [a,b,c,d]
 */

public class ArrayBlock extends Expression {


    private OperandArray operanda;


    /**
     * Create an empty block
     */

    public ArrayBlock(List<Expression> children) {
        Expression[] kids = children.toArray(new Expression[children.size()]);
        setOperanda(new OperandArray(this, kids, OperandRole.SAME_FOCUS_ACTION));
    }

    /**
     * Set the data structure for the operands of this expression. This must be created during initialisation of the
     * expression and must not be subsequently changed
     *
     * @param operanda the data structure for expression operands
     */

    protected void setOperanda(OperandArray operanda) {
        this.operanda = operanda;
    }

    /**
     * Get the data structure holding the operands of this expression.
     *
     * @return the data structure holding expression operands
     */

    public OperandArray getOperanda() {
        return operanda;
    }

    @Override
    public Iterable<Operand> operands() {
        return operanda.operands();
    }


    public String getExpressionName() {
        return "arrayBlock";
    }


    public int computeSpecialProperties() {
        return 0;
    }

    /**
     * Determine whether the block includes any instructions that might return nodes with a type annotation
     *
     * @param th the type hierarchy cache
     * @return true if any expression in the block can return type-annotated nodes
     */

    private boolean mayReturnTypedNodes(TypeHierarchy th) {
        for (Operand o : operands()) {
            Expression exp = o.getChildExpression();
            if ((exp.getSpecialProperties() & StaticProperty.ALL_NODES_UNTYPED) == 0) {
                ItemType it = exp.getItemType();
                if (th.relationship(it, NodeKindTest.ELEMENT) != TypeHierarchy.DISJOINT ||
                    th.relationship(it, NodeKindTest.ATTRIBUTE) != TypeHierarchy.DISJOINT ||
                    th.relationship(it, NodeKindTest.ATTRIBUTE) != TypeHierarchy.DISJOINT) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Is this expression the same as another expression?
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ArrayBlock)) {
            return false;
        } else {
            ArrayBlock ab2 = (ArrayBlock) other;
            if (ab2.getOperanda().getNumberOfOperands() != getOperanda().getNumberOfOperands()) {
                return false;
            }
            for (int i = 0; i < getOperanda().getNumberOfOperands(); i++) {
                if (!getOperanda().getOperand(i).equals(ab2.getOperanda().getOperand(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        int h = 0x878b92a0;
        for (Operand o : operands()) {
            h ^= o.getChildExpression().hashCode();
        }
        return h;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        List<Expression> m2 = new ArrayList<Expression>(getOperanda().getNumberOfOperands());
        for (Operand o : operands()) {
            m2.add(o.getChildExpression().copy(rebindings));
        }
        ArrayBlock b2 = new ArrayBlock(m2);
        ExpressionTool.copyLocationInfo(this, b2);
        return b2;
    }

    /**
     * Determine the data type of the items returned by this expression
     *
     * @return the data type
     */

    /*@NotNull*/
    public final ItemType getItemType() {
        return ArrayItemType.ANY_ARRAY_TYPE;
        // TODO: could get a more precise answer
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int computeCardinality() {
        // An array is an item!
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any child instruction
     * returns true.
     */

    public final boolean createsNewNodes() {
        for (Operand o : operands()) {
            int props = o.getChildExpression().getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("arrayBlock", this);
        for (Operand o : operands()) {
            o.getChildExpression().export(out);
        }
        out.endElement();
    }

    @Override
    public String toShortString() {
        if (getOperanda().getNumberOfOperands() == 0) {
            return "[]";
        } else {
            return "[" + getOperanda().getOperand(0).getChildExpression().toShortString() + ", ...]";
        }
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the node or atomic value that results from evaluating the
     * expression; or null to indicate that the result is an empty
     * sequence
     * @throws net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *                                           expression
     */
    @Override
    public Item evaluateItem(XPathContext context) throws XPathException {
        List<Sequence> value = new ArrayList<Sequence>(getOperanda().getNumberOfOperands());
        for (Operand o : operands()) {
            Sequence s = ExpressionTool.eagerEvaluate(o.getChildExpression(), context);
            value.add(s);
        }
        return new SimpleArrayItem(value);
    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Block expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ArrayBlockCompiler();
    }
    //#endif
//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public BlockAdjunct getStreamingAdjunct() {
        // TODO
        return new BlockAdjunct();
    }

    //#endif
}