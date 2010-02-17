package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;

/**
 * Implements the saxon:item-at() function. This is handled specially because it is generated
 * by the optimizer.
 *
 * <p>The function takes two arguments: the first is an arbitrary sequence, the second is optional numeric.
 * The function returns the same result as let $N := NUMBER return SEQUENCE[$N], including cases where the
 * numeric argument is not a whole number.</p>
*/


public class ItemAt extends ExtensionFunctionDefinition {

    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.SAXON, "item-at");

    /**
     * Get the function name, as a QName
     * @return the QName of the function
     */

    public StructuredQName getFunctionQName() {
        return qName;
    }



    /**
     * Get the minimum number of arguments required by the function
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 2;
    }

    /**
     * Get the maximum number of arguments allowed by the function
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    /**
     * Get the required types for the arguments of this function, counting from zero
     * @return the required types of the argument, as defined by the function signature. Normally
     *         this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     *         that allow a variable number of arguments, the array can be smaller than this, with the last
     *         entry in the array providing the required type for all the remaining arguments.
     */

    public SequenceType[] getArgumentTypes() {
        return argumentTypes;
    }

    private static SequenceType[] argumentTypes = new SequenceType[] {
            SequenceType.ANY_SEQUENCE,
            SequenceType.OPTIONAL_NUMERIC
    };

    /**
     * Get the type of the result of the function
     * @param suppliedArgumentTypes the static types of the arguments to the function.
     *    This is provided so that a more precise result type can be returned in the common
     *    case where the type of the result depends on the type of the first argument. The value
     *    will be null if the function call has no arguments.
     * @return the return type of the function, as defined by its function signature
     */

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.makeSequenceType(
                suppliedArgumentTypes[0].getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_ONE);
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    public ExtensionFunctionCall makeCallExpression() {
        return new ItemAtCall();
    }

    /**
     * Return an object capable of compiling this IntegratedFunction call to Java source code. The returned
     * object may be null, in which case Java code generation is not supported for this IntegratedFunction.
     * If the returned object is not null, it must implement the interface
     * com.saxonica.codegen.IntegratedFunctionCompiler. The default implementation returns null
     * @return an instance of com.saxonica.codegen.IntegratedFunctionCompiler that generates Java code
     *         to implement the call on this extension function
     */

    public Object getCompilerForJava() {
        try {
            return Class.forName("com.saxonica.codegen.ItemAtCompiler").newInstance();
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }


    private static class ItemAtCall extends ExtensionFunctionCall {

        /**
         * Optimize the function call. This method is called at compile time if expression optimization
         * is enabled. It gives the implementation an opportunity to replace itself with an optimized implementation
         * that returns the same result.
         * @param context   The static context in which the function call appears. The method must not modify
         *                  the static context.
         * @param arguments The XPath expressions supplied in the call to this function. The method must not
         *                  modify this array, or any of the expressions contained in the array.
         * @return an expression to be evaluated at run-time in place of this function call. Return null
         *         if no optimization is possible and the function call should be used unchanged. Return a
         *         {@link net.sf.saxon.expr.Literal} representing the result of the function call if the function call
         *         can be precomputed at compile time
         * @throws net.sf.saxon.trans.XPathException
         *          if the implementation is able to detect a static error in the way the
         *          function is being called (for example it might require that the types of the arguments are
         *          consistent with each other).
         */

        public Expression rewrite(StaticContext context, Expression[] arguments) throws XPathException {
            if (arguments[1] instanceof Literal) {
                NumericValue val = (NumericValue)((Literal)arguments[1]).getValue();
                if (val.compareTo(1) < 0 || val.compareTo(Integer.MAX_VALUE) > 0 || !val.isWholeNumber()) {
                    return new Literal(EmptySequence.getInstance());
                }
                if (val.compareTo(1) > 0 && !Cardinality.allowsMany(arguments[0].getCardinality())) {
                    return new Literal(EmptySequence.getInstance());
                }
            }
            return null;
        }

        /**
         * Evaluate this function call at run-time
         * @param arguments The values of the arguments to the function call. Each argument value (which is in general
         *                  a sequence) is supplied in the form of an iterator over the items in the sequence. If required, the
         *                  supplied sequence can be materialized by calling, for example, <code>new SequenceExtent(arguments[i])</code>.
         *                  If the argument is always a singleton, then the single item may be obtained by calling
         *                  <code>arguments[i].next()</code>. The implementation is not obliged to read all the items in each
         *                  <code>SequenceIterator</code> if they are not required to compute the result; but if any SequenceIterator is not read
         *                  to completion, it is good practice to call its close() method.
         * @param context   The XPath dynamic evaluation context
         * @return an iterator over the results of the function. If the result is a single item, it can be
         *         returned in the form of a {@link net.sf.saxon.om.SingletonIterator}. If the result is an empty sequence,
         *         the method should return <code>EmptyIterator.getInstance()</code>
         * @throws net.sf.saxon.trans.XPathException
         *          if a dynamic error occurs during evaluation of the function. The Saxon run-time
         *          code will add information about the error location.
         */

        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            NumericValue index = (NumericValue)arguments[1].next();
            if (index == null) {
                return EmptyIterator.getInstance();
            }
            if (index.compareTo(Integer.MAX_VALUE) <= 0 && index.isWholeNumber()) {
                int intindex = (int)index.longValue();
                if (intindex < 1) {
                    return EmptyIterator.getInstance();
                }
                Item item;
                SequenceIterator base = arguments[0];
                if (intindex == 1) {
                    item = base.next();
                } else if (base instanceof GroundedIterator) {
                    GroundedValue value = ((GroundedIterator)base).materialize();
                    item = value.itemAt(intindex-1);
                } else {
                    SequenceIterator tail = TailIterator.make(base, intindex);
                    item = tail.next();
                }
                return SingletonIterator.makeIterator(item);
            } else {
                // there is no item at the required position
                return EmptyIterator.getInstance();
            }
        }
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
