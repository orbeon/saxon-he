package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SimpleNodeConstructor;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.pattern.NodeKindTest;

/**
* Implement XPath function string()
*/

public class StringFn extends SystemFunction {

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     *         {@link #PROCESS_METHOD}
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | WATCH_METHOD;
    }

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
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
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (th.isSubType(argument[0].getItemType(th), BuiltInAtomicType.STRING) &&
                argument[0].getCardinality() == StaticProperty.EXACTLY_ONE) {
            return argument[0];
        }
        if (argument[0] instanceof SimpleNodeConstructor) {
            return ((SimpleNodeConstructor)argument[0]).getContentExpression();
        }
        return this;
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
        PathMap.PathMapNodeSet result = argument[0].addToPathMap(pathMap, pathMapNodeSet);
        if (result != null && (result != pathMapNodeSet || argument[0] instanceof ContextItemExpression)) {
            TypeHierarchy th = getExecutable().getConfiguration().getTypeHierarchy();
            ItemType operandItemType = argument[0].getItemType(th);
            if (th.relationship(NodeKindTest.ELEMENT, operandItemType) != TypeHierarchy.DISJOINT ||
                    th.relationship(NodeKindTest.DOCUMENT, operandItemType) != TypeHierarchy.DISJOINT) {
                result.setAtomized();
            }
        }
        return null;
    }


    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        try {
            Item arg = argument[0].evaluateItem(c);
            if (arg==null) {
                return StringValue.EMPTY_STRING;
            } else if (arg instanceof StringValue && ((StringValue)arg).getTypeLabel() == BuiltInAtomicType.STRING) {
                return arg;
            } else {
                return StringValue.makeStringValue(arg.getStringValueCS());
            }
        } catch (UnsupportedOperationException e) {
            // Cannot obtain the string value of a function item
            XPathException err = new XPathException(e.getMessage(), "FOTY0014");
            err.setLocator(this);
            err.setXPathContext(c);
            throw err;
        }
    }

    /**
     * In streaming mode, process the first half of the instruction (for example, to start a new document or element)
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

//    public void processLeft(Stack<XPathContext> contextStack, Stack state) throws XPathException {
//        state.push(new FastStringBuffer(100));
//    }

    /**
     * In streaming mode, process the right half of the instruction (for example, to end a new document or element)
     * Note that unlike other divisible instructions this one doesn't push the result to the current output
     * destination, it leaves a sequenceIterator over the result on the stack.
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

//    public void processRight(Stack<XPathContext> contextStack, Stack state) throws XPathException {
//        FastStringBuffer value = (FastStringBuffer)state.pop();
//        contextStack.peek().getReceiver().append(new StringValue(value), 0, 0);
//    }


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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
