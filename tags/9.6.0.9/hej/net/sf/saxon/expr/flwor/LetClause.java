////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.List;

/**
 * A "let" clause in a FLWOR expression
 */
public class LetClause extends Clause {

    private LocalVariableBinding rangeVariable;
    private Expression sequence;

    @Override
    public int getClauseKey() {
        return LET;
    }

    public LetClause copy() {
        LetClause let2 = new LetClause();
        let2.setLocationId(getLocationId());
        let2.rangeVariable = rangeVariable.copy();
        let2.sequence = sequence.copy();
        return let2;
    }

    public void setSequence(Expression sequence) {
        this.sequence = sequence;
    }

    public Expression getSequence() {
        return sequence;
    }


    public void setRangeVariable(LocalVariableBinding binding) {
        this.rangeVariable = binding;
    }

    public LocalVariableBinding getRangeVariable() {
        return rangeVariable;
    }

    /**
     * Get the number of variables bound by this clause
     *
     * @return the number of variable bindings
     */
    @Override
    public LocalVariableBinding[] getRangeVariables() {
        return new LocalVariableBinding[]{rangeVariable};
    }

    /**
     * Get a tuple stream that implements the functionality of this clause, taking its
     * input from another tuple stream which this clause modifies
     *
     * @param base    the input tuple stream
     * @param context
     * @return the output tuple stream
     */

    @Override
    public TuplePull getPullStream(TuplePull base, XPathContext context) {
        return new LetClausePull(base, this);
    }

    /**
     * Get a push-mode tuple stream that implements the functionality of this clause, supplying its
     * output to another tuple stream
     *
     * @param destination the output tuple stream
     * @param context
     * @return the push tuple stream that implements the functionality of this clause of the FLWOR
     *         expression
     */
    @Override
    public TuplePush getPushStream(TuplePush destination, XPathContext context) {
        return new LetClausePush(destination, this);
    }

    /**
     * Process the subexpressions of this clause
     *
     * @param processor the expression processor used to process the subexpressions
     */
    @Override
    public void processSubExpressions(ExpressionProcessor processor) throws XPathException {
        sequence = processor.processExpression(sequence);
    }

    /**
     * Type-check the expression
     */

    public void typeCheck(ExpressionVisitor visitor) throws XPathException {
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, rangeVariable.getVariableQName(), 0);
        sequence = TypeChecker.strictTypeCheck(
                sequence, rangeVariable.getRequiredType(), role, visitor.getStaticContext());
    }

    @Override
    public void gatherVariableReferences(final ExpressionVisitor visitor, Binding binding, List<VariableReference> references) {
        ExpressionTool.gatherVariableReferences(sequence, binding, references);
    }

    @Override
    public void refineVariableType(ExpressionVisitor visitor, List<VariableReference> references, Expression returnExpr) {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = sequence.getItemType();
        for (VariableReference ref : references) {
            ref.refineVariableType(actualItemType, sequence.getCardinality(),
                    (sequence instanceof Literal ? ((Literal) sequence).getValue() : null),
                    sequence.getSpecialProperties(), visitor);
            visitor.resetStaticProperties();
        }
    }

    /**
     * Provide additional information about the type of the variable, typically derived by analyzing
     * the initializer of the variable binding
     * @param type the item type of the variable
     * @param cardinality the cardinality of the variable
     * @param constantValue the actual value of the variable, if this is known statically, otherwise null
     * @param properties additional static properties of the variable's initializer
     * @param visitor an ExpressionVisitor
     */

//    public void refineVariableType(
//            ItemType type, int cardinality, Value constantValue, int properties, ExpressionVisitor visitor) {
//        Executable exec = visitor.getExecutable();
//        if (exec == null) {
//            // happens during use-when evaluation
//            return;
//        }
//        TypeHierarchy th = exec.getConfiguration().getTypeHierarchy();
//        ItemType oldItemType = rangeVariable.getRequiredType().getPrimaryType();
//        ItemType newItemType = oldItemType;
//        if (th.isSubType(type, oldItemType)) {
//            newItemType = type;
//        }
//        if (oldItemType instanceof NodeTest && type instanceof AtomicType) {
//            // happens when all references are flattened
//            newItemType = type;
//        }
//        int newcard = cardinality & rangeVariable.getRequiredType().getCardinality();
//        if (newcard==0) {
//            // this will probably lead to a type error later
//            newcard = rangeVariable.getRequiredType().getCardinality();
//        }
//        SequenceType seqType = SequenceType.makeSequenceType(newItemType, newcard);
//        //setStaticType(seqType, constantValue, properties);
//    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */
    @Override
    public void explain(ExpressionPresenter out) {
        out.startElement("let");
        out.emitAttribute("var", getRangeVariable().getVariableQName().getDisplayName());
        out.emitAttribute("slot", getRangeVariable().getLocalSlotNumber() + "");
        sequence.explain(out);
        out.endElement();
    }

    public String toString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
        fsb.append("let $");
        fsb.append(rangeVariable.getVariableQName().getDisplayName());
        fsb.append(" := ");
        fsb.append(sequence.toString());
        return fsb.toString();
    }
}

