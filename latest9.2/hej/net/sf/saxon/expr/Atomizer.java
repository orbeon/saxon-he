package net.sf.saxon.expr;

import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.instruct.DivisibleInstruction;
import net.sf.saxon.instruct.ValueOf;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.util.Stack;

/**
* An Atomizer is an expression corresponding essentially to the fn:data() function: it
* maps a sequence by replacing nodes with their typed values
*/

public final class Atomizer extends UnaryExpression implements DivisibleInstruction {

    private boolean untyped = false;       //set to true if it is known that the nodes being atomized will be untyped
    private boolean singleValued = false; // set to true if all atomized nodes will atomize to a single atomic value
    private ItemType operandItemType = null;

    /**
     * Constructor
     * @param sequence the sequence to be atomized
     *  */

    public Atomizer(Expression sequence) {
        super(sequence);
        sequence.setFlattened(true);
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        computeSingleValued(visitor.getConfiguration().getTypeHierarchy());
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            Value val = ((Literal)operand).getValue();
            if (val instanceof SingletonItem && ((SingletonItem)val).getItem() instanceof FunctionItem) {
                XPathException err = new XPathException("Cannot atomize a function item", "FOTY0013");
                err.setLocator(this);
                throw err;
            }
            if (val instanceof AtomicValue) {
                return operand;
            }
            SequenceIterator iter = val.iterate();
            while (true) {
                // if all items in the sequence are atomic (they generally will be, since this is
                // done at compile time), then return the sequence
                Item i = iter.next();
                if (i == null) {
                    return operand;
                }
                if (i instanceof NodeInfo) {
                    return this;
                }
            }
        } else if (operand instanceof ValueOf && (((ValueOf)operand).getOptions()& ReceiverOptions.DISABLE_ESCAPING) == 0) {
            // XSLT users tend to use ValueOf unnecessarily
            return ((ValueOf)operand).convertToStringJoin(visitor.getStaticContext());
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        untyped = !visitor.getExecutable().isSchemaAware();
        operand = visitor.typeCheck(operand, contextItemType);
        // If the configuration allows typed data, check whether the content type of these particular nodes is untyped
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        computeSingleValued(th);
        visitor.resetStaticProperties();
        ItemType operandType = operand.getItemType(th);
        if (th.isSubType(operandType, BuiltInAtomicType.ANY_ATOMIC)) {
            return operand;
        }
        if (!operandType.isAtomizable()) {
            XPathException err;
            if (operandType instanceof FunctionItemType) {
                err = new XPathException(
                        "Cannot atomize a function item", "FOTY0013");
            } else {
                err = new XPathException(
                        "Cannot atomize an element that is defined in the schema to have element-only content", "FOTY0012");
            }
            err.setLocator(this);
            throw err;
        }
        operand.setFlattened(true);
        return this;
    }

