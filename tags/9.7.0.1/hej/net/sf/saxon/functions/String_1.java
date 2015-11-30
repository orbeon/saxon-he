////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.StringFnCompiler;
import com.saxonica.ee.stream.adjunct.StringFnAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.SimpleNodeConstructor;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.One;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;


/**
 * Implement XPath function string() with a single argument
 */

public class String_1 extends ScalarSystemFunction {

    @Override
    public AtomicValue evaluate(Item arg, XPathContext context) throws XPathException {
        CharSequence result;
        try {
            result = arg.getStringValueCS();
        } catch (UnsupportedOperationException err) {
            throw new XPathException(err.getMessage(), "FOTY0014");
        }
        return new StringValue(result);
    }

    @Override
    public ZeroOrOne<? extends AtomicValue> resultWhenEmpty() {
        return new One<StringValue>(StringValue.EMPTY_STRING);
    }

    /**
     * Allow the function to create an optimized call based on the values of the actual arguments
     *
     * @param visitor     the expression visitor
     * @param contextInfo information about the context item
     * @param arguments   the supplied arguments to the function call
     * @return either a function call on this function, or an expression that delivers
     * the same result, or null indicating that no optimization has taken place
     * @throws net.sf.saxon.trans.XPathException if an error is detected
     */
    @Override
    public Expression makeOptimizedFunctionCall(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, Expression... arguments) throws XPathException {
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression arg = arguments[0];
        if (th.isSubType(arg.getItemType(), BuiltInAtomicType.STRING) &&
                arg.getCardinality() == StaticProperty.EXACTLY_ONE) {
            return arg;
        }
        if (arg instanceof SimpleNodeConstructor) {
            return ((SimpleNodeConstructor) arg).getSelect();
        }
        return null;
    }


//#ifdefined BYTECODE

    /**
     * Return the compiler of the StringFn expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new StringFnCompiler();
    }

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    //@Override
    public StringFnAdjunct getStreamingAdjunct() {
        return new StringFnAdjunct();
    }

    //#endif


}

