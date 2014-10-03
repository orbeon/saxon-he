////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.Sweep;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.Current;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;

/**
 * Class for handling patterns with simple non-positional boolean predicates
 */
public class PatternWithPredicate extends Pattern {

    private Pattern basePattern;
    private Expression predicate;

    public PatternWithPredicate(Pattern basePattern, Expression predicate) {
        this.basePattern = basePattern;
        this.predicate = predicate;
    }

    @Override
    public void setPackageData(PackageData packageData) {
        super.setPackageData(packageData);
        basePattern.setPackageData(packageData);
    }

    /**
     * Replace any calls on current() by a variable reference bound to the supplied binding
     */
    @Override
    public void bindCurrent(LocalBinding binding) {
        if (predicate instanceof Current) {
            predicate = new LocalVariableReference(binding);
        } else if (ExpressionTool.callsFunction(predicate, Current.FN_CURRENT)) {
            replaceCurrent(predicate, binding);
        }
        basePattern.bindCurrent(binding);
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
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        Binding[] savedBindingList = offer.bindingList;
        basePattern.promote(offer, parent);
        predicate = predicate.promote(offer, parent);
        offer.bindingList = savedBindingList;
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager holds details of the allocated slots
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        int n = ExpressionTool.allocateSlots(predicate, nextFree, slotManager);
        return basePattern.allocateSlots(slotManager, n);
    }

    /**
     * Determine whether this Pattern matches the given Node.
     *
     * @param item    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */
    @Override
    public boolean matches(Item item, XPathContext context) throws XPathException {
        if (!basePattern.matches(item, context)) {
            return false;
        }
        return matchesPredicate(item, context);
    }

    private boolean matchesPredicate(Item item, XPathContext context) throws XPathException {
        XPathContext c2 = context.newMinorContext();
        ManualIterator si = new ManualIterator(item);
        c2.setCurrentIterator(si);
        try {
            return predicate.effectiveBooleanValue(c2);
        } catch (XPathException.Circularity e) {
            throw e;
        } catch (XPathException ex) {
            if ("XTDE0640".equals(ex.getErrorCodeLocalPart())) {
                // Treat circularity error as fatal (test error213)
                throw ex;
            }
            XPathException err = new XPathException("An error occurred matching pattern {" + toString() + "}: ", ex);
            err.setXPathContext(c2);
            err.setErrorCodeQName(ex.getErrorCodeQName());
            err.setLocator(this);
            c2.getController().recoverableError(err);
            return false;
        }
    }

    @Override
    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return basePattern.matchesBeneathAnchor(node, anchor, context) &&
                matchesPredicate(node, context);
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE. For patterns that
     * do not match nodes, return -1.
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    @Override
    public int getNodeKind() {
        return basePattern.getNodeKind();
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on. The default
     * implementation indicates that nodes of all kinds are matched.
     *
     * @return a combination of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on
     */
    @Override
    public int getNodeKindMask() {
        return basePattern.getNodeKindMask();
    }


    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints,
     *         or it if matches atomic values
     */
    @Override
    public int getFingerprint() {
        return basePattern.getFingerprint();
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     *
     * @return an ItemType, as specific as possible, which all the matching items satisfy
     */
    /*@Nullable*/
    @Override
    public ItemType getItemType() {
        return basePattern.getItemType();
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */
    @Override
    public int getDependencies() {
        return predicate.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */
    /*@NotNull*/
    @Override
    public Iterator<Expression> iterateSubExpressions() {
        return new PairIterator<Expression>(new PatternSponsor(basePattern), predicate);
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */
    @Override
    public Pattern analyze(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        ContextItemStaticInfo cit = new ContextItemStaticInfo(getItemType(), false, true);
        predicate = visitor.typeCheck(predicate, cit);
        predicate = visitor.optimize(predicate, cit);
        basePattern = basePattern.analyze(visitor, contextItemType);
        return this;
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     *
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     *         conversion
     * @throws net.sf.saxon.trans.XPathException
     *          if the pattern cannot be converted
     */
    @Override
    public Pattern convertToTypedPattern(String val) throws XPathException {
        Pattern b2 = basePattern.convertToTypedPattern(val);
        if (b2 == basePattern) {
            return this;
        } else {
            return new PatternWithPredicate(b2, predicate);
        }
    }

    //#ifdefined STREAM
    @Override
    public boolean isMotionless(boolean allowExtensions) {
        predicate.getStreamability(allowExtensions, new ContextItemStaticInfo(getItemType(), false, true), null);
        return basePattern.isMotionless(allowExtensions) &&
                predicate.getSweep() == Sweep.MOTIONLESS;
    }
//#endif

}

