////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.CastableToUnionCompiler;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UnionType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Cardinality;

/**
 * Expression class for testing castability to a union type
 */
public class CastableToUnion extends UnaryExpression {

    private UnionType targetType;
    private boolean allowEmpty;
    private NamespaceResolver nsResolver = null;

    /**
     * Create the expression
     * @param source the operand value to be tested for castability
     * @param targetType the union type that is the target of the cast
     * @param allowEmpty true if the operand value is allowed to be an empty sequence
     */

    public CastableToUnion(Expression source, UnionType targetType, boolean allowEmpty) {
        super(source);
        this.targetType = targetType;
        this.allowEmpty = allowEmpty;
    }

    /**
     * Ask whether the operand is allowed to be an empty sequence
     * @return true if the operand is allowed to be an empty sequence
     */

    public boolean isAllowEmpty() {
        return allowEmpty;
    }

    /**
     * Get the target type (a union type) of the castability test
     * @return the target type
     */

    public UnionType getTargetType() {
        return targetType;
    }

    /**
     * Get the namespace resolver to be used for resolving namespace-sensitive content (QNames)
     * @return the namespace resolver, or null if there is none
     */

    public NamespaceResolver getNamespaceResolver() {
        return nsResolver;
    }

    /**
     * Get the operand role for streamability analysis
     * @return absorption
     */

    protected OperandRole getOperandRole() {
        return OperandRole.SINGLE_ATOMIC;
    }

    /**
     * Simplify the expression
     *
     * @param visitor an expression visitor
     * @return the simplified expression
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (targetType.isNamespaceSensitive()) {
            StaticContext env = visitor.getStaticContext();
            nsResolver = env.getNamespaceResolver();
        }
        operand = visitor.simplify(operand);
        return this;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        operand = visitor.typeCheck(operand, contextInfo);

        if (operand instanceof Literal) {
            GroundedValue literalOperand = ((Literal) operand).getValue();
            if (literalOperand instanceof AtomicValue) {
                AtomicValue av = evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                return Literal.makeLiteral(av, getContainer());
            }
            if (literalOperand.getLength() == 0) {
                return Literal.makeLiteral(BooleanValue.get(allowEmpty), getContainer());
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
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // if the operand can't be empty, then set allowEmpty to false to provide more information for analysis
        if (!Cardinality.allowsZero(operand.getCardinality())) {
            allowEmpty = false;
            resetLocalStaticProperties();
        }
        return this;
    }

    /**
     * Get the static cardinality of the expression
     */

    public int computeCardinality() {
        return (allowEmpty && Cardinality.allowsZero(operand.getCardinality())
                ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);
    }

    /**
     * Get the static type of the expression
     */

    /*@NotNull*/
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
        CastableToUnion c = new CastableToUnion(getBaseExpression().copy(), targetType, allowEmpty);
        c.nsResolver = nsResolver;
        return c;
    }

    /**
     * Evaluate the expression
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws XPathException if an error occurs evaluating the operand
     */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
     * Get the effective boolean value of the expression
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws XPathException if an error occurs evaluating the operand
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // This method does its own atomization so that it can distinguish between atomization
        // failures and casting failures
        int count = 0;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                Sequence atomizedValue = ((NodeInfo) item).atomize();
                int length = SequenceTool.getLength(atomizedValue);
                count += length;
                if (count > 1) {
                    return false;
                }
                if (length != 0) {
                    AtomicValue av = (AtomicValue) atomizedValue.head();
                    if (!CastToUnion.castable(av, targetType, nsResolver, context)) {
                        return false;
                    }
                }
            } else if (item instanceof AtomicValue) {
                AtomicValue av = (AtomicValue) item;
                count++;
                if (count > 1) {
                    return false;
                }
                if (!CastToUnion.castable(av, targetType, nsResolver, context)) {
                    return false;
                }
            } else {
                throw new XPathException("Input to 'castable' operator cannot be atomized", "XPTY0004");
            }
        }
        return count != 0 || allowEmpty;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                other instanceof CastableToUnion &&
                targetType == ((CastableToUnion) other).targetType &&
                allowEmpty == ((CastableToUnion) other).allowEmpty &&
                nsResolver == ((CastableToUnion) other).nsResolver;
    }

    /**
     * get HashCode for comparing two expressions.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ targetType.hashCode();
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the CastableToUnion expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public CastableToUnionCompiler getExpressionCompiler() {
        return new CastableToUnionCompiler();
    }
//#endif

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return operand.toString() + " castable as " + targetType.getEQName();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("castableToUnion");
        out.emitAttribute("as", targetType.getEQName());
        operand.explain(out);
        out.endElement();
    }

}

// Copyright (c) 2011-2014 Saxonica Limited. All rights reserved.


