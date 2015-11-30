////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.CastableToListCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.ListType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

/**
 * Expression class for a cast to a List type
 */
public class CastableToList extends UnaryExpression {

    private ListType targetType;
    private boolean allowEmpty;


    public CastableToList(Expression source, ListType targetType, boolean allowEmpty) {
        super(source);
        this.targetType = targetType;
        this.allowEmpty = allowEmpty;
    }


    public boolean isAllowEmpty() {
        return allowEmpty;
    }

    public ListType getTargetType() {
        return targetType;
    }

    public NamespaceResolver getNamespaceResolver() {
        return getRetainedStaticContext();
    }

    protected OperandRole getOperandRole() {
        return OperandRole.SINGLE_ATOMIC;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        SequenceType atomicType = SequenceType.makeSequenceType(
                BuiltInAtomicType.STRING, allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);

        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.TYPE_OP, "castable as", 0);
        //role.setSourceLocator(this);
        Expression operand = TypeChecker.staticTypeCheck(getBaseExpression(), atomicType, false, role, visitor);
        setBaseExpression(operand);

        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        boolean maybeString = th.relationship(operand.getItemType(), BuiltInAtomicType.STRING) != TypeHierarchy.DISJOINT;
        boolean maybeUntyped = th.relationship(operand.getItemType(), BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT;

        if (!maybeString && !maybeUntyped) {
            // Casting from other types can never succeed
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        if (operand instanceof Literal) {
            GroundedValue literalOperand = ((Literal) operand).getValue();
            if (literalOperand instanceof AtomicValue) {
                return Literal.makeLiteral(BooleanValue.get(effectiveBooleanValue(visitor.getStaticContext().makeEarlyEvaluationContext())));
            }
            if (literalOperand.getLength() == 0) {
                return Literal.makeLiteral(BooleanValue.get(allowEmpty));
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
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextInfo);
        if (e2 != this) {
            return e2;
        }
        // if the operand can't be empty, then set allowEmpty to false to provide more information for analysis
        if (!Cardinality.allowsZero(getBaseExpression().getCardinality())) {
            allowEmpty = false;
            resetLocalStaticProperties();
        }
        return this;
    }

    /**
     * Get the static cardinality of the expression
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the static type of the expression
     * <p/>
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link net.sf.saxon.expr.StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        CastableToList c = new CastableToList(getBaseExpression().copy(), targetType, allowEmpty);
        ExpressionTool.copyLocationInfo(this, c);
        return c;
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
     * Evaluate the expression
     */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) getBaseExpression().evaluateItem(context);
        if (value == null) {
            return allowEmpty;
        }
        try {
            CastToList.cast(value.getStringValue(), targetType, getRetainedStaticContext(), context.getConfiguration().getConversionRules());
            return true;
        } catch (XPathException err) {
            return false;
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the CastableList expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new CastableToListCompiler();
    }
//#endif


    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastableToList) other).targetType &&
                allowEmpty == ((CastableToList) other).allowEmpty;
    }

    /**
     * get HashCode for comparing two expressions.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return getBaseExpression().toString() + " castable as " + targetType.getEQName();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) {
        out.startElement("castableToList", this);
        out.emitAttribute("as", targetType.toString());
        getBaseExpression().export(out);
        out.endElement();
    }

}

// Copyright (c) 2011 Saxonica Limited. All rights reserved.


