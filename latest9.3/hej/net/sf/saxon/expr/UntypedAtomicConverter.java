package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.ForEach;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
 * An UntypedAtomicConverter is an expression that converts any untypedAtomic items in
 * a sequence to a specified type
 */

public final class UntypedAtomicConverter extends UnaryExpression {

    private AtomicType requiredItemType;
    private boolean allConverted;
    private boolean singleton = false;
    private RoleLocator role;

    /**
     * Constructor
     *
     * @param sequence         this must be a sequence of atomic values. This is not checked; a ClassCastException
     *                         will occur if the precondition is not satisfied.
     * @param requiredItemType the item type to which untypedAtomic items in the sequence should be converted,
     *                         using the rules for "cast as".
     * @param allConverted     true if the result of this expression is a sequence in which all items
     *                         belong to the required type
     * @param role             Diagnostic information for use if conversion fails
     */

    public UntypedAtomicConverter(Expression sequence, AtomicType requiredItemType, boolean allConverted, RoleLocator role) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        this.allConverted = allConverted;
        this.role = role;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
     * Get the item type to which untyped atomic items must be converted
     * @return the required item type
     */

    public ItemType getRequiredItemType() {
        return requiredItemType;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     *         {@link #PROCESS_METHOD}
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }

    /**
     * Determine whether all items are to be converted, or only the subset that are untypedAtomic
     * @return true if all items are to be converted
     */

    public boolean areAllItemsConverted() {
        return allConverted;
    }

    /**
     * Determine the data type of the items returned by the expression
     *
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType it = operand.getItemType(th);
        singleton = it.isAtomicType() && !Cardinality.allowsMany(operand.getCardinality());
        if (allConverted) {
            return requiredItemType;
        } else {
            return Type.getCommonSuperType(requiredItemType, operand.getItemType(th), th);
        }
    }

    public int computeCardinality() {
        if (singleton) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return super.computeCardinality();
        }
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (allConverted && requiredItemType.isNamespaceSensitive()) {
            XPathException err = new XPathException("Cannot convert untypedAtomic values to QNames or NOTATIONs");
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand instanceof Literal) {
            return Literal.makeLiteral(
                    ((Value)SequenceExtent.makeSequenceExtent(
                            iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))).reduce());
        }
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType type = operand.getItemType(th);
        if (type instanceof NodeTest) {
            return this;
        }
        singleton = type.isAtomicType() && !Cardinality.allowsMany(operand.getCardinality());
                
        // If we're atomizing a node that always returns an untyped atomic value, and then converting
        // the untyped atomic value to a string, then we might as well take the string value of the node
        if (operand instanceof Atomizer &&
                type.equals(BuiltInAtomicType.UNTYPED_ATOMIC) &&
                requiredItemType == BuiltInAtomicType.STRING &&
                ((Atomizer)operand).getBaseExpression().getItemType(th) instanceof NodeTest) {
            Expression nodeExp = ((Atomizer)operand).getBaseExpression();
            if (nodeExp.getCardinality() != StaticProperty.EXACTLY_ONE) {
                // TODO: Saxon 9.2 as issued was converting to a call to string() when the
                // expected type is "xs:string?", which is fine for functions that treat an empty
                // sequence like a zero-length string, but fails for example on resolve-QName()
                // which treats them differently. It would be good to revert to the string() call
                // in all cases: or perhaps to a variant of string() that maps () to (). This would
                // enable further optimizations.
                SystemFunction fn = (SystemFunction)SystemFunction.makeSystemFunction(
                        "string", new Expression[]{new ContextItemExpression()});
                fn.setContainer(getContainer());
                ForEach map = new ForEach(nodeExp, fn);
                map.setContainer(getContainer());
                return map;
            } else {
                SystemFunction fn = (SystemFunction)SystemFunction.makeSystemFunction(
                        "string", new Expression[]{nodeExp});
                fn.setContainer(getContainer());
                return fn;
            }
        }
        if (type.equals(BuiltInAtomicType.ANY_ATOMIC) || type instanceof AnyItemType ||
                type.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            return this;
        }
        // the sequence can't contain any untyped atomic values, so there's no need for a converter
        return operand;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
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

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // If the underlying expression is casting to xs:untypedAtomic, there's scope for a short-circuit
        // (This happens when xsl:value-of is used unnecessarily)
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression)operand).getTargetType();
            if (th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression)operand).getBaseExpression();
                ItemType et = e.getItemType(th);
                if (et instanceof AtomicType && th.isSubType(et, requiredItemType)) {
                    return e;
                }
            }
        }
        return this;
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE | StaticProperty.NOT_UNTYPED;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new UntypedAtomicConverter(getBaseExpression().copy(), requiredItemType, allConverted, role);
    }

    /**
     * Iterate over the sequence of values
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new ItemMappingIterator(base, getMappingFunction(context), true);
    }

    /**
     * Get the mapping function that converts untyped atomic values to the required type
     * @param context  the dynamic evaluation context for the conversion
     * @return the mapping function
     */

    public ItemMappingFunction getMappingFunction(final XPathContext context) {
        final ConversionRules rules = context.getConfiguration().getConversionRules();
        return new ItemMappingFunction() {
            public Item mapItem(Item item) throws XPathException {
                if (item instanceof UntypedAtomicValue) {
                    ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true, rules);
                    if (val instanceof ValidationFailure) {
                        String msg = role.composeRequiredMessage(requiredItemType, context.getNamePool());
                        msg += ". " + ((ValidationFailure)val).getMessage();
                        XPathException err = new XPathException(msg);
                        err.setErrorCode(role.getErrorCode());
                        err.setLocator(UntypedAtomicConverter.this);
                        throw err;
                    }
                    return (AtomicValue)val;
                } else {
                    return item;
                }
            }
        };
    }

    /**
     * Evaluate as an Item. This should only be called if the UntypedAtomicConverter has cardinality zero-or-one
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final ConversionRules rules = context.getConfiguration().getConversionRules();
        Item item = operand.evaluateItem(context);
        if (item == null) {
            return null;
        }
        if (item instanceof UntypedAtomicValue) {
            ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true, rules);
            if (val instanceof ValidationFailure) {
                String msg = role.composeRequiredMessage(requiredItemType, context.getNamePool());
                msg += ". " + ((ValidationFailure)val).getMessage();
                XPathException err = new XPathException(msg);
                err.setErrorCode(role.getErrorCode());
                err.setLocator(UntypedAtomicConverter.this);
                throw err;
            } else {
                return (AtomicValue)val;
            }
        } else {
            return item;
        }
    }

     /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("convertUntypedAtomic");
        out.emitAttribute("to", requiredItemType.toString(out.getConfiguration().getNamePool()));
        out.emitAttribute("all", allConverted ? "true" : "false");
        operand.explain(out);
        out.endElement();
    }
    protected String displayOperator(Configuration config) {
        return "convert untyped atomic items to " + requiredItemType.toString(config.getNamePool());
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "convertUntypedAtomic";
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
