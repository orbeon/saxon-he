package net.sf.saxon.pattern;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.sort.DocumentSorter;
import net.sf.saxon.om.Axis;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;

/**
 * This is a singleton class used to convert an expression to an equivalent pattern.
 * This version of the class is used to generate conventional XSLT match patterns;
 * there is another version used to generate patterns suitable for streamed evaluation
 * in Saxon-EE.
 */
public class PatternMaker {

    /**
     * Static factory method to make a pattern by converting an expression. The supplied
     * expression is the equivalent expression to the pattern, in the sense that it takes
     * the same syntactic form.
     *
     * <p>Note that this method does NOT check all the rules for XSLT patterns; it deliberately allows
     * a (slightly) wider class of expressions to be converted than XSLT allows.</p>
     *
     * <p>The expression root() at the start of the expression has a special meaning: it refers to
     * the root of the subtree in which the pattern must match, which can be supplied at run-time
     * during pattern matching. This is used for patterns that represent streamable path expressions.</p>
     *
     * @param expression the expression to be converted
     * @param config the Saxon configuration
     * @throws net.sf.saxon.trans.XPathException if the expression cannot be converted
     */

    public static Pattern fromExpression(Expression expression, Configuration config) throws XPathException {
        Pattern result = null;
        if (expression instanceof DocumentSorter) {
            expression = ((DocumentSorter)expression).getBaseExpression();
        }
        if (expression instanceof VennExpression && ((VennExpression)expression).getOperator() == Token.UNION) {
            result = new UnionPattern(
                    fromExpression(((VennExpression)expression).getOperands()[0], config),
                    fromExpression(((VennExpression)expression).getOperands()[1], config));
        } else if (expression instanceof AxisExpression) {
            int axis = ((AxisExpression)expression).getAxis();
            NodeTest test = ((AxisExpression)expression).getNodeTest();

            if (test == null) {
                test = AnyNodeTest.getInstance();
            }
            if (test instanceof AnyNodeTest && (axis == Axis.CHILD || axis == Axis.DESCENDANT)) {
                test = AnyChildNodeTest.getInstance();
            }
            int kind = test.getPrimitiveType();
            if (axis == Axis.SELF && kind == Type.DOCUMENT) {
                result = new NodeTestPattern(test);
            } else if (axis == Axis.ATTRIBUTE) {
                if (kind == Type.NODE) {
                    // attribute::node() matches any attribute, and only an attribute
                    result = new NodeTestPattern(NodeKindTest.ATTRIBUTE);
                } else if (!Axis.containsNodeKind(axis, kind)) {
                    // for example, attribute::comment()
                    result = new NodeTestPattern(EmptySequenceTest.getInstance());
                } else {
                    result = new NodeTestPattern(test);
                }
            } else if (axis == Axis.CHILD || axis == Axis.DESCENDANT || axis == Axis.DESCENDANT_OR_SELF) {
                if (kind != Type.NODE && !Axis.containsNodeKind(axis, kind)) {
                     test = EmptySequenceTest.getInstance();
                }
                result = new NodeTestPattern(test);
            } else {
                throw new XPathException("Only downwards axes are allowed in a pattern", "XTSE0340");
            }
            // TODO: //A only matches an A element in a tree rooted at a document
        } else if (expression instanceof FilterExpression) {
            Expression base = ((FilterExpression)expression).getControllingExpression();
            Expression filter = ((FilterExpression)expression).getFilter();
            Pattern basePattern = fromExpression(base, config);
            if (basePattern instanceof NodeTestPattern) {
                LocationPathPattern path = new LocationPathPattern();
                path.setNodeTest(basePattern.getNodeTest());
                basePattern = path;
            }
            if (!(basePattern instanceof LocationPathPattern)) {
                throw new XPathException("The filtered expression in a pattern must be a simple step");
            }
            ((LocationPathPattern)basePattern).addFilter(filter);
            result = basePattern;
        } else if (expression instanceof SlashExpression) {
            Expression head = ((SlashExpression)expression).getLeadingSteps();
            Expression tail =  ((SlashExpression)expression).getLastStep();
            Pattern tailPattern = fromExpression(tail, config);
            if (tailPattern instanceof NodeTestPattern) {
                LocationPathPattern path = new LocationPathPattern();
                path.setNodeTest(tailPattern.getNodeTest());
                tailPattern = path;
            }
            if (!(tailPattern instanceof LocationPathPattern)) {
                throw new XPathException("The path in a pattern must contain simple steps: found " + tailPattern.toString());
            }
            if (((LocationPathPattern)tailPattern).getUpperPattern() != null) {
                throw new XPathException("The path in a pattern must contain simple steps");
            }
            byte axis = getAxisForPathStep(tail);
            Pattern headPattern = fromExpression(head, config);
            ((LocationPathPattern)tailPattern).setUpperPattern(axis, headPattern);
            result = tailPattern;
        } else if (expression instanceof ContextItemExpression) {
            result = new AnchorPattern();
        } else if (expression instanceof RootExpression) {
            result = new NodeTestPattern(NodeKindTest.DOCUMENT);
        } else {
            TypeHierarchy th = config.getTypeHierarchy();
            ItemType type = expression.getItemType(th);
            if (((expression.getDependencies() & StaticProperty.DEPENDS_ON_NON_DOCUMENT_FOCUS) == 0) &&
                    (type instanceof NodeTest || expression instanceof VariableReference)) {
                result = new NodeSetPattern(expression, config);
            }
        }
        if (result == null) {
            throw new XPathException("Cannot convert the expression {" + expression.toString() + "} to a pattern");
        } else {
            result.setOriginalText(expression.toString());
            return result;
        }
    }

    public static byte getAxisForPathStep(Expression step) throws XPathException {
        if (step instanceof AxisExpression) {
            return Axis.inverseAxis[((AxisExpression)step).getAxis()];
        } else if (step instanceof FilterExpression) {
            return getAxisForPathStep(((FilterExpression)step).getControllingExpression());
        } else if (step instanceof PathExpression) {
            return getAxisForPathStep(((PathExpression)step).getFirstStep());
        } else if (step instanceof ContextItemExpression) {
            return Axis.SELF;
        } else {
            throw new XPathException("The path in a pattern must contain simple steps");
        }
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
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



