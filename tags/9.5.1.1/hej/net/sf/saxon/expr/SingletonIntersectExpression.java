////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;

/**
 * This expression is equivalent to (A intersect B) in the case where A has cardinality
 * zero-or-one. This is handled as a special case because the standard sort-merge algorithm
 * involves an unnecessary sort on B.
 */
public class SingletonIntersectExpression extends VennExpression {

    /**
     * Special case of an intersect expression where the first argument is a singleton
     * @param p1 the first argument, always a singleton
     * @param op the operator, always Token.INTERSECT
     * @param p2 the second argument
     */

    public SingletonIntersectExpression(final Expression p1, final int op, final Expression p2) {
        super(p1, op, p2);
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    /*@NotNull*/
    @Override
    public Expression copy() {
        return new SingletonIntersectExpression(operand0.copy(), operator, operand1.copy());
    }

    /**
     * Iterate over the value of the expression. The result will always be sorted in document order,
     * with duplicates eliminated
     * @param c The context for evaluation
     * @return a SequenceIterator representing the union of the two operands
     */

    /*@NotNull*/
    @Override
    public SequenceIterator iterate(XPathContext c) throws XPathException {
        NodeInfo m = (NodeInfo)operand0.evaluateItem(c);
        if (m == null) {
            return EmptyIterator.getInstance();
        }
        SequenceIterator iter = operand1.iterate(c);
        while (true) {
            NodeInfo n = (NodeInfo)iter.next();
            if (n == null) {
                return EmptyIterator.getInstance();
            }
            if (n.isSameNodeInfo(m)) {
                return SingletonIterator.makeIterator(m);
            }
        }
    }

    /**
     * Get the effective boolean value. In the case of a union expression, this
     * is reduced to an OR expression, for efficiency
     */

    @Override
    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        NodeInfo m = (NodeInfo)operand0.evaluateItem(c);
        if (m == null) {
            return false;
        }
        SequenceIterator iter = operand1.iterate(c);
        while (true) {
            NodeInfo n = (NodeInfo)iter.next();
            if (n == null) {
                return false;
            }
            if (n.isSameNodeInfo(m)) {
                return true;
            }
        }
    }

    public String getExpressionName() {
        return "singleton-intersect";
    }

/**
     * Display the operator used by this binary expression
     * @return String representation of the operator (for diagnostic display only)
     */

    protected String displayOperator() {
        return "singleton-intersect";
    }

}

