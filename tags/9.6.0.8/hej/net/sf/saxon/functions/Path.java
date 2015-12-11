////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;

/**
 * Implement the fn:path function
 */
public class Path extends SystemFunctionCall implements Callable {

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
     * Evaluate the function
     */

    public StringValue evaluateItem(XPathContext context) throws XPathException {
        NodeInfo node = (NodeInfo) argument[0].evaluateItem(context);
        if (node == null) {
            return null;
        }
        return makePath(node, context);
    }

    /*@Nullable*/
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo node = arguments.length == 0 ? getContextNode(context) : (NodeInfo) arguments[0].head();
        if (node == null) {
            return ZeroOrOne.empty();
        }
        return new ZeroOrOne<StringValue>(makePath(node, context));
    }

    private static StringValue makePath(NodeInfo node, XPathContext context) throws XPathException {
        if (node.getNodeKind() == Type.DOCUMENT) {
            return StringValue.makeStringValue("/");
        }
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.MEDIUM);
        AxisIterator iter = node.iterateAxis(AxisInfo.ANCESTOR_OR_SELF);
        NodeInfo n;
        while ((n = iter.next()) != null) {
            if (n.getParent() == null) {
                if (n.getNodeKind() == Type.DOCUMENT) {
                    return new StringValue(fsb);
                } else {
                    fsb.prepend("Q{http://www.w3.org/2005/xpath-functions}root()");
                    return new StringValue(fsb);
                }
            }
            FastStringBuffer fsb2 = new FastStringBuffer(FastStringBuffer.MEDIUM);
            switch (n.getNodeKind()) {
                case Type.DOCUMENT:
                    return new StringValue(fsb);
                case Type.ELEMENT:
                    fsb2.append("/Q{");
                    fsb2.append(n.getURI());
                    fsb2.append("}");
                    fsb2.append(n.getLocalPart());
                    fsb2.append("[" + Navigator.getNumberSimple(n, context) + "]");
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                case Type.ATTRIBUTE:
                    fsb2.append("/@");
                    String attURI = n.getURI();
                    if (!"".equals(attURI)) {
                        fsb2.append("Q{");
                        fsb2.append(attURI);
                        fsb2.append("}");
                    }
                    fsb2.append(n.getLocalPart());
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                case Type.TEXT:
                    fsb2.append("/text()[");
                    fsb2.append(Navigator.getNumberSimple(n, context) + "]");
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                case Type.COMMENT:
                    fsb2.append("/comment()[");
                    fsb2.append(Navigator.getNumberSimple(n, context) + "]");
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                case Type.PROCESSING_INSTRUCTION:
                    fsb2.append("/processing-instruction(");
                    fsb2.append(n.getLocalPart());
                    fsb2.append(")[");
                    fsb2.append(Navigator.getNumberSimple(n, context) + "]");
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                case Type.NAMESPACE:
                    fsb2.append("/namespace::");
                    if (n.getLocalPart().length() == 0) {
                        fsb2.append("*[Q{" + NamespaceConstant.FN + "}local-name()=\"\"]");
                    } else {
                        fsb.append(n.getLocalPart());
                    }
                    fsb2.append(fsb);
                    fsb = fsb2;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        // should not reach here...
        fsb.prepend("Q{http://www.w3.org/2005/xpath-functions}root()");
        return new StringValue(fsb);
    }

}

// Copyright (c) 2012 Saxonica Limited. All rights reserved.

