////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.InstanceOfCompiler;
import com.saxonica.ee.stream.adjunct.InstanceOfAdjunct;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceType;

/**
 * InstanceOf Expression: implements "Expr instance of data-type"
 */

public final class InstanceOfExpression extends UnaryExpression {

    ItemType targetType;
    int targetCardinality;

    /**
     * Construct an "instance of" expression in the form "source instance of target"
     *
     * @param source the expression whose type is to be tested
     * @param target the type against which it is tested
     */

    public InstanceOfExpression(Expression source, SequenceType target) {
        super(source);
        targetType = target.getPrimaryType();
        if (targetType == null) {
            throw new IllegalArgumentException("Primary item type must not be null");
        }
        targetCardinality = target.getCardinality();
    }

    protected OperandRole getOperandRole() {
        return OperandRole.INSPECT;
    }

    /**
     * Get the item type that we are testing for membership of
     *
     * @return the item type
     */

    public ItemType getRequiredItemType() {
        return targetType;
    }

    /**
     * Get the cardinality that we are testing for membership of
     *
     * @return the required cardinality
     */

    public int getRequiredCardinality() {
        return targetCardinality;
    }

    /**
     * Type-check the expression
     *
     * @return the checked expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        Expression operand = getBaseExpression();
        if (operand instanceof Literal) {
            Literal lit = Literal.makeLiteral(
                    evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
            ExpressionTool.copyLocationInfo(this, lit);
            return lit;
        }

        // See if we can get the answer by static analysis.

        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            final TypeHierarchy th = getConfiguration().getTypeHierarchy();
            int relation = th.relationship(operand.getItemType(), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                Literal lit = Literal.makeLiteral(BooleanValue.TRUE);
                ExpressionTool.copyLocationInfo(this, lit);
                return lit;
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(operand.getCardinality())) {
                    Literal lit = Literal.makeLiteral(BooleanValue.FALSE);
                    ExpressionTool.copyLocationInfo(this, lit);
                    return lit;
                }
            }
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextInfo the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        Expression e = super.optimize(visitor, contextInfo);
        if (e != this) {
            return e;
        }
        if (Cardinality.subsumes(targetCardinality, getBaseExpression().getCardinality())) {
            final TypeHierarchy th = getConfiguration().getTypeHierarchy();
            int relation = th.relationship(getBaseExpression().getItemType(), targetType);
            if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else if (relation == TypeHierarchy.DISJOINT) {
                // if the item types are disjoint, the result might still be true if both sequences are empty
                if (!Cardinality.allowsZero(targetCardinality) || !Cardinality.allowsZero(getBaseExpression().getCardinality())) {
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            }
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((InstanceOfExpression) other).targetType &&
                targetCardinality == ((InstanceOfExpression) other).targetCardinality;
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode() ^ targetCardinality;
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        InstanceOfExpression exp = new InstanceOfExpression(getBaseExpression().copy(rebindings),
                SequenceType.makeSequenceType(targetType, targetCardinality));
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
    }

    /**
     * Determine the data type of the result of the InstanceOf expression
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Evaluate the expression
     */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Evaluate the expression as a boolean
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        SequenceIterator iter = getBaseExpression().iterate(context);
        return isInstance(iter, context);
    }

    /**
     * Here is the method that does the work
     * @param iter iterator over the operand sequence
     * @param context dynamic evaluation context
     * @return true if the operand is an instance of the required type
     * @throws XPathException if a failure occurs evaluating the operand
     */

    private boolean isInstance(SequenceIterator iter, XPathContext context) throws XPathException {
        int count = 0;
        Item item;
        while ((item = iter.next()) != null) {
            count++;
            if (!targetType.matches(item, context.getConfiguration().getTypeHierarchy())) {
                iter.close();
                return false;
            }
            if (count == 2 && !Cardinality.allowsMany(targetCardinality)) {
                iter.close();
                return false;
            }
        }
        return !(count == 0 && ((targetCardinality & StaticProperty.ALLOWS_ZERO) == 0));
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the InstanceOf expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new InstanceOfCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("instance", this);
        if (targetCardinality == StaticProperty.ALLOWS_ZERO) {
            out.emitAttribute("of", "empty-sequence()");
        } else {
            out.emitAttribute("of", targetType.toString() + Cardinality.getOccurrenceIndicator(targetCardinality));
        }
        if ("JS".equals(out.getOption("target"))) {
            out.emitAttribute("jsTest", targetType.generateJavaScriptItemTypeTest(getBaseExpression().getItemType()));
        }
        getBaseExpression().export(out);
        out.endElement();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */
    @Override
    public String toString() {
        String occ = Cardinality.getOccurrenceIndicator(targetCardinality);
        return "(" + getBaseExpression().toString() + " instance of " +
                targetType.toString() + occ + ")";
    }

    /**
     * Produce a short string identifying the expression for use in error messages
     *
     * @return a short string, sufficient to identify the expression
     */
    @Override
    public String toShortString() {
        String occ = Cardinality.getOccurrenceIndicator(targetCardinality);
        return getBaseExpression().toShortString() + " instance of " +
                targetType.toString() + occ;
    }

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public InstanceOfAdjunct getStreamingAdjunct() {
        return new InstanceOfAdjunct();
    }

//#endif
}

