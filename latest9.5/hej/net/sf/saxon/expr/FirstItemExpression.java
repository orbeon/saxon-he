////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.bytecode.ExpressionCompiler;
import com.saxonica.bytecode.FirstItemExpressionCompiler;
import com.saxonica.stream.adjunct.FirstItemExpressionAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.ItemTypePattern;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.SimplePositionalPattern;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

import java.util.List;



/**
* A FirstItemExpression returns the first item in the sequence returned by a given
* base expression
*/

public final class FirstItemExpression extends SingleItemFilter {

    /**
    * Private Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    private FirstItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        //computeStaticProperties();
    }

    /**
     * Static factory method
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
        return new FirstItemExpression(getBaseExpression().copy());
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
        if (basePattern instanceof ItemTypePattern && basePattern.getItemType() instanceof NodeTest) {
            return new SimplePositionalPattern(
                    (NodeTest)basePattern.getItemType(), Literal.makeLiteral(Int64Value.PLUS_ONE), Token.FEQ);
        }
//        if (basePattern instanceof LocationPathPattern) {
//            ((LocationPathPattern) basePattern).addFilter(new Literal(IntegerValue.PLUS_ONE));
//            return basePattern;
//        } else
        if (is30) {
            // TODO: rules unclear - bug 12455
            return basePattern;
        } else {
            throw new XPathException("The filtered expression in an XSLT 2.0 pattern must be a simple step");
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
    public int getStreamability(int syntacticContext, boolean allowExtensions, List<String> reasons) {
        return operand.getStreamability(syntacticContext, allowExtensions, reasons);
    }

     @Override
    public FirstItemExpressionAdjunct getStreamingAdjunct() {
        return new FirstItemExpressionAdjunct();
    }

    /**
     * Convert this expression to a streaming pattern (a pattern used internally to match nodes during
     * push processing of an event stream)
     *
     * @param config           the Saxon configuration
     * @param reasonForFailure a list which will be populated with messages giving reasons why the
     *                         expression cannot be converted
     * @return the equivalent pattern if conversion succeeds; otherwise null
     */
    @Override
    public Pattern toStreamingPattern(Configuration config, List<String> reasonForFailure) {
        TypeHierarchy th = config.getTypeHierarchy();
        Expression base = getBaseExpression();

        if (base instanceof AxisExpression &&
                ((AxisExpression)base).getAxis() == AxisInfo.CHILD &&
                base.getItemType(th).getPrimitiveType() == Type.ELEMENT) {
            return new SimplePositionalPattern(
                    (NodeTest)base.getItemType(th),
                    Literal.makeLiteral(IntegerValue.PLUS_ONE),
                    Token.FEQ);
        } else {
            return super.toStreamingPattern(config, reasonForFailure);
        }
    }

    //#endif
}

