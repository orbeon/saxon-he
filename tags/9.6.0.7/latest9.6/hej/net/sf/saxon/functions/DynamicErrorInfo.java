////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.SourceLocator;

/**
 * This class implements the extension function saxon:dynamic-error-info(), which underpins the
 * variables accessible within saxon:catch such as $err:code etc. This function is not documented
 * for direct use by applications, though there is no reason it would not work; instead, calls to
 * this function are generated to represent the conventional variables $err:code, $err:description, etc.
 * The function has a single argument, which is the local name of the corresponding variable name,
 * as a string.
 */

public class DynamicErrorInfo extends ExtensionFunctionDefinition {

    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.SAXON, "dynamic-error-info");

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
        return new SequenceType[]{SequenceType.SINGLE_STRING};
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
        return true;
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
        // return true to prevent loop-lifting out of the try/catch expression: see bug 2181
        return true;
    }


    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    public ExtensionFunctionCall makeCallExpression() {
        return new DynamicErrorInfoCall();
    }

    private static class DynamicErrorInfoCall extends ExtensionFunctionCall {

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
            String var = arguments[0].head().getStringValue();
            @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
            XPathException error = context.getCurrentException();
            if (error == null) {
                return EmptySequence.getInstance();
            }
            SourceLocator locator = error.getLocator();
            if (var.equals("code")) {
                StructuredQName errorCodeQName = error.getErrorCodeQName();
                if (errorCodeQName == null) {
                    return EmptySequence.getInstance();
                } else {
                    return new QNameValue(errorCodeQName, BuiltInAtomicType.QNAME);
                }
            } else if (var.equals("description")) {
                return new StringValue(error.getMessage());
            } else if (var.equals("value")) {
                Sequence value = error.getErrorObject();
                if (value == null) {
                    return EmptySequence.getInstance();
                } else {
                    return value;
                }
            } else if (var.equals("module")) {
                String module = locator == null ? null : locator.getSystemId();
                if (module == null) {
                    return EmptySequence.getInstance();
                } else {
                    return new StringValue(module);
                }
            } else if (var.equals("line-number")) {
                int line = locator == null ? -1 : locator.getLineNumber();
                if (line == -1) {
                    return EmptySequence.getInstance();
                } else {
                    return new Int64Value(line);
                }
            } else if (var.equals("column-number")) {
                int column = locator == null ? -1 : locator.getColumnNumber();
                if (column == -1) {
                    return EmptySequence.getInstance();
                } else {
                    return new Int64Value(column);
                }
            } else {
                return EmptySequence.getInstance();
            }

        }
    }

}
