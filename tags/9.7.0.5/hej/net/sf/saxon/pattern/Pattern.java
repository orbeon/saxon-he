////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.expr.parser.XPathParser;
import net.sf.saxon.functions.Current;
import net.sf.saxon.om.*;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.PrependIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A Pattern represents the result of parsing an XSLT pattern string. <br>
 * Patterns are created by calling the static method Pattern.make(string). <br>
 * The pattern is used to test a particular node by calling match().
 */

public abstract class Pattern extends PseudoExpression {

    private double priority = 0.5;

    /**
     * Static factory method to make a Pattern by parsing a String. <br>
     *
     * @param pattern     The pattern text as a String
     * @param env         An object defining the compile-time context for the expression
     * @param packageData The package containing this pattern
     * @return The pattern object
     * @throws net.sf.saxon.trans.XPathException if the pattern is invalid
     */

    public static Pattern make(String pattern, StaticContext env, PackageData packageData) throws XPathException {

        int languageLevel = 20;
        if (packageData.getXPathVersion() >= 30 && env.getXPathVersion() >= 30) {
            languageLevel = 30;
        }
        int lineNumber = env instanceof ExpressionContext ? ((ExpressionContext) env).getStyleElement().getLineNumber() : -1;
        PatternParser parser = (PatternParser) env.getConfiguration().newExpressionParser("PATTERN", false, languageLevel);
        ((XPathParser) parser).setLanguage(XPathParser.XSLT_PATTERN, env.getXPathVersion());
        Pattern pat = parser.parsePattern(pattern, env, lineNumber);
        pat.setRetainedStaticContext(env.makeRetainedStaticContext());
        // System.err.println("Simplified [" + pattern + "] to " + pat.getClass() + " default prio = " + pat.getDefaultPriority());
        // set the pattern text for use in diagnostics
        pat.setOriginalText(pattern);
        pat = pat.simplify();
        return pat;
    }

    /**
     * Replace any call to current() within a contained expression by a reference to a variable
     * @param exp the expression in which the replacement is to take place (which must not itself be
     *            a call to current())
     * @param binding the binding for the variable reference
     */

    protected static void replaceCurrent(Expression exp, LocalBinding binding) {
        for (Operand o : exp.operands()) {
            Expression child = o.getChildExpression();
            if (child.isCallOn(Current.class)) {
                LocalVariableReference ref = new LocalVariableReference(binding);
                o.setChildExpression(ref);
            } else {
                replaceCurrent(child, binding);
            }
        }
    }

    /**
     * Replace any calls on current() by a variable reference bound to the supplied binding
     */

    public void bindCurrent(LocalBinding binding) {
        // default: no action
    }

    /**
     * Set the original text of the pattern for use in diagnostics
     *
     * @param text the original text of the pattern
     */

    public void setOriginalText(String text) {
        /*originalText = text;*/
    }

    /**
     * Simplify the pattern by applying any context-independent optimisations.
     * Default implementation does nothing.
     *
     */

    public Pattern simplify() throws XPathException {
        return this;
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextInfo     the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return 0;
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {
        List<Expression> list = Collections.emptyList();
        return list.iterator();
    }

    /**
     * Set the static context for use in evaluating subexpressions
     * @param rsc the static context
     */
//
//    public void setRetainedStaticContext(RetainedStaticContext rsc) {
//        for (Iterator<Expression> iter = iterateSubExpressions(); iter.hasNext();) {
//            Expression exp = iter.next();
//            exp.setRetainedStaticContext(rsc);
//        }
//    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree    the next slot that is free to be allocated
     * @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        return nextFree;
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent
     * @throws net.sf.saxon.trans.XPathException if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Test whether a pattern is motionless, that is, whether it can be evaluated against a node
     * without repositioning the input stream. This is a necessary condition for patterns used
     * as the match pattern of a streamed template rule.
     *
     * @param allowExtensions if false, the result is determined strictly according to the W3C
     *                        "guaranteed streamability rules. If true, Saxon extensions are permitted: that is, constructs
     *                        may be recognized as motionless by Saxon even if they are not recognized as motionless by
     *                        the W3C rules.
     * @return true if the pattern is motionless, that is, if it can be evaluated against a streamed
     * node without changing the position in the streamed input file
     */

    public boolean isMotionless(boolean allowExtensions) {
        // default implementation for subclasses
        return true;
    }

    /**
     * Evaluate a pattern as a boolean expression, returning true if the context item matches the pattern
     * @param context the evaluation context
     * @return true if the context item matches the pattern
     * @throws XPathException if an error occurs during pattern matching
     */

    public final boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return matches(context.getContextItem(), context);
    }

    /**
     * Determine whether this Pattern matches the given item. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     * @param item    The item to be tested against the Pattern
     * @param context The dynamic context.
     * @return true if the node matches the Pattern, false otherwise
     * @throws XPathException if an error occurs while matching the pattern (the caller will usually
     *                        treat this the same as a false result)
     */

