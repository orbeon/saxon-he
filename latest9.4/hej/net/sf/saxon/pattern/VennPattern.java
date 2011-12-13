package net.sf.saxon.pattern;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LetExpression;
import net.sf.saxon.expr.MultiIterator;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A pattern formed as the union, intersection, or difference of two other patterns
 */

public abstract class VennPattern extends Pattern {

    protected Pattern p1, p2;
    private int nodeType = Type.NODE;

    /**
     * Constructor
     *
     * @param p1 the left-hand operand
     * @param p2 the right-hand operand
     */

    public VennPattern(Pattern p1, Pattern p2) {
        this.p1 = p1;
        this.p2 = p2;
        if (p1.getNodeKind() == p2.getNodeKind()) {
            nodeType = p1.getNodeKind();
        }
    }


    /**
     * Set the executable containing this pattern
     *
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        p1.setExecutable(executable);
        p2.setExecutable(executable);
        super.setExecutable(executable);
    }

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     * @param visitor an expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        p1 = p1.simplify(visitor);
        p2 = p2.simplify(visitor);
        return this;
    }

    /**
     * Type-check the pattern.
     * This is only needed for patterns that contain variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        p1 = p1.analyze(visitor, contextItemType);
        p2 = p2.analyze(visitor, contextItemType);
        return this;
    }

    /**
     * If the pattern contains any calls on current(), this method is called to modify such calls
     * to become variable references to a variable declared in a specially-allocated local variable
     *
     * @param let   the expression that assigns the local variable. This returns a dummy result, and is executed
     *              just before evaluating the pattern, to get the value of the context item into the variable.
     * @param offer A PromotionOffer used to process the expressions and change the call on current() into
     *              a variable reference
     * @param topLevel
     * @throws net.sf.saxon.trans.XPathException
     */

    public void resolveCurrent(LetExpression let, PromotionOffer offer, boolean topLevel) throws XPathException {
        p1.resolveCurrent(let, offer, topLevel);
        p2.resolveCurrent(let, offer, topLevel);
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link net.sf.saxon.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        p1.promote(offer, parent);
        p2.promote(offer, parent);
    }

    /**
     * Replace a subexpression by a replacement subexpression
     * @param original    the expression to be replaced
     * @param replacement the new expression to be inserted in its place
     * @return true if the replacement was carried out
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        return p1.replaceSubExpression(original, replacement) ||
                p2.replaceSubExpression(original, replacement);
    }

    /**
     * Set the original text
     */

    public void setOriginalText(String pattern) {
        super.setOriginalText(pattern);
        p1.setOriginalText(pattern);
        p2.setOriginalText(pattern);
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param env      the static context in the XSLT stylesheet
     * @param slotManager
     *@param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        nextFree = p1.allocateSlots(env, slotManager, nextFree);
        nextFree = p2.allocateSlots(env, slotManager, nextFree);
        return nextFree;
    }

    /**
     * Gather the component (non-Venn) patterns of this Venn pattern
     * @param set the set into which the components will be added
     */

    public void gatherComponentPatterns(Set set) {
        if (p1 instanceof VennPattern) {
            ((VennPattern)p1).gatherComponentPatterns(set);
        } else {
            set.add(p1);
        }
        if (p2 instanceof VennPattern) {
            ((VennPattern)p2).gatherComponentPatterns(set);
        } else {
            set.add(p2);
        }
    }

     /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Node.NODE
     *
     * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
     */

    public int getNodeKind() {
        return nodeType;
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     */

    public ItemType getItemType() {
        if (nodeType == Type.NODE) {
            return AnyNodeTest.getInstance();
        } else if (nodeType == -1) {
            // pattern does not match nodes
            return AnyItemType.getInstance();
        } else {
            return NodeKindTest.makeNodeKindTest(nodeType);
        }
    }


    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return p1.getDependencies() | p2.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     * @return an iterator over the subexpressions. 
     */

    /*@NotNull*/
    public Iterator iterateSubExpressions() {
        return new MultiIterator(new Iterator[]{p1.iterateSubExpressions(), p2.iterateSubExpressions()});
    }

    /**
     * Get the LHS of the union
     *
     * @return the first operand of the union
     */

    public Pattern getLHS() {
        return p1;
    }

    /**
     * Get the RHS of the union
     *
     * @return the second operand of the union
     */

    public Pattern getRHS() {
        return p2;
    }

    /**
     * Override method to set the system ID, so it's set on both halves
     */

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        p1.setSystemId(systemId);
        p2.setSystemId(systemId);
    }

    /**
     * Override method to set the system ID, so it's set on both halves
     */

    public void setLineNumber(int lineNumber) {
        super.setLineNumber(lineNumber);
        p1.setLineNumber(lineNumber);
        p2.setLineNumber(lineNumber);
    }

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof VennPattern) {
            Set s0 = new HashSet(10);
            gatherComponentPatterns(s0);
            Set s1 = new HashSet(10);
            ((VennPattern)other).gatherComponentPatterns(s1);
            return s0.equals(s1);
        } else {
            return false;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x9bd723a6 ^ p1.hashCode() ^ p2.hashCode();
    }


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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//