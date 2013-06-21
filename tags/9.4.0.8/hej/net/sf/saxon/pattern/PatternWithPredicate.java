package net.sf.saxon.pattern;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.PairIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;

/**
 *
 */
public class PatternWithPredicate extends Pattern {

    private Pattern basePattern;
    private Expression predicate;

    public PatternWithPredicate(Pattern basePattern, Expression predicate) {
        this.basePattern = basePattern;
        this.predicate = predicate;
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
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
        // TODO: set current()
        // TODO: reexamine the semantics - see bug 12455
        XPathContext c2 = context.newMinorContext();
        SingletonIterator si = (SingletonIterator)SingletonIterator.makeIterator(item);
        si.next();
        c2.setCurrentIterator(si);
        return predicate.effectiveBooleanValue(context);
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     *
     * @return an ItemType, as specific as possible, which all the matching items satisfy
     */
    /*@Nullable*/@Override
    public ItemType getItemType() {
        return basePattern.getItemType();
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */
    /*@NotNull*/
    @Override
    public Iterator iterateSubExpressions() {
        return new PairIterator(new PatternSponsor(basePattern), predicate);
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
    public Pattern analyze(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        ExpressionVisitor.ContextItemType cit = new ExpressionVisitor.ContextItemType(getItemType(), false);
        predicate = visitor.typeCheck(predicate, cit);
        return this;
    }

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     *
     * @return the default priority for the pattern
     */
    @Override
    public double getDefaultPriority() {
        return basePattern.getDefaultPriority() + 0.5;
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