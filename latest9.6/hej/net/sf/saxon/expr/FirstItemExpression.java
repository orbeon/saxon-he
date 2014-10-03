////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.FirstItemExpressionCompiler;
import com.saxonica.ee.stream.adjunct.FirstItemExpressionAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Int64Value;


/**
 * A FirstItemExpression returns the first item in the sequence returned by a given
 * base expression
 */

public final class FirstItemExpression extends SingleItemFilter {

    /**
     * Private Constructor
     *
     * @param base A sequence expression denoting sequence whose first item is to be returned
     */

    private FirstItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        //computeStaticProperties();
    }

    /**
     * Static factory method
     *
     * @param base A sequence expression denoting sequence whose first item is to be returned
     * @return the FirstItemExpression, or an equivalent
     */

    public static Expression makeFirstItemExpression(Expression base) {
        if (base instanceof FirstItemExpression) {
            return base;
        } else {
            return new FirstItemExpression(base);
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        Expression e2 = new FirstItemExpression(getBaseExpression().copy());
        ExpressionTool.copyLocationInfo(this, e2);
        return e2;
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        Pattern basePattern = operand.toPattern(config, is30);
        ItemType type = basePattern.getItemType();
        if (type instanceof NodeTest) {
            if (basePattern instanceof NodeTestPattern) {
                return new SimplePositionalPattern(
                        (NodeTest) type, Literal.makeLiteral(Int64Value.PLUS_ONE, getContainer()), Token.FEQ);
            } else {
                return new GeneralNodePattern(this, (NodeTest) type);
            }
        } else {
            // For a non-node pattern, the predicate [1] is always true
            return basePattern;
        }
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item result = iter.next();
        iter.close();
        return result;
    }

    public String getExpressionName() {
        return "firstItem";
    }

    @Override
    public String toShortString() {
        return getBaseExpression().toShortString() + "[1]";
    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the FirstItem expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new FirstItemExpressionCompiler();
    }
    //#endif

    //#ifdefined STREAM

    @Override
    public FirstItemExpressionAdjunct getStreamingAdjunct() {
        return new FirstItemExpressionAdjunct();
    }
    //#endif
}