    private void computeSingleValued(TypeHierarchy th) {
        singleValued = untyped;
        if (!singleValued) {
            ItemType nodeType = operand.getItemType(th);
            if (nodeType instanceof NodeTest) {
                SchemaType st = ((NodeTest)nodeType).getContentType();
                if (st == Untyped.getInstance() || st.isAtomicType()) {
                    singleValued = true;
                }
            }
        }
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

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            if (th.isSubType(operand.getItemType(th), BuiltInAtomicType.ANY_ATOMIC)) {
                return operand;
            }
            if (operand instanceof ValueOf && (((ValueOf)operand).getOptions()& ReceiverOptions.DISABLE_ESCAPING) == 0) {
                // XSLT users tend to use ValueOf unnecessarily
                return ((ValueOf)operand).convertToStringJoin(visitor.getStaticContext());
            }
        }
        return exp;
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
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Atomizer copy = new Atomizer(getBaseExpression().copy());
        copy.untyped = untyped;
        copy.singleValued = singleValued;
        return copy;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return getAtomizingIterator(base);
    }

    /**
    * Evaluate as an Item. This should only be called if the Atomizer has cardinality zero-or-one,
    * which will only be the case if the underlying expression has cardinality zero-or-one.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item i = operand.evaluateItem(context);
        if (i==null) {
            return null;
        }
        if (i instanceof NodeInfo) {
            return Value.asItem(((NodeInfo)i).atomize());
        } else {
            return i;
        }
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        super.process(context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER. For this class, the
     * result is always an atomic type, but it might be more specific.
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
        operandItemType = operand.getItemType(th);
        return getAtomizedItemType(operand, untyped, th);
    }

    /**
     * Compute the type that will result from atomizing the result of a given expression
     * @param operand the given expression
     * @param alwaysUntyped true if it is known that nodes will always be untyped
     * @param th the type hierarchy cache
     * @return the item type of the result of evaluating the operand expression, after atomization
     */

    public static ItemType getAtomizedItemType(Expression operand, boolean alwaysUntyped, TypeHierarchy th) {
        ItemType in = operand.getItemType(th);
        if (in.isAtomicType()) {
            return in;
        }
        if (in instanceof NodeTest) {

            if (in instanceof EmptySequenceTest) {
                return in;
            }
            int kinds = ((NodeTest)in).getNodeKindMask();
            if (alwaysUntyped) {
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
        }
	    return BuiltInAtomicType.ANY_ATOMIC;
	}

    /**
     * Node kinds whose typed value is always a string
     */
    private static final int STRING_KINDS =
            (1<<Type.NAMESPACE) | (1<<Type.COMMENT) | (1<<Type.PROCESSING_INSTRUCTION);

    /**
     * Node kinds whose typed value is always untypedAtomic
     */

    private static final int UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.DOCUMENT);

    /**
     * Node kinds whose typed value is untypedAtomic if the configuration is untyped
     */

    private static final int UNTYPED_IF_UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.ELEMENT) | (1<<Type.DOCUMENT) | (1<<Type.ATTRIBUTE);

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        if (untyped || singleValued) {
            return operand.getCardinality();
        } else {
            if (Cardinality.allowsMany(operand.getCardinality())) {
                return StaticProperty.ALLOWS_ZERO_OR_MORE;
            }
            ItemType in = operandItemType;
            if (in == null) {
                try {
                    in = operand.getItemType(getContainer().getExecutable().getConfiguration().getTypeHierarchy());
                } catch (NullPointerException npe) {
                    return StaticProperty.ALLOWS_ZERO_OR_MORE;
                }
            }
            if (in.isAtomicType()) {
                return operand.getCardinality();
            }
            if (in instanceof NodeTest) {
                SchemaType schemaType = ((NodeTest)in).getContentType();
                if (schemaType.isAtomicType()) {
                    // can return at most one atomic value per node
                    return operand.getCardinality();
                }
            }
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
	}


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     *         expression, and that represent possible results of this expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet result = operand.addToPathMap(pathMap, pathMapNodeSet);
        if (result != null) {
            result.setAtomized();
        }
        return null;
    }

    /**
     * Get an iterator that returns the result of atomizing the sequence delivered by the supplied
     * iterator
     * @param base the supplied iterator, the input to atomization
     * @return an iterator that returns atomic values, the result of the atomization
     */

    public static SequenceIterator getAtomizingIterator(SequenceIterator base) throws XPathException {
        if (base instanceof AxisIterator) {
            return new AxisAtomizingIterator((AxisIterator)base);
        }
        if ((base.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            int count = ((LastPositionFinder)base).getLastPosition();
            if (count == 0) {
                return EmptyIterator.getInstance();
            } else if (count == 1) {
                Item first = base.next();
                return first.getTypedValue();
            }
        }
        return new MappingIterator(base, AtomizingFunction.getInstance());
    }

    /**
     * In streaming mode, process the first half of the instruction (for example, to start a new document or element)
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processLeft(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        SequenceReceiver out = context.getReceiver();
        state.push(out);
        SequenceOutputter out2 = new SequenceOutputter();
        out2.setPipelineConfiguration(out.getPipelineConfiguration());
        context.setReceiver(out2);
        int annotation = ((NodeInfo)context.getContextItem()).getTypeAnnotation();
        state.push(annotation);
    }

    /**
     * In streaming mode, process the right half of the instruction (for example, to end a new document or element)
     * Note that unlike other divisible instructions this one doesn't push the result to the current output
     * destination, it leaves a sequenceIterator over the result on the stack.
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processRight(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        int annotation = (Integer)state.pop();
        CharSequence value = ((SequenceOutputter)context.getReceiver()).getFirstItem().getStringValueCS();
        SchemaType type = context.getConfiguration().getSchemaType(annotation);

        SequenceReceiver out = (SequenceReceiver)state.pop();
        context.setReceiver(out);
        
        if (type.isComplexType()) {
            if (((ComplexType)type).isMixedContent()) {
                SequenceIterator iter = SingletonIterator.makeIterator(new UntypedAtomicValue(value));
                state.push(iter);
            } else {
                throw new XPathException("Cannot atomize element with element-only content");
            }
        } else {
            // TODO: handle namespace-sensitive types
            if (((SimpleType)type).isNamespaceSensitive()) {
                throw new XPathException("Cannot yet do streaming of content with namespace-sensitive element types");
            }
            SequenceIterator iter = ((SimpleType)type).getTypedValue(value, null, context.getConfiguration().getNameChecker());
            state.push(iter);
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public String displayExpressionName() {
        return "atomize";
    }

    /**
    * Implement the mapping function. This is stateless, so there is a singleton instance.
    */

    public static class AtomizingFunction implements MappingFunction {

        /**
         * Private constructor, ensuring that everyone uses the singleton instance
         */

        private AtomizingFunction(){}

        private static final AtomizingFunction theInstance = new AtomizingFunction();

        /**
         * Get the singleton instance
         * @return the singleton instance of this mapping function
         */

        public static AtomizingFunction getInstance() {
            return theInstance;
        }

        public SequenceIterator map(Item item) throws XPathException {
            if (item instanceof NodeInfo) {
                return item.getTypedValue();
            } else {
                return SingletonIterator.makeIterator(item);
            }
        }
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
