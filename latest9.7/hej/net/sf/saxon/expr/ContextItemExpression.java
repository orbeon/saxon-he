////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ContextItemCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.ContextItemExprAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.AnchorPattern;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;


/**
 * This class represents the expression "(dot)", which always returns the context item.
 * This may be a AtomicValue or a Node.
 */

public class ContextItemExpression extends Expression {

    ContextItemStaticInfo staticInfo = ContextItemStaticInfo.DEFAULT;

    /**
     * Create the expression
     */

    public ContextItemExpression() {
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "contextItem";
    }

    /**
     * Create a clone copy of this expression
     *
     * @return a copy of this expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        ContextItemExpression cie2 = new ContextItemExpression();
        cie2.staticInfo = staticInfo;
        ExpressionTool.copyLocationInfo(this, cie2);
        return cie2;
    }

    protected String getErrorCodeForUndefinedContext() {
        return "XPDY0002";
    }

    /**
     * Set static information about the context item
     * @param info static information about the context item
     */

    public void setStaticInfo(ContextItemStaticInfo info) {
        staticInfo = info;
    }

    /**
     * Type-check the expression.
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ContextItemStaticInfo contextInfo) throws XPathException {
        if (contextInfo.getItemType() == ErrorType.getInstance()) {
            visitor.issueWarning("Evaluation will always fail: there is no context item", getLocation());
            ErrorExpression ee = new ErrorExpression("There is no context item", getErrorCodeForUndefinedContext(), true);
            ExpressionTool.copyLocationInfo(this, ee);
            return ee;
        } else {
            staticInfo = contextInfo;
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
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        // In XSLT, we don't catch this error at the typeCheck() phase because it's done one XPath expression
        // at a time. So we repeat the check here.
        if (contextItemType == null) {
            XPathException err = new XPathException("The context item is undefined at this point");
            err.setErrorCode(getErrorCodeForUndefinedContext());
            err.setIsTypeError(true);
            err.setLocation(getLocation());
            throw err;
        }
        // If streaming, rewrite "." as current-group()[1]. See bug 2972.
//        if (visitor.isOptimizeForStreaming()) {
//            Operand focusser = ExpressionTool.getFocusSettingContainerOperand(this);
//            if (focusser != null && focusser.getParentExpression() instanceof ForEachGroup &&
//                    focusser.getChildExpression() == ((ForEachGroup)focusser.getParentExpression()).getActionExpression()) {
//                ForEachGroup feg = (ForEachGroup)focusser.getParentExpression();
//                CurrentGroupCall cg = new CurrentGroupCall();
//                cg.setControllingInstruction(
//                        feg, feg.getSelectExpression().getItemType(), false);
//                FilterExpression fe = new FilterExpression(cg, new Literal(Int64Value.PLUS_ONE));
//                return fe;
//            }
//        }
        return this;
    }

    /**
     * Determine the item type
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return staticInfo.getItemType();
    }

    /**
     * Ask whether the context item may possibly be undefined
     *
     * @return true if it might be undefined
     */

    public boolean isContextPossiblyUndefined() {
        return staticInfo.isPossiblyAbsent();
    }

    /**
     * Get the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the special properties of this expression
     *
     * @return the value {@link StaticProperty#NON_CREATIVE}
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE | StaticProperty.CONTEXT_DOCUMENT_NODESET;
    }

    @Override
    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return (other instanceof ContextItemExpression);
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return "ContextItemExpression".hashCode();
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
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
        if (pathMapNodeSet == null) {
            pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
        }
        return pathMapNodeSet;
    }

    /**
     * Determine whether the expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     *
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     *         on the context node are downward selections using the self, child, descendant, attribute, and namespace
     *         axes.
     */

    public boolean isSubtreeExpression() {
        return true;
    }


    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        return AnchorPattern.getInstance();
    }

    //#ifdefined STREAM


    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new ContextItemExprAdjunct();
    }

    //#endif

    /**
     * Iterate over the value of the expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item == null) {
            dynamicError("The context item is absent", getErrorCodeForUndefinedContext(), context);
        }
        return SingletonIterator.makeIterator(item);
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item == null) {
            dynamicError("The context item is absent", getErrorCodeForUndefinedContext(), context);
        }
        return item;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the ContextItem expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ContextItemCompiler();
    }
//#endif

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return ".";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter destination) throws XPathException {
        destination.startElement("dot", this);
        ItemType type = getItemType();
        if (!(type == AnyItemType.getInstance())) {
            destination.emitAttribute("type", type.toString());
        }
        if (staticInfo.isPossiblyAbsent()) {
            destination.emitAttribute("flags", "a");
        }
        destination.endElement();
    }

    @Override
    public String toShortString() {
        return ".";
    }
}