    public abstract boolean matches(Item item, XPathContext context) throws XPathException;

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     * @throws XPathException if an error occurs while matching the pattern (the caller will usually
     *                        treat this the same as a false result)
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        // default implementation ignores the anchor node
        return matches(node, context);
    }

    /**
     * Select nodes in a document using this PatternFinder.
     *
     * @param document     the document
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(TreeInfo document, final XPathContext context) throws XPathException {
        NodeInfo doc = document.getRootNode();
        final UType uType = getUType();
        if (UType.DOCUMENT.subsumes(uType)) {
            if (matches(doc, context)) {
                return SingletonIterator.makeIterator(doc);
            } else {
                return EmptyIterator.getInstance();
            }
        } else if (UType.ATTRIBUTE.subsumes(uType)) {
            AxisIterator allElements = doc.iterateAxis(AxisInfo.DESCENDANT, NodeKindTest.ELEMENT);
            MappingFunction<NodeInfo, NodeInfo> atts = new MappingFunction<NodeInfo, NodeInfo>() {
                public SequenceIterator map(NodeInfo item) {
                    return item.iterateAxis(AxisInfo.ATTRIBUTE);
                }
            };
            SequenceIterator allAttributes =
                    new MappingIterator<NodeInfo, NodeInfo>(allElements, atts);
            ItemMappingFunction<NodeInfo, NodeInfo> test = new ItemMappingFunction<NodeInfo, NodeInfo>() {
                public NodeInfo mapItem(NodeInfo item) throws XPathException {
                    if (matches(item, context)) {
                        return item;
                    } else {
                        return null;
                    }
                }
            };
            return new ItemMappingIterator(allAttributes, test);
        } else if (UType.NAMESPACE.subsumes(uType)) {
            AxisIterator allElements = doc.iterateAxis(AxisInfo.DESCENDANT, NodeKindTest.ELEMENT);
            MappingFunction<NodeInfo, NodeInfo> atts = new MappingFunction<NodeInfo, NodeInfo>() {
                public SequenceIterator map(NodeInfo item) {
                    return item.iterateAxis(AxisInfo.NAMESPACE);
                }
            };
            SequenceIterator allNamespaces =
                    new MappingIterator<NodeInfo, NodeInfo>(allElements, atts);
            ItemMappingFunction<NodeInfo, NodeInfo> test = new ItemMappingFunction<NodeInfo, NodeInfo>() {
                public NodeInfo mapItem(NodeInfo item) throws XPathException {
                    if (matches(item, context)) {
                        return item;
                    } else {
                        return null;
                    }
                }
            };
            return new ItemMappingIterator(allNamespaces, test);

        } else if (UType.CHILD_NODE_KINDS.subsumes(uType)) {
            AxisIterator allChildren = doc.iterateAxis(AxisInfo.DESCENDANT, new MultipleNodeKindTest(uType));
            ItemMappingFunction test = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (matches(item, context)) {
                        return item;
                    } else {
                        return null;
                    }
                }
            };
            return new ItemMappingIterator(allChildren, test);
        } else {
            // TODO: handle patterns that match namespaces as well as other nodes
            AxisIterator allChildren = doc.iterateAxis(AxisInfo.DESCENDANT);
            MappingFunction<NodeInfo, NodeInfo> attsOrSelf = new MappingFunction<NodeInfo, NodeInfo>() {
                public SequenceIterator map(NodeInfo item) {
                    return new PrependIterator(item, item.iterateAxis(AxisInfo.ATTRIBUTE));
                }
            };
            SequenceIterator attributesOrSelf =
                    new MappingIterator<NodeInfo, NodeInfo>(allChildren, attsOrSelf);
            ItemMappingFunction test = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (matches(item, context)) {
                        return item;
                    } else {
                        return null;
                    }
                }
            };
            return new ItemMappingIterator(attributesOrSelf, test);

        }
    }

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */

    public abstract UType getUType();


    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints,
     * or it if matches atomic values
     */

    public int getFingerprint() {
        return -1;
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     *
     * @return an ItemType, as specific as possible, which all the matching items satisfy
     */

    public abstract ItemType getItemType();

    /**
     * Set a priority to override the default priority. This is used when the pattern is written in a complex
     * form such as a[true()] justifying a priority of 0.5, but then simplifies down to an NodeTestPattern
     *
     * @param priority the priority to be used if no explicit priority is given in the template rule
     */

    public void setPriority(double priority) {
        this.priority = priority;
    }

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     *
     * @return the default priority for the pattern
     */

    public double getDefaultPriority() {
        return priority;
    }

    /**
     * Get a string representation of the pattern. This will be in a form similar to the
     * original pattern text, but not necessarily identical. It is not guaranteed to be
     * in legal pattern syntax.
     */

    public String toString() {
        return "pattern matching " + getItemType().toString();
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     *
     * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XSLT;
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     *
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     * conversion
     * @throws XPathException if the pattern cannot be converted
     */

    public Pattern convertToTypedPattern(String val) throws XPathException {
        return null;
    }

    public abstract void export(ExpressionPresenter presenter) throws XPathException;

//    /**
//     * Copy location information (the line number, priority and references to package) from one pattern
//     * to another
//     *
//     * @param from the pattern containing the location information
//     * @param to   the pattern to which the information is to be copied
//     */

//    public static void copyLocationInfo(Pattern from, Pattern to) {
//        if (from != null && to != null) {
//            to.setSystemId(from.getSystemId());
//            to.setLineNumber(from.getLineNumber());
//            to.setPackageData(from.getPackageData());
//            to.setOriginalText(from.getOriginalText());
//            to.setPriority(from.getDefaultPriority());
//        }
//    }

    public abstract Pattern copy();

    /**
     * Perform optimisation of an expression and its subexpressions. This is the third and final
     * phase of static optimization.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor     an expression visitor
     * @param contextInfo the static type of "." at the point where this expression is invoked.
     *                    The parameter is set to null if it is known statically that the context item will be undefined.
     *                    If the type of the context item is not known statically, the argument is set to
     *                    {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException if an error is discovered during this phase
     *                                           (typically a type error)
     */
    @Override
    public Pattern optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        return this;
    }
}

