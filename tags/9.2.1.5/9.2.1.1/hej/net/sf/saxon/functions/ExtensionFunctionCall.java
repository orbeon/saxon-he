package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

import java.io.Serializable;

/**
 * This abstract class is provided to allow user-written extension functions to be implemented
 * with the full capabilities of functions that are an intrinsic part of the Saxon product.
 * In particular, the class has the opportunity to save data from the static context and
 * to optimize itself at compile time.
 *
 * <p>Instances of this class are created by calling the method makeCallExpression() on the
 * {@link ExtensionFunctionDefinition} object that represents the definition of the function.</p>
 *
 * <p>The compiler will create one instance of this class for each function call appearing in the
 * expression being compiled. The class must therefore have a public zero-argument constructor.</p>
 *
 * <p>The compiler will ensure that the supplied arguments in the extension function call are converted
 * if necessary to the declared argument types, by applying the standard conversion rules. The result
 * returned by the function is checked against the declared return type, but no conversion takes place:
 * the returned value must strictly conform to the declared type.</p>
 *
 * <p>Note that an IntegratedFunction is trusted; calls are allowed even if the configuration option
 * {@link net.sf.saxon.FeatureKeys#ALLOW_EXTERNAL_FUNCTIONS} is false. In cases where an IntegratedFunction
 * is used to load and execute untrusted code, it should check this configuration option before doing so.</p>
 *
 * @since 9.2
 */
public abstract class ExtensionFunctionCall implements Serializable {

    ExtensionFunctionDefinition definition;

    public final void setDefinition(ExtensionFunctionDefinition definition) {
        this.definition = definition;
    }

    /**
     * Get the definition of this extension function
     * @return the function definition from which this ExtensionFunctionCall was created
     */

    public ExtensionFunctionDefinition getDefinition() {
        return definition;
    }

   /**
     * Supply static context information.
     * <p>This method is called during compilation to provide information about the static context in which
     * the function call appears. If the implementation of the function needs information from the static context,
     * then it should save it now, as it will not be available later at run-time.</p>
     * <p>The implementation also has the opportunity to examine the expressions that appear in the
     * arguments to the function call at this stage. These might already have been modified from the original
     * expressions as written by the user. The implementation must not modify any of these expressions.</p>
     * <p>The default implementation of this method does nothing.</p>
     * @param context The static context in which the function call appears. The method must not modify
     * the static context.
     * @param locationId An integer code representing the location of the call to the extension function
     * in the stylesheet; can be used in conjunction with the locationMap held in the static context for diagnostics
     * @param arguments The XPath expressions supplied in the call to this function. The method must not
     * modify this array, or any of the expressions contained in the array.
     * @throws XPathException if the implementation is able to detect a static error in the way the
     * function is being called (for example it might require that the types of the arguments are
     * consistent with each other).
     */

    public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Rewrite the function call. This method is called at compile time. It gives the implementation
     * an opportunity to replace itself with an optimized implementation that returns the same result.
     * This includes the ability to pre-evaluate the function and return its result as a literal value.
     *
     * <p>There is also a further opportunity to perform static checking at this stage and to throw an error
     * if the arguments are invalid.</p>
     *
     * @param context The static context in which the function call appears. The method must not modify
     * the static context.
     * @param arguments The XPath expressions supplied in the call to this function. This method is called
     * after type-checking, so these expressions may have been modified by adding atomization operators
     * or type-checking operations, for example.
     * @return an expression to be evaluated at run-time in place of this function call. Return null
     * if no rewriting is required and the function call should be used unchanged. Return a
     * {@link net.sf.saxon.expr.Literal} representing the result of the function call if the function call
     * can be precomputed at compile time
     * @throws XPathException if the implementation is able to detect a static error in the way the
     * function is being called (for example it might require that the types of the arguments are
     * consistent with each other).
     */

    public Expression rewrite(StaticContext context, Expression[] arguments) throws XPathException {
        // default implementation does nothing
        return null;
    }

    /**
     * Copy local data from one copy of the function to another. This method must be implemented
     * in any implementation that maintains local data retained from the static context; the job of the
     * method is to copy this local data to the supplied destination function.
     *
     * <p>This method is called if a call to the extension function needs to be copied during
     * the process of optimization. For example, this occurs if the function containing the call
     * to the extension function is inlined.</p>
     *
     * <p>If any objects held as local data for the function call are mutable then deep copies must
     * be made.</p>
     *
     * @param destination the function to which the local data must be copied. This will always
     * be an instance of the same function class as the source function.
     */

    public void copyLocalData(ExtensionFunctionCall destination) {
        // default implementation does nothing
    }

    /**
     * Evaluate this function call at run-time
     *
     * @param arguments The values of the arguments to the function call. Each argument value (which is in general
     * a sequence) is supplied in the form of an iterator over the items in the sequence. If required, the
     * supplied sequence can be materialized by calling, for example, <code>new SequenceExtent(arguments[i])</code>.
     * If the argument is always a singleton, then the single item may be obtained by calling
     * <code>arguments[i].next()</code>. The implementation is not obliged to read all the items in each
     * <code>SequenceIterator</code> if they are not required to compute the result; but if any SequenceIterator is not read
     * to completion, it is good practice to call its close() method.
     * @param context The XPath dynamic evaluation context
     * @return an iterator over the results of the function. If the result is a single item, it can be
     * returned in the form of a {@link net.sf.saxon.om.SingletonIterator}. If the result is an empty sequence,
     * the method should return <code>EmptyIterator.getInstance()</code>
     * @throws XPathException if a dynamic error occurs during evaluation of the function. The Saxon run-time
     * code will add information about the error location.
     */

    public abstract SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException;

    /**
     * Compute the effective boolean value of the result.
     *
     * <p>Implementations can override this method but are not required to do so. If it is overridden,
     * the result must be consistent with the rules for calculating effective boolean value. The method
     * should be implemented in cases where computing the effective boolean value is significantly cheaper
     * than computing the full result.</p>
     *
     * @param context The XPath dynamic evaluation context
     * @param arguments The values of the arguments to the function call. Each argument value (which is in general
     * a sequence) is supplied in the form of an iterator over the items in the sequence. If required, the
     * supplied sequence can be materialized by calling, for example, <code>new SequenceExtent(arguments[i])</code>.
     * If the argument is always a singleton, then the single item may be obtained by calling
     * <code>arguments[i].next()</code>. The implementation is not obliged to read all the items in each
     * <code>SequenceIterator</code> if they are not required to compute the result; but if any SequenceIterator is not read
     * to completion, it is good practice to call its close() method.
     * @return the effective boolean value of the result
     * @throws XPathException if a dynamic error occurs during evaluation of the function. The Saxon run-time
     * code will add information about the error location.
     */

    public boolean effectiveBooleanValue(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        return ExpressionTool.effectiveBooleanValue(call(arguments, context));
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

