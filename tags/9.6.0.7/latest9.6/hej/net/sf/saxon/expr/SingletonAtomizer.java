////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SingletonAtomizerCompiler;
import com.saxonica.ee.stream.adjunct.SingletonAtomizerAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;

/**
 * A SingletonAtomizer combines the functions of an Atomizer and a CardinalityChecker: it is used to
 * atomize a sequence of nodes, checking that the result of the atomization contains zero or one atomic
 * values. Note that the input may be a sequence of nodes or atomic values, even though the result must
 * contain at most one atomic value.
 */

public final class SingletonAtomizer extends UnaryExpression {

    private boolean allowEmpty;
    private RoleLocator role;

    /**
     * Constructor
     *
     * @param sequence   the sequence to be atomized
     * @param role       contains information about where the expression appears, for use in any error message
     * @param allowEmpty true if the result sequence is allowed to be empty.
     */

    public SingletonAtomizer(Expression sequence, RoleLocator role, boolean allowEmpty) {
        super(sequence);
        this.allowEmpty = allowEmpty;
        this.role = role;
    }

    protected OperandRole getOperandRole() {
        return OperandRole.SINGLE_ATOMIC;
    }

    /**
     * Ask if the expression is allowed to return an empty sequence
     * @return true if the expected cardinality is zero-or-one, false if it is exactly-one
     */

    public boolean isAllowEmpty() {
        return allowEmpty;
    }

    /**
     * Simplify an expression
     *
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal && ((Literal) operand).getValue() instanceof AtomicValue) {
            return operand;
        }
        return this;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        operand = visitor.typeCheck(operand, contextInfo);
        visitor.resetStaticProperties();
        if (Literal.isEmptySequence(operand)) {
            if (!allowEmpty) {
                typeError("An empty sequence is not allowed as the " + role.getMessage(), role.getErrorCode(), null);
            }
            return operand;
        }
        ItemType operandType = operand.getItemType();
        if (operandType.isPlainType()) {
            return operand;
        }
        if (!operandType.isAtomizable()) {
            XPathException err;
            if (operandType instanceof FunctionItemType) {
                err = new XPathException("Cannot atomize a function item", "FOTY0013");
            } else {
                err = new XPathException(
                        "Cannot atomize an element that is defined in the schema to have element-only content", "FOTY0012");
            }
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return this;
    }


    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            if (operand.getItemType().isPlainType()) {
                return operand;
            }
            return this;
        } else {
            return exp;
        }
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
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
        Expression e2 = new SingletonAtomizer(getBaseExpression().copy(), role, allowEmpty);
        ExpressionTool.copyLocationInfo(this, e2);
        return e2;
    }

    //#ifdefined STREAM
    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new SingletonAtomizerAdjunct();
    }

    //#endif

    /**
     * Get the RoleLocator (used to construct error messages)
     *
     * @return the role locator
     */

    public RoleLocator getRole() {
        return role;
    }


    /*@Nullable*/
    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet result = operand.addToPathMap(pathMap, pathMapNodeSet);
        if (result != null) {
            TypeHierarchy th = getConfiguration().getTypeHierarchy();
            ItemType operandItemType = operand.getItemType();
            if (th.relationship(NodeKindTest.ELEMENT, operandItemType) != TypeHierarchy.DISJOINT ||
                    th.relationship(NodeKindTest.DOCUMENT, operandItemType) != TypeHierarchy.DISJOINT) {
                result.setAtomized();
            }
        }
        return null;
    }

    /**
     * Evaluate as an Item. This should only be called if a singleton or empty sequence is required;
     * it throws a type error if the underlying sequence is multi-valued.
     */

    public AtomicValue evaluateItem(XPathContext context) throws XPathException {
        int found = 0;
        AtomicValue result = null;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof AtomicValue) {
                if (found++ > 0) {
                    typeError(
                            "A sequence of more than one item is not allowed as the " +
                                    role.getMessage(), role.getErrorCode(), context);
                }
                result = (AtomicValue) item;
            } else if (item instanceof FunctionItem) {
                typeError("A function item cannot appear as the " +
                        role.getMessage(), "FOTY0013", context);
            } else if (item instanceof ObjectValue) {
                return new StringValue(item.getStringValue());
            } else {
                AtomicSequence value = ((NodeInfo) item).atomize();
                found += SequenceTool.getLength(value);
                if (found > 1) {
                    typeError(
                            "A sequence of more than one item is not allowed as the " +
                                    role.getMessage(), role.getErrorCode(), context);
                }
                result = value.head();
            }
        }
        if (found == 0 && !allowEmpty) {
            typeError("An empty sequence is not allowed as the " +
                    role.getMessage(), role.getErrorCode(), null);
        }
        return result;
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER. For this class, the
     *         result is always an atomic type, but it might be more specific.
     */

    /*@NotNull*/
    public ItemType getItemType() {
        boolean isSchemaAware = true;
        try {
            isSchemaAware = getContainer().getPackageData().isSchemaAware();
        } catch (NullPointerException err) {
            // ultra-cautious code in case expression container has not been set
            if (!getConfiguration().isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION)) {
                isSchemaAware = false;
            }
        }
        ItemType in = operand.getItemType();
        if (in.isPlainType()) {
            return in;
        } else if (in instanceof NodeTest) {
            int kinds = ((NodeTest) in).getNodeKindMask();
            if (!isSchemaAware) {
                // Some node-kinds always have a typed value that's a string

                if ((kinds | STRING_KINDS) == STRING_KINDS) {
                    return BuiltInAtomicType.STRING;
                }
                // Some node-kinds are always untyped atomic; some are untypedAtomic provided that the configuration
                // is untyped

                if ((kinds | UNTYPED_IF_UNTYPED_KINDS) == UNTYPED_IF_UNTYPED_KINDS) {
                    return BuiltInAtomicType.UNTYPED_ATOMIC;
                }
            } else {
                if ((kinds | UNTYPED_KINDS) == UNTYPED_KINDS) {
                    return BuiltInAtomicType.UNTYPED_ATOMIC;
                }
            }

            return in.getAtomizedItemType();
        } else if (in instanceof JavaExternalObjectType) {
            return in.getAtomizedItemType();
        }
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Node kinds whose typed value is always a string
     */
    private static final int STRING_KINDS =
            (1 << Type.NAMESPACE) | (1 << Type.COMMENT) | (1 << Type.PROCESSING_INSTRUCTION);

    /**
     * Node kinds whose typed value is always untypedAtomic
     */

    private static final int UNTYPED_KINDS =
            (1 << Type.TEXT) | (1 << Type.DOCUMENT);

    /**
     * Node kinds whose typed value is untypedAtomic if the configuration is untyped
     */

    private static final int UNTYPED_IF_UNTYPED_KINDS =
            (1 << Type.TEXT) | (1 << Type.ELEMENT) | (1 << Type.DOCUMENT) | (1 << Type.ATTRIBUTE);

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        if (allowEmpty) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Give a string representation of the expression name for use in diagnostics
     *
     * @return the expression name, as a string
     */

    public String getExpressionName() {
        return "atomizeSingleton";
    }

    @Override
    public String toShortString() {
        return operand.toShortString();
    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Cast expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SingletonAtomizerCompiler();
    }
//#endif

}

