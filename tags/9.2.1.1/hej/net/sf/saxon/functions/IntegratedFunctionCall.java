package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
 * Expression representing a call to a user-written extension
 * function implemented as a subtype of {@link ExtensionFunctionCall}
 */
public class IntegratedFunctionCall extends FunctionCall {

    private ExtensionFunctionCall function;
    private SequenceType resultType = SequenceType.ANY_SEQUENCE;
    private int state = 0;

    public IntegratedFunctionCall(ExtensionFunctionCall function) {
        this.function = function;
    }

    /**
     * Get the underlying IntegratedFunction
     */

    public ExtensionFunctionCall getFunction() {
        return function;
    }

    /**
    * Method called during static type checking
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        ExtensionFunctionDefinition definition = function.getDefinition();
        checkArgumentCount(definition.getMinimumNumberOfArguments(), definition.getMaximumNumberOfArguments(), visitor);
        final int args = getNumberOfArguments();
        SequenceType[] declaredArgumentTypes = definition.getArgumentTypes();
        if (declaredArgumentTypes == null || (args != 0 && declaredArgumentTypes.length == 0)) {
            throw new XPathException("Integrated function " + getDisplayName() +
                    " failed to declare its argument types");
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        SequenceType[] actualArgumentTypes = new SequenceType[args];
        for (int i=0; i<args; i++) {
            argument[i] = TypeChecker.staticTypeCheck(
                                argument[i],
                                i < declaredArgumentTypes.length ?
                                        declaredArgumentTypes[i] :
                                        declaredArgumentTypes[declaredArgumentTypes.length - 1],
                                false,
                                new RoleLocator(RoleLocator.FUNCTION, getFunctionName(), i),
                                visitor);

            actualArgumentTypes[i] = SequenceType.makeSequenceType(
                    argument[i].getItemType(th),
                    argument[i].getCardinality());
        }
        resultType = definition.getResultType(actualArgumentTypes);
        if (state == 0) {
            function.supplyStaticContext(visitor.getStaticContext(), 0, getArguments());
        }
        state++;
    }

    /**
     * Type-check the expression. 
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.typeCheck(visitor, contextItemType);
        if (exp instanceof IntegratedFunctionCall) {
            Expression exp2 = ((IntegratedFunctionCall)exp).function.rewrite(visitor.getStaticContext(), argument);
            if (exp2 == null) {
                return exp;
            } else {
                ExpressionTool.copyLocationInfo(this, exp2);
                return exp2.simplify(visitor).typeCheck(visitor, contextItemType).optimize(visitor, contextItemType);
            }
        }
        return exp;
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     * @return the result of the early evaluation, or the original expression, or potentially
     *         a simplified expression
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Determine the data type of the expression, if possible. All expression return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     * @param th the type hierarchy cache
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     *         Type.NODE, or Type.ITEM (meaning not known at compile time)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return resultType.getPrimaryType();
    }

    /**
     * Compute the static cardinality of this expression
     * @return the computed cardinality, as one of the values {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     *         {@link net.sf.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     *         {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}
     */

    protected int computeCardinality() {
        return resultType.getCardinality();
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        ExtensionFunctionDefinition definition = function.getDefinition();
        return (definition.dependsOnFocus() ? StaticProperty.DEPENDS_ON_FOCUS : 0);
    }

    /**
     * Compute the special properties of this expression. These properties are denoted by a bit-significant
     * integer, possible values are in class {@link net.sf.saxon.expr.StaticProperty}. The "special" properties are properties
     * other than cardinality and dependencies, and most of them relate to properties of node sequences, for
     * example whether the nodes are in document order.
     * @return the special properties, as a bit-significant integer
     */

    protected int computeSpecialProperties() {
        ExtensionFunctionDefinition definition = function.getDefinition();
        return (definition.hasSideEffects() ? StaticProperty.HAS_SIDE_EFFECTS : StaticProperty.NON_CREATIVE);
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        ExtensionFunctionCall newCall = function.getDefinition().makeCallExpression();
        newCall.setDefinition(function.getDefinition());
        function.copyLocalData(newCall);
        IntegratedFunctionCall copy = new IntegratedFunctionCall(newCall);
        Expression[] args = new Expression[getNumberOfArguments()];
        for (int i=0; i<args.length; i++) {
            args[i] = argument[i].copy();
        }
        copy.setFunctionName(getFunctionName());
        copy.setArguments(args);
        copy.resultType = resultType;
        copy.state = state;
        return copy;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ExtensionFunctionDefinition definition = function.getDefinition();
        SequenceIterator[] argValues = new SequenceIterator[getNumberOfArguments()];
        for (int i=0; i<argValues.length; i++) {
            argValues[i] = argument[i].iterate(context);
        }
        final RoleLocator role = new RoleLocator(RoleLocator.FUNCTION, "result", 0);
        final Configuration config = context.getConfiguration();
        SequenceIterator result;
        try {
            result = function.call(argValues, context);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            throw e;
        }
        if (!definition.trustResultType()) {
            int card = resultType.getCardinality();
            if (card != StaticProperty.ALLOWS_ZERO_OR_MORE) {
                result = new CardinalityCheckingIterator(result,
                        card,
                        role,
                        this);
            }
            final ItemType type = resultType.getPrimaryType();
            if (type != AnyItemType.getInstance()) {
                result = new ItemMappingIterator(result,
                        new ItemMappingFunction() {
                            public Item map(Item item) throws XPathException {
                                if (!type.matchesItem(item, false, config)) {
                                    String msg = "Item returned by integrated function " +
                                        getFunctionName().getDisplayName() +
                                            "() is not of declared item type. Actual type is " +
                                            Value.asValue(item).getItemType(config.getTypeHierarchy()).toString(config.getNamePool()) +
                                            "; expected type is " + type.toString(config.getNamePool());
                                    XPathException err = new XPathException(
                                            msg);
                                    err.setErrorCode("XPTY0004");
                                    err.setLocator(IntegratedFunctionCall.this);
                                    throw err;
                                }
                                return item;
                            }
                        }, true);
            }
        }
        return result;
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
                SequenceIterator[] argValues = new SequenceIterator[getNumberOfArguments()];
        for (int i=0; i<argValues.length; i++) {
            argValues[i] = argument[i].iterate(context);
        }
        try {
            return function.effectiveBooleanValue(argValues, context);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            throw e;
        }
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



