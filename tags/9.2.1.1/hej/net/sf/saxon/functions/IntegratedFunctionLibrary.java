package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A library of integrated function calls, that is, user-written extension functions implemented
 * as instances of the class IntegratedFunction.
 */
public class IntegratedFunctionLibrary implements FunctionLibrary, Serializable {

    private HashMap<StructuredQName, ExtensionFunctionDefinition> functions =
            new HashMap<StructuredQName, ExtensionFunctionDefinition>();

    /**
     * Register an integrated function with this function library
     * @param function the implementation of the function (or set of functions)
     */

    public void registerFunction(ExtensionFunctionDefinition function) throws XPathException {
        functions.put(function.getFunctionQName(), function);
    }

    /**
     * Test whether a system function with a given name and arity is available, and return its signature. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param functionName the name of the function
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */


    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        ExtensionFunctionDefinition defn = functions.get(functionName);
        if (defn == null) {
            return null;
        }
        if (arity == -1) {
            return new SequenceType[0];
        } else {
            try {
                if (defn.getMinimumNumberOfArguments() <= arity && defn.getMaximumNumberOfArguments() >= arity) {
                    SequenceType[] sig = new SequenceType[arity+1];
                    sig[0] = defn.getResultType(defn.getArgumentTypes());
                    System.arraycopy(defn.getArgumentTypes(), 0, sig, 1, arity);
                    return sig;
                } else {
                    return null;
                }
            } catch (Exception err) {
                return null;
            }
        }

    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the QName of the function being called
     * @param staticArgs   The expressions supplied statically in arguments to the function call.
     *                     The length of this array represents the arity of the function. The intention is
     *                     that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     *                     be used as part of the binding algorithm. In some cases it may be possible for the function
     *                     to be pre-evaluated at compile time, for example if these expressions are all constant values.
     *                     <p/>
     *                     The conventions of the XPath language demand that the results of a function depend only on the
     *                     values of the expressions supplied as arguments, and not on the form of those expressions. For
     *                     example, the result of f(4) is expected to be the same as f(2+2). The actual expression is supplied
     *                     here to enable the binding mechanism to select the most efficient possible implementation (including
     *                     compile-time pre-evaluation where appropriate).
     * @param env          The static context of the function call
     * @return An object representing the function to be called, if one is found;
     *         null if no function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env) throws XPathException {
        ExtensionFunctionDefinition defn = functions.get(functionName);
        if (defn == null) {
            return null;
        }
        try {
            ExtensionFunctionCall f = defn.makeCallExpression();
            f.setDefinition(defn);
            IntegratedFunctionCall fc = new IntegratedFunctionCall(f);
            fc.setFunctionName(functionName);
            fc.setArguments(staticArgs);
            return fc;
        } catch (Exception err) {
            throw new XPathException("Failed to create call to extension function " + functionName.getDisplayName(), err);
        }
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        IntegratedFunctionLibrary lib = new IntegratedFunctionLibrary();
        lib.functions = new HashMap<StructuredQName, ExtensionFunctionDefinition>(functions);
        return lib;
    }

    /**
     * Return an iterator over the names of the functions registered in this library
     * @return an iterator over the function names
     */

    public Iterator<StructuredQName> iterateFunctionNames() {
        return functions.keySet().iterator();
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
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



