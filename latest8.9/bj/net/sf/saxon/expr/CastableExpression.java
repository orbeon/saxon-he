package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.ValidationErrorValue;
import net.sf.saxon.value.Value;

/**
* Castable Expression: implements "Expr castable as atomic-type?".
* The implementation simply wraps a cast expression with a try/catch.
*/

public final class CastableExpression extends UnaryExpression {

    AtomicType targetType;
    boolean allowEmpty;

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        this.targetType = target;
        this.allowEmpty = allowEmpty;
    }

    /**
     * Get the target type
     */

    public AtomicType getTargetType() {
        return targetType;
    }

    /**
     * Determine whether the empty sequence is allowed
     */

    public boolean allowsEmpty() {
        return allowEmpty;
    }

    /**
    * Simplify the expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (Literal.isAtomic(operand)) {
            return Literal.makeLiteral(
                    BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext())));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);

        // We need to take care here. The usual strategy of wrapping the operand in an expression that
        // does type-checking doesn't work here, because an error in the type checking should be caught,
        // while an error in evaluating the expression as written should not.

//        SequenceType atomicType = SequenceType.makeSequenceType(
//                                 BuiltInAtomicType.ANY_ATOMIC,
//                                 (allowEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE
//                                             : StaticProperty.EXACTLY_ONE));
//
//        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "castable as", 0, null);
//        role.setSourceLocator(this);
//        try {
//            operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);
//        } catch (XPathException err) {
//            return Literal.makeLiteral(BooleanValue.FALSE);
//        }

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (!CastExpression.isPossibleCast(
                operand.getItemType(th).getAtomizedItemType().getFingerprint(),
                targetType.getPrimitiveType())) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }

        if (Literal.isAtomic(operand)) {
            return Literal.makeLiteral(BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext())));
        }
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.optimize(opt, env, contextItemType);
        if (Literal.isAtomic(operand)) {
            return Literal.makeLiteral(
                    BooleanValue.get(effectiveBooleanValue(env.makeEarlyEvaluationContext())));
        }
        return this;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastableExpression)other).targetType &&
                allowEmpty == ((CastableExpression)other).allowEmpty;
    }

    /**
    * Determine the data type of the result of the Castable expression
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        int count = 0;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                Value atomizedValue = ((NodeInfo)item).atomize();
                int length = atomizedValue.getLength();
                count += length;
                if (count > 1) {
                    return false;
                }
                if (length != 0) {
                    AtomicValue av = (AtomicValue)atomizedValue.itemAt(0);
                    if (!isCastable(av, targetType, context)) {
                        return false;
                    }
                }
            } else {
                AtomicValue av = (AtomicValue)item;
                count++;
                if (count > 1) {
                    return false;
                }
                if (!isCastable(av, targetType, context)) {
                    return false;
                }
            }
        }
        if (count == 0) {
            return allowEmpty;
        }
        return true;
    }

    public static boolean isCastable(AtomicValue value, AtomicType targetType, XPathContext context) {
        if (targetType instanceof BuiltInAtomicType) {
            return !(value.convert(targetType, context, true) instanceof ValidationErrorValue);
        } else {
            AtomicValue prim =
                value.convert((AtomicType)targetType.getBuiltInBaseType(), context, true);
            if (prim instanceof ValidationErrorValue) {
                return false;
            }
            AtomicValue val =
                targetType.setDerivedTypeLabel(prim.copy(null), prim.getStringValueCS(), true);
            return !(val instanceof ValidationErrorValue);
        }
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "castable as " + targetType.toString(config.getNamePool());
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
