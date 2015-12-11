////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.RootExpressionCompiler;
import com.saxonica.ee.stream.adjunct.RootExpressionAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTestPattern;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;


/**
 * An expression whose value is always a set of nodes containing a single node,
 * the document root. This corresponds to the XPath Expression "/", including the implicit
 * "/" at the start of a path expression with a leading "/".
 */

public class RootExpression extends Expression {


    private boolean contextMaybeUndefined = true;

    /**
     * Type-check the expression.
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ContextItemStaticInfo contextInfo) throws XPathException {
        if (contextInfo == null || contextInfo.getItemType() == null) {
            XPathException err = new XPathException(noContextMessage() + ": the context item is absent");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else {
            contextMaybeUndefined = contextInfo.isPossiblyAbsent();
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (th.isSubType(contextInfo.getItemType(), NodeKindTest.DOCUMENT)) {
            // this rewrite is important for streamability analysis
            ContextItemExpression cie = new ContextItemExpression();
            cie.setStaticInfo(contextInfo);
            return cie;
        }

        int relation = th.relationship(contextInfo.getItemType(), AnyNodeTest.getInstance());
        if (relation == TypeHierarchy.DISJOINT) {
            XPathException err = new XPathException(noContextMessage() + ": the context item is not a node");
            err.setErrorCode("XPTY0020");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
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
     * @throws net.sf.saxon.trans.XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        // repeat the check: in XSLT insufficient information is available the first time
        return typeCheck(visitor, contextItemType);
    }

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.CONTEXT_DOCUMENT_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
     * Ask whether there is a possibility that the context item will be undefined
     *
     * @return true if this is a possibility
     */

    public boolean isContextPossiblyUndefined() {
        return contextMaybeUndefined;
    }


    /**
     * Customize the error message on type checking
     */

    protected String noContextMessage() {
        return "Leading '/' cannot select the root node of the tree containing the context item";
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return (other instanceof RootExpression);
    }

    /**
     * Specify that the expression returns a singleton
     */

    public final int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Determine the data type of the items returned by this expression
     *
     * @return Type.NODE
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return NodeKindTest.DOCUMENT;
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return "RootExpression".hashCode();
    }

    /**
     * Return the first element selected by this Expression
     *
     * @param context The evaluation context
     * @return the NodeInfo of the first selected element, or null if no element
     *         is selected
     */

    /*@Nullable*/
    public NodeInfo getNode(XPathContext context) throws XPathException {
        Item current = context.getContextItem();
        if (current == null) {
            dynamicError("Finding root of tree: the context item is absent", "XPDY0002", context);
        }
        if (current instanceof NodeInfo) {
            DocumentInfo doc = ((NodeInfo) current).getDocumentRoot();
            if (doc == null) {
                dynamicError("The root of the tree containing the context item is not a document node", "XPDY0050", context);
            }
            return doc;
        }
        typeError("Finding root of tree: the context item is not a node", "XPTY0020", context);
        // dummy return; we never get here
        return null;
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
     * StaticProperty.CURRENT_NODE
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_DOCUMENT;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new RootExpression();
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
        return new NodeTestPattern(NodeKindTest.DOCUMENT);
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        if (pathMapNodeSet == null) {
            ContextItemExpression cie = new ContextItemExpression();
            cie.setContainer(getContainer());
            pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(cie));
        }
        return pathMapNodeSet.createArc(AxisInfo.ANCESTOR_OR_SELF, NodeKindTest.DOCUMENT);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Root expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new RootExpressionCompiler();
    }
//#endif
//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new RootExpressionAdjunct();
    }

    //#endif

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "(/)";
    }

    @Override
    public String getExpressionName() {
        return "(/)";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("root");
        destination.endElement();
    }


    /**
     * Evaluate the expression in a given context to return an iterator
     *
     * @param context the evaluation context
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(getNode(context));
    }

    public NodeInfo evaluateItem(XPathContext context) throws XPathException {
        return getNode(context);
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return getNode(context) != null;
    }
}

