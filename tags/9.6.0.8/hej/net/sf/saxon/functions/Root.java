////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.RootFunctionCompiler;
import com.saxonica.ee.stream.adjunct.RootAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;

/**
 * Implement the XPath 2.0 root() function
 */


public class Root extends SystemFunctionCall {

    /**
     * Simplify and validate.
     *
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        return simplifyArguments(visitor);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getNumberOfArguments() == 0) ||
                (argument[0].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
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
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return pathMapNodeSet.createArc(AxisInfo.ANCESTOR_OR_SELF, NodeKindTest.DOCUMENT);
    }


    /**
     * Evaluate in a general context
     */

    /*@Nullable*/
    public NodeInfo evaluateItem(XPathContext c) throws XPathException {
        NodeInfo start = (NodeInfo) argument[0].evaluateItem(c);
        if (start == null) {
            return null;
        }
        return start.getRoot();
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public ZeroOrOne<NodeInfo> call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo node = arguments.length == 0 ? getContextNode(context) : (NodeInfo) arguments[0].head();
        if (node == null) {
            return ZeroOrOne.empty();
        } else {
            return new ZeroOrOne<NodeInfo>(node.getRoot());
        }
    }

    //#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new RootAdjunct();
    }
    //#endif

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Root Function
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new RootFunctionCompiler();
    }
//#endif
}

