package net.sf.saxon.expr;

import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

/**
 * An atomic mapping expression is a slash expression A/B where B has a static type that is an atomic type.
 * For example, * / name().
 */

public final class AtomicMappingExpression extends SlashExpression
        implements ContextMappingFunction {

    /**
     * Constructor
     * @param start A node-set expression denoting the absolute or relative set of nodes from which the
     * navigation path should start.
     * @param step The step to be followed from each node in the start expression to yield a new
     * node-set
     */

    public AtomicMappingExpression(Expression start, Expression step) {
        super(start, step);
    }

    /**
     * Determine whether this expression is capable (as far as static analysis is concerned)
     * of returning a mixture of nodes and atomic values. If so, this needs to be prevented
     * at run time
     * @return true if the static type allows both nodes and atomic values
     */

    public boolean isHybrid() {
        return false;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        start = visitor.typeCheck(start, contextItemType);
        step = visitor.typeCheck(step, start.getItemType(visitor.getConfiguration().getTypeHierarchy()));
        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new AtomicMappingExpression(getControllingExpression().copy(), getControlledExpression().copy());
    }

    /**
     * Iterate the path-expression in a given context
     * @param context the evaluation context
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {

        // This class delivers the result of the expression in unsorted order,
        // without removal of duplicates.

        SequenceIterator result = start.iterate(context);
        XPathContext context2 = context.newMinorContext();
        context2.setCurrentIterator(result);
        context2.setOrigin(this);
        context2.setOriginatingConstructType(Location.PATH_EXPRESSION);

        return new ContextMappingIterator(this, context2);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("atomicMap");
        getControllingExpression().explain(destination);
        getControlledExpression().explain(destination);
        destination.endElement();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

