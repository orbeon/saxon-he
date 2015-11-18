////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.expr.parser.XPathParser;
import net.sf.saxon.functions.Current;
import net.sf.saxon.om.*;
import net.sf.saxon.sxpath.SimpleContainer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.PrependIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DecimalValue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A Pattern represents the result of parsing an XSLT pattern string. <br>
 * Patterns are created by calling the static method Pattern.make(string). <br>
 * The pattern is used to test a particular node by calling match().
 */

public abstract class Pattern implements PatternFinder, Container {

    private double priority = 0.5;
    private String originalText;
    private PackageData packageData;
    private String systemId;      // the module where the pattern occurred
    private int lineNumber;       // the line number where the pattern occurred

    /**
     * Static factory method to make a Pattern by parsing a String. <br>
     *
     * @param pattern     The pattern text as a String
     * @param env         An object defining the compile-time context for the expression
     * @param packageData The package containing this pattern
     * @return The pattern object
     * @throws net.sf.saxon.trans.XPathException
     *          if the pattern is invalid
     */

    public static Pattern make(String pattern, StaticContext env, PackageData packageData) throws XPathException {

        DecimalValue languageLevel = DecimalValue.TWO;
        if (packageData.isAllowXPath30() && env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
            languageLevel = DecimalValue.THREE;
        }
        PatternParser parser = (PatternParser) env.getConfiguration().newExpressionParser("PATTERN", false, languageLevel);
        ((XPathParser) parser).setLanguage(XPathParser.XSLT_PATTERN, env.getXPathLanguageLevel());
        SimpleContainer container = new SimpleContainer(packageData);
        container.setLocation(env.getSystemId(), env.getLineNumber());
        ((XPathParser) parser).setDefaultContainer(container);
        Pattern pat = parser.parsePattern(pattern, env);
        pat.packageData = packageData;
        pat.setSystemId(env.getSystemId());
        pat.setLineNumber(env.getLineNumber());
        // System.err.println("Simplified [" + pattern + "] to " + pat.getClass() + " default prio = " + pat.getDefaultPriority());
        // set the pattern text for use in diagnostics
        pat.setOriginalText(pattern);
        //pat.setExecutable(exec);
        ExpressionVisitor visitor = ExpressionVisitor.make(env);
        pat = pat.simplify(visitor);
        return pat;
    }

    /**
     * Get the Configuration to which this Container belongs
     *
     * @return the Configuration
     */
    public Configuration getConfiguration() {
        return packageData.getConfiguration();
    }

    /**
     * Get data about the unit of compilation (XQuery module, XSLT package) to which this
     * container belongs
     */
    public PackageData getPackageData() {
        return packageData;
    }

    public void setPackageData(PackageData packageData) {
        this.packageData = packageData;
    }

    protected static void replaceCurrent(Expression exp, LocalBinding binding) {
        for (Operand o : exp.operands()) {
            Expression child = o.getExpression();
            if (child instanceof Current) {
                LocalVariableReference ref = new LocalVariableReference(binding);
                exp.replaceOperand(child, ref);
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
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return packageData.getLocationMap();
    }

    /**
     * Get the granularity of the container.
     *
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 1;
    }

    /**
     * Set the original text of the pattern for use in diagnostics
     *
     * @param text the original text of the pattern
     */

    public void setOriginalText(String text) {
        originalText = text;
    }

    /**
     * Simplify the pattern by applying any context-independent optimisations.
     * Default implementation does nothing.
     *
     * @param visitor the expression visitor
     * @return the optimised Pattern
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
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
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Set the system ID where the pattern occurred
     *
     * @param systemId the URI of the module containing the pattern
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the line number where the pattern occurred
     *
     * @param lineNumber the line number of the pattern in the source module
     */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
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
     *         node without changing the position in the streamed input file
     */

    public boolean isMotionless(boolean allowExtensions) {
        // default implementation for subclasses
        return true;
    }

    /**
     * Determine whether this Pattern matches the given item. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     * @param item    The item to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
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
     * @param doc     the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(DocumentInfo doc, final XPathContext context) throws XPathException {
        final int kind = getNodeKind();
        switch (kind) {
            case Type.DOCUMENT:
                if (matches(doc, context)) {
                    return SingletonIterator.makeIterator(doc);
                } else {
                    return EmptyIterator.getInstance();
                }
            case Type.ATTRIBUTE: {
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
                        if ((matches(item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allAttributes, test);
            }
            case Type.ELEMENT:
            case Type.COMMENT:
            case Type.TEXT:
            case Type.PROCESSING_INSTRUCTION: {
                AxisIterator allChildren = doc.iterateAxis(AxisInfo.DESCENDANT, NodeKindTest.makeNodeKindTest(kind));
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item mapItem(Item item) throws XPathException {
                        if ((matches(item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allChildren, test);
            }
            case Type.NODE: {
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
                        if ((matches(item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(attributesOrSelf, test);
            }
            case Type.NAMESPACE:
                throw new UnsupportedOperationException("Patterns can't match namespace nodes");
            default:
                throw new UnsupportedOperationException("Unknown node kind");
        }
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE. For patterns that
     * do not match nodes, return -1.
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    public int getNodeKind() {
        return Type.NODE;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on. The default
     * implementation indicates that nodes of all kinds are matched.
     *
     * @return a combination of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on
     */

    public int getNodeKindMask() {
        return 1 << Type.ELEMENT | 1 << Type.TEXT | 1 << Type.COMMENT | 1 << Type.PROCESSING_INSTRUCTION |
                1 << Type.ATTRIBUTE | 1 << Type.NAMESPACE | 1 << Type.DOCUMENT;
    }

    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints,
     *         or it if matches atomic values
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
     * Get the system id of the entity in which the pattern occurred
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the line number on which the pattern was defined
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number (always -1)
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the public ID (always null)
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Get the original pattern text
     */

    public String toString() {
        if (originalText != null) {
            return originalText;
        } else {
            return "pattern matching " + getItemType().toString();
        }
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
     * Replace a subexpression by a replacement subexpression
     *
     * @param original    the expression to be replaced
     * @param replacement the new expression to be inserted in its place
     * @return true if the replacement was carried out
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        throw new IllegalArgumentException("Invalid replacement");
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     * conversion
     * @throws XPathException if the pattern cannot be converted
     */

    public Pattern convertToTypedPattern(String val) throws XPathException {
        return this;
    }
}

