package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
 * The SystemFunctionLibrary represents the collection of functions in the fn: namespace. That is, the
 * functions defined in the "Functions and Operators" specification, optionally augmented by the additional
 * functions defined in XSLT.
 */

public class SystemFunctionLibrary implements FunctionLibrary {

    private int functionSet;

    private static IntHashMap<SystemFunctionLibrary> THE_INSTANCES =
            new IntHashMap<SystemFunctionLibrary>(3);

    /**
     * Factory method to create or get a SystemFunctionLibrary
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     * {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, {@link StandardFunction#XQUPDATE}, etc
     * @return the appropriate SystemFunctionLibrary
     */

    public static SystemFunctionLibrary getSystemFunctionLibrary(int functionSet) {
        if (THE_INSTANCES.get(functionSet) == null) {
            THE_INSTANCES.put(functionSet, new SystemFunctionLibrary(functionSet));
        }
        return THE_INSTANCES.get(functionSet);
    }

    /**
     * Create a SystemFunctionLibrary
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     * {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, {@link StandardFunction#XQUPDATE}, etc
     */

    private SystemFunctionLibrary(int functionSet) {
        this.functionSet = functionSet;
    }

    /**
     * Test whether a function with a given name and arity is available; if so, return its signature.
     * This supports the function-available() function in XSLT; it is also used to support
     * higher-order functions introduced in XQuery 1.1.
     *
     * <p>This method may be called either at compile time
     * or at run time. If the function library is to be used only in an XQuery or free-standing XPath
     * environment, this method may throw an UnsupportedOperationException.</p>
     * @param functionName the qualified name of the function being called
     * @param arity        The number of arguments. This is set to -1 in the case of the single-argument
     *                     function-available() function; in this case the method should return a zero-length array
     *                     if there is some
     *                     function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */

    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (uri.equals(NamespaceConstant.FN)) {
            StandardFunction.Entry entry = StandardFunction.getFunction(local, arity);
            if (entry == null) {
                return null;
            }
            if (!(arity == -1 || (arity >= entry.minArguments && arity <= entry.maxArguments))) {
                return null;
            }
            if ((functionSet & entry.applicability) == 0) {
                return null;
            }

            SequenceType[] sig = new SequenceType[arity+1];
            if (arity >= 0) {
                if (entry.itemType == StandardFunction.SAME_AS_FIRST_ARGUMENT) {
                    sig[0] = entry.argumentTypes[0];
                } else {
                    sig[0] = SequenceType.makeSequenceType(entry.itemType, entry.cardinality);
                }
                for (int i=0; i<arity; i++) {
                    if (i < entry.argumentTypes.length) {
                        sig[i+1] = entry.argumentTypes[i];
                    } else {
                        // special path for concat()
                        sig[i+1] = entry.argumentTypes[0];
                    }
                }
            }
            return sig;
        } else {
            return null;
        }
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the name of the function to be bound
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        String uri = functionName.getNamespaceURI();
        if (uri.equals(NamespaceConstant.FN)) {
            String local = functionName.getLocalName();
            StandardFunction.Entry entry = StandardFunction.getFunction(local, staticArgs.length);
            if (entry == null) {
                if (StandardFunction.getFunction(local, -1) == null) {
                    XPathException err = new XPathException("Unknown system function " + local + "()");
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                } else {
                    XPathException err = new XPathException("System function " + local + "() cannot be called with "
                            + pluralArguments(staticArgs.length));
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                }
            }
            if ((functionSet & entry.applicability) == 0) {
                XPathException err = new XPathException("System function " + local + "() is not available with this host language");
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }
            Class functionClass = entry.implementationClass;
            SystemFunction f;
            try {
                f = (SystemFunction)functionClass.newInstance();
            } catch (Exception err) {
                throw new AssertionError("Failed to load system function: " + err.getMessage());
            }
            f.setDetails(entry);
            f.setFunctionName(functionName);
            f.setArguments(staticArgs);
            checkArgumentCount(staticArgs.length, entry.minArguments, entry.maxArguments, local);
            return f;
        } else {
            return null;
        }
    }

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param numArgs the actual number of arguments (arity)
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param local the local name of the function (for diagnostics)
    * @return the actual number of arguments
    * @throws net.sf.saxon.trans.XPathException if the number of arguments is out of range
    */

    private int checkArgumentCount(int numArgs, int min, int max, String local) throws XPathException {
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have "
                    + min + pluralArguments(min));
        }
        if (numArgs < min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have at least "
                    + min + pluralArguments(min));
        }
        if (numArgs > max) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have no more than "
                    + max + pluralArguments(max));
        }
        return numArgs;
    }

    /**
     * Utility routine used in constructing error messages
     * @param num the number of arguments
     * @return the string " argument" or "arguments" depending whether num is plural
    */

    private static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//