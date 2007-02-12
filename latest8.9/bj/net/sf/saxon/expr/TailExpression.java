package net.sf.saxon.expr;

import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.Configuration;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * A TailExpression represents a FilterExpression of the form EXPR[position() > n]
 * Here n is usually 2, but we allow other values
 */
public class TailExpression extends Expression {

    Expression base;
    int start;      // 1-based offset of first item from base expression
                    // to be included

    /**
     * Construct a TailExpression, representing a filter expression of the form
     * $base[position() >= $start]
     * @param base    the expression to be filtered
     * @param start   the position (1-based) of the first item to be included
     */

    public TailExpression(Expression base, int start) {
        this.base = base;
        this.start = start;
        adoptChildExpression(base);
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.typeCheck(env, contextItemType);
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        base = base.optimize(opt, env, contextItemType);
        return this;
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                base = doPromotion(base, offer);
            }
            return this;
        }
    }

    public int computeSpecialProperties() {
        return base.getSpecialProperties();
    }

    public ItemType getItemType(TypeHierarchy th) {
        return base.getItemType(th);
    }

    public int computeCardinality() {
        return base.getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(base);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (base == original) {
             base = replacement;
             found = true;
         }
         return found;
     }


    public Expression getBaseExpression() {
        return base;
    }

    public int getStart() {
        return start;
    }

    public boolean equals(Object other) {
        return other instanceof TailExpression &&
                base.equals(((TailExpression)other).base) &&
                start == ((TailExpression)other).start;
    }

    public int hashCode() {
        return base.hashCode();
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator baseIter = base.iterate(context);
        if ((baseIter.getProperties() & SequenceIterator.GROUNDED) != 0) {
            return new ValueTailIterator(((GroundedIterator)baseIter).materialize(), start - 1);
        } else {
            return TailIterator.make(baseIter, start);
        }
    }

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "tail " + start);
        base.display(level+1, out, config);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//