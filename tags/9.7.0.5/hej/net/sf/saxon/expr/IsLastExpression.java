////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.IsLastExpressionCompiler;
import com.saxonica.ee.stream.adjunct.IsLastExprAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.BooleanValue;


/**
 * A position() eq last() expression, generated by the optimizer.
 */

public final class IsLastExpression extends Expression {

    private boolean condition;

    /**
     * Construct a condition that tests position() eq last() (if condition
     * is true) or position() ne last() (if condition is false).
     *
     * @param condition true if we are testing "equals", false for "not equals".
     */

    public IsLastExpression(boolean condition) {
        this.condition = condition;
    }

    /**
     * Get the condition we are testing for
     *
     * @return true if we are testing "equals", false for "not equals".
     */

    public boolean getCondition() {
        return condition;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) {
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) {
        return this;
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    public BooleanValue evaluateItem(XPathContext c) throws XPathException {
        return BooleanValue.get(condition == c.isAtLast());
    }

    /**
     * Determine the data type of the expression
     *
     * @return Type.BOOLEAN
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the dependencies of this expression on the context
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        IsLastExpression exp = new IsLastExpression(condition);
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
     * Is this expression the same as another expression?
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof IsLastExpression && ((IsLastExpression)other).condition == condition;
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return condition ? 0x236b91a0 : 0x896b92a0;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter destination) throws XPathException {
        destination.startElement("isLast", this);
        destination.emitAttribute("test", condition ? "1" : "0");
        destination.endElement();
    }

    /**
     * <p>The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form.</p>
     * <p/>
     * <p>For subclasses of Expression that represent XPath expressions, the result should always be a string that
     * parses as an XPath 3.0 expression.</p>
     *
     * @return a representation of the expression as a string
     */
    @Override
    public String toString() {
        if (condition) {
            return "position() eq last()";
        } else {
            return "position() ne last()";
        }
    }

//#ifdefined STREAM


    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new IsLastExprAdjunct();
    }

    //#endif

//#ifdefined BYTECODE

    /**
     * Return the compiler for the IsLast expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new IsLastExpressionCompiler();
    }
//#endif
}
