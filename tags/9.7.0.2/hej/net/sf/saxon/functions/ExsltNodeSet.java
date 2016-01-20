////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
 * This class implements the extension function exslt:node-set. This is available in all Saxon
 * editions including Saxon-HE. It is provided in this form so that it is a true function, making
 * it available for example in function-available(), function-lookup(), and as a function literal
 * exslt:node-set#1.
 *
 * The function is implemented to simply return its single operand unchanged. (Its original purpose,
 * to convert an XSLT 1.0 result tree fragment to a node-set, is long since obsolete, since there
 * is no longer any distinction between the two types; but the function call is commonly used in
 * XSLT 1.0 stylesheets).
 */

public class ExsltNodeSet extends ExtensionFunctionDefinition {

    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.EXSLT_COMMON, "node-set");

    /**
     * Get the function name, as a QName
     *
     * @return the QName of the function
     */

    public StructuredQName getFunctionQName() {
        return qName;
    }


    /**
     * Get the minimum number of arguments required by the function
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    /**
     * Get the maximum number of arguments allowed by the function
     *
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return 1;
    }

    /**
     * Get the required types for the arguments of this function, counting from zero
     *
     * @return the required types of the argument, as defined by the function signature. Normally
     *         this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     *         that allow a variable number of arguments, the array can be smaller than this, with the last
     *         entry in the array providing the required type for all the remaining arguments.
     */

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.ANY_SEQUENCE};
    }

    /**
     * Get the type of the result of the function
     *
     * @param suppliedArgumentTypes the static types of the arguments to the function.
     *                              This is provided so that a more precise result type can be returned in the common
     *                              case where the type of the result depends on the type of the first argument. The value
     *                              will be null if the function call has no arguments.
     * @return the return type of the function, as defined by its function signature
     */

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.ANY_SEQUENCE;
    }

    /**
     * Ask whether the result of the function depends on the focus
     *
     * @return true if the result of the function depends on the context item, position, or size.
     *         The default implementation returns false. This must be set to true if the function
     *         makes use of any of these values from the dynamic context.
     */

    public boolean dependsOnFocus() {
        return false;
    }

    /**
     * Ask whether the function has side-effects. If the function does have side-effects, the optimizer
     * will be less aggressive in moving or removing calls to the function. However, calls on functions
     * with side-effects can never be guaranteed.
     *
     * @return true if the function has side-effects (including creation of new nodes, if the
     *         identity of those nodes is significant). The default implementation returns false.
     */
    @Override
    public boolean hasSideEffects() {
        return false;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    public ExtensionFunctionCall makeCallExpression() {
        return new ExsltNodeSetCall();
    }

    private static class ExsltNodeSetCall extends ExtensionFunctionCall {

        /**
         * Evaluate this function call at run-time
         *
         * @param context   The XPath dynamic evaluation context
         * @param arguments The values of the arguments to the function call. Each argument value (which is in general
         *                  a sequence) is supplied in the form of an iterator over the items in the sequence. If required, the
         *                  supplied sequence can be materialized by calling, for example, <code>new SequenceExtent(arguments[i])</code>.
         *                  If the argument is always a singleton, then the single item may be obtained by calling
         *                  <code>arguments[i].next()</code>. The implementation is not obliged to read all the items in each
         *                  <code>SequenceIterator</code> if they are not required to compute the result; but if any SequenceIterator is not read
         *                  to completion, it is good practice to call its close() method.
         * @return an iterator over the results of the function. If the result is a single item, it can be
         *         returned in the form of a {@link net.sf.saxon.tree.iter.SingletonIterator}. If the result is an empty sequence,
         *         the method should return <code>EmptyIterator.getInstance()</code>
         * @throws net.sf.saxon.trans.XPathException
         *          if a dynamic error occurs during evaluation of the function. The Saxon run-time
         *          code will add information about the error location.
         */

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            return arguments[0];
        }
    }

}
