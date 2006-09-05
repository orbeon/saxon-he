package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.event.TypeCheckingFilter;
import net.sf.saxon.pattern.DocumentNodeTest;
import net.sf.saxon.Configuration;

/**
* A ItemChecker implements the item type checking of "treat as": that is,
* it returns the supplied sequence, checking that all its items are of the correct type
*/

public final class ItemChecker extends UnaryExpression {

    private ItemType requiredItemType;
    private RoleLocator role;

    /**
    * Constructor
    */

    public ItemChecker(Expression sequence, ItemType itemType, RoleLocator role) {
        super(sequence);
        this.requiredItemType = itemType;
        this.role = role;
        adoptChildExpression(sequence);
    }

    /**
     * Get the required type
     */

    public ItemType getRequiredType() {
        return requiredItemType;
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (requiredItemType instanceof AnyItemType) {
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        // When typeCheck is called a second time, we might have more information...

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        int card = operand.getCardinality();
        if (card == StaticProperty.EMPTY) {
            //value is always empty, so no item checking needed
            return operand;
        }
        ItemType supplied = operand.getItemType(th);
        int relation = th.relationship(requiredItemType, supplied);
        if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMES) {
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        } else if (relation == TypeHierarchy.DISJOINT) {
            if (Cardinality.allowsZero(card)) {
                String message = role.composeErrorMessage(requiredItemType, operand.getItemType(th), env.getNamePool());
                env.issueWarning("Warning: the only value that can pass type-checking is an empty sequence. " +
                        message, this);
            } else if (requiredItemType == Type.STRING_TYPE && th.isSubType(supplied, Type.ANY_URI_TYPE)) {
                // URI promotion will take care of this at run-time
                ComputedExpression.setParentExpression(operand, getParentExpression());
                return operand;
            } else {
                String message = role.composeErrorMessage(requiredItemType, operand.getItemType(th), env.getNamePool());
                StaticError err = new StaticError(message);
                err.setErrorCode(role.getErrorCode());
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        int m = ITERATE_METHOD | PROCESS_METHOD;
        if (!Cardinality.allowsMany(getCardinality())) {
            m |= EVALUATE_METHOD;
        }
        return m;
    }


    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemCheckMappingFunction map = new ItemCheckMappingFunction();
        map.externalContext = context;
        return new ItemMappingIterator(base, map);
    }

    /**
    * Mapping function: this is used only if the expression does not allow a sequence of more than
    * one item.
    */
    private class ItemCheckMappingFunction implements ItemMappingFunction {
        public XPathContext externalContext;
        public Item map(Item item) throws XPathException {
            testConformance(item, externalContext);
            return item;
        }
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        testConformance(item, context);
        return item;
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        Expression next = operand;
        int card = StaticProperty.ALLOWS_ZERO_OR_MORE;
        if (next instanceof CardinalityChecker) {
            card = ((CardinalityChecker)next).getRequiredCardinality();
            next = ((CardinalityChecker)next).getBaseExpression();
        }
        if ((next.getImplementationMethod() & PROCESS_METHOD) != 0 && !(requiredItemType instanceof DocumentNodeTest)) {
            SequenceReceiver out = context.getReceiver();
            TypeCheckingFilter filter = new TypeCheckingFilter();
            filter.setUnderlyingReceiver(out);
            filter.setPipelineConfiguration(out.getPipelineConfiguration());
            filter.setRequiredType(requiredItemType, card, role);
            context.setReceiver(filter);
            next.process(context);
            filter.close();
            context.setReceiver(filter);
        } else {
            super.process(context);
        }
    }



    private void testConformance(Item item, XPathContext context) throws XPathException {
        if (!requiredItemType.matchesItem(item, true, (context == null ? null : context.getConfiguration()))) {
            String message;
            if (context == null) {
                // no name pool available
                message = "Supplied value of type " + Type.displayTypeName(item) +
                        " does not match the required type of " + role.getMessage();
            } else {
                final NamePool pool = context.getNamePool();
                final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
                message = role.composeErrorMessage(requiredItemType, Value.asValue(item).getItemType(th), pool);
            }
            String errorCode = role.getErrorCode();
            if ("XPDY0050".equals(errorCode)) {
                // error in "treat as" assertion
                dynamicError(message, errorCode, context);
            } else {
                typeError(message, errorCode, context);
            }
        }
    }

    /**
    * Determine the data type of the items returned by the expression
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
        // TODO: take the intersection of the required type with the static type of the operand
	    return requiredItemType;
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType == ((ItemChecker)other).requiredItemType;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "treat as " + requiredItemType.toString(config.getNamePool());
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
