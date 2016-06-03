////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LastItemExpressionCompiler;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.IdentityWrapper;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ReversibleIterator;

import java.util.Map;

/**
 * A LastItemExpression returns the last item in the sequence returned by a given
 * base expression. The evaluation strategy is to read the input sequence with a one-item lookahead.
 */

public final class LastItemExpression extends SingleItemFilter {

    /**
     * Constructor
     *
     * @param base A sequence expression denoting sequence whose first item is to be returned
     */

    public LastItemExpression(Expression base) {
        super(base);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        LastItemExpression exp = new LastItemExpression(getBaseExpression().copy(rebindings));
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
     * Evaluate the expression
     */

    /*@Nullable*/
    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator forwards = getBaseExpression().iterate(context);
        if (forwards instanceof ReversibleIterator) {
            return ((ReversibleIterator) forwards).getReverseIterator().next();
        } else {
            Item current = null;
            while (true) {
                Item item = forwards.next();
                if (item == null) {
                    return current;
                }
                current = item;
            }
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the LastItem expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LastItemExpressionCompiler();
    }
//#endif


    public String getExpressionName() {
        return "lastOf";
    }

}

