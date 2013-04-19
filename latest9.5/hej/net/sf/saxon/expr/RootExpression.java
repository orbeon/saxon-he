////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.bytecode.ExpressionCompiler;
import com.saxonica.bytecode.RootExpressionCompiler;
import com.saxonica.stream.adjunct.StreamingAdjunct;
import com.saxonica.stream.adjunct.UnsupportedOperationAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.ItemTypePattern;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.List;


/**
 * An expression whose value is always a set of nodes containing a single node,
 * the document root. This corresponds to the XPath Expression "/", including the implicit
 * "/" at the start of a path expression with a leading "/".
*/

public class RootExpression extends SingleNodeExpression {

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
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
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
    * @param context The evaluation context
    * @return the NodeInfo of the first selected element, or null if no element
    * is selected
    */

    /*@Nullable*/ public NodeInfo getNode(XPathContext context) throws XPathException {
        Item current = context.getContextItem();
        if (current==null) {
            dynamicError("Finding root of tree: the context item is absent", "XPDY0002", context);
        }
        if (current instanceof NodeInfo) {
            DocumentInfo doc = ((NodeInfo)current).getDocumentRoot();
            if (doc==null) {
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
        return new ItemTypePattern(NodeKindTest.DOCUMENT);
    }

    //#ifdefined STREAM


    /**
     * Get the "sweep" of this expression as defined in the W3C streamability specifications.
     * This provides an assessment of stylesheet code against the W3C criteria for guaranteed
     * streamability, and is implemented to allow these criteria to be tested. It is not the
     * case that all expression that emerge as streamable from this analysis are currently
     * capable of being streamed by Saxon
     *
     * @param syntacticContext one of the values {@link #NAVIGATION_CONTEXT},
     *                         {@link #NODE_VALUE_CONTEXT}, {@link #INHERITED_CONTEXT}, {@link #INSPECTION_CONTEXT}
     * @param allowExtensions  if false, the definition of "guaranteed streamability" in the
     *                         W3C specification is used. If true, Saxon extensions are permitted, which make some
     * @param reasons          the caller may supply a list, in which case the implementation may add to this
     *                         list a message explaining why the construct is not streamable, suitable for inclusion in an
     *                         error message.
     * @return one of the values {@link #W3C_MOTIONLESS}, {@link #W3C_CONSUMING},
     *         {@link #W3C_GROUP_CONSUMING}, {@link #W3C_FREE_RANGING}
     */
    @Override
    public int getStreamability(int syntacticContext, boolean allowExtensions, List<String> reasons) {
        reasons.add("Root expression '/' is free-ranging");
        return W3C_FREE_RANGING;
    }

    /**
     * Convert this expression to a streaming pattern (a pattern used internally to match nodes during
     * push processing of an event stream)
     *
     * @param config           the Saxon configuration
     * @param reasonForFailure a list which will be populated with messages giving reasons why the
     *                         expression cannot be converted
     * @return the equivalent pattern if conversion succeeds; otherwise null
     */
    @Override
    public Pattern toStreamingPattern(Configuration config, List<String> reasonForFailure) {
        return new ItemTypePattern(NodeKindTest.DOCUMENT);
    }

    //#endif

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
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
        return new UnsupportedOperationAdjunct();
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
}

