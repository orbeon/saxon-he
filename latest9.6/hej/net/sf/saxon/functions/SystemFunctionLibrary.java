////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.functions.hof.CallableFunctionItem;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.z.IntHashMap;

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
     *
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     *                    {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, {@link StandardFunction#XQUPDATE}, etc
     * @return the appropriate SystemFunctionLibrary
     */

    public static synchronized SystemFunctionLibrary getSystemFunctionLibrary(int functionSet) {
        if (THE_INSTANCES.get(functionSet) == null) {
            THE_INSTANCES.put(functionSet, new SystemFunctionLibrary(functionSet));
        }
        return THE_INSTANCES.get(functionSet);
    }

    /**
     * Create a SystemFunctionLibrary
     *
     * @param functionSet determines the set of functions allowed. One or more of the bit settings
     *                    {@link StandardFunction#CORE}, {@link StandardFunction#XSLT}, {@link StandardFunction#XQUPDATE}, etc
     */

    private SystemFunctionLibrary(int functionSet) {
        this.functionSet = functionSet;
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     *
     * @param symbolicName the name and arity of the function to be bound
     * @param staticArgs   The expressions supplied statically in the function call. The intention is
     *                     that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     *                     be used as part of the binding algorithm.
     * @param env
     * @param container
     * @return An object representing the extension function to be called, if one is found;
     *         null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function; or if this function library "owns" the namespace containing
     *          the function call, but no function was found.
     */

    public Expression bind(SymbolicName symbolicName, Expression[] staticArgs, StaticContext env, Container container)
            throws XPathException {
        StructuredQName functionName = symbolicName.getComponentName();
        int arity = symbolicName.getArity();
        String uri = functionName.getURI();
        if (uri.equals(NamespaceConstant.FN)) {
            String local = functionName.getLocalPart();
            StandardFunction.Entry entry = StandardFunction.getFunction(local, arity);
            if (entry == null) {
                if (StandardFunction.getFunction(local, -1) == null) {
                    XPathException err = new XPathException("Unknown system function " + local + "()");
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                } else {
                    XPathException err = new XPathException("System function " + local + "() cannot be called with "
                            + pluralArguments(arity));
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                }
            }
            if ((entry.properties & StandardFunction.IMP_CX_I) != 0 && arity == 0) {
                // Add the context item as an implicit argument
                staticArgs = new Expression[1];
                staticArgs[0] = new ContextItemExpression();
                arity = 1;
                entry = StandardFunction.getFunction(local, arity);
            }
            if ((entry.properties & StandardFunction.IMP_CX_D) != 0) {
                // Add the context document as an implicit argument
                Expression[] newArgs = new Expression[arity + 1];
                System.arraycopy(staticArgs, 0, newArgs, 0, arity);
                newArgs[arity] = new RootExpression();
                staticArgs = newArgs;
                arity++;
                entry = StandardFunction.getFunction(local, arity);
            }
            if ((functionSet & entry.applicability) == 0) {
                XPathException err = new XPathException(
                        "System function " + local + "#" + staticArgs.length + " is not available with this host language/version");
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }
            Class functionClass = entry.implementationClass;
            SystemFunctionCall f;
            try {
                f = (SystemFunctionCall) functionClass.newInstance();
            } catch (Exception err) {
                err.printStackTrace();
                throw new AssertionError("Failed to load system function fn:" + local + ":" + err.getMessage());
            }
            f.setDetails(entry);
            f.setFunctionName(functionName);
            checkArgumentCount(arity, entry.minArguments, entry.maxArguments, local);
            if (staticArgs != null) {
                f.setArguments(staticArgs);
            }
            f.setContainer(container);
            f.bindStaticContext(env);
            return f;
        } else {
            return null;
        }
    }

//#ifdefined HOF

    /**
     * Test whether a function with a given name and arity is available; if so, return a function
     * item that can be dynamically called.
     * <p/>
     * <p>This supports the function-lookup() function in XPath 3.0.</p>
     *
     * @param functionName  the qualified name and arity of the function being called
     * @param staticContext the static context to be used by the function, in the event that
     *                      it is a system function with dependencies on the static context
     * @param container
     * @return if a function of this name and arity is available for calling, then a corresponding
     *         function item; or null if the function does not exist
     * @throws net.sf.saxon.trans.XPathException
     *          in the event of certain errors, for example attempting to get a function
     *          that is private
     */
    public FunctionItem getFunctionItem(SymbolicName functionName, StaticContext staticContext, Container container) throws XPathException {
        SuppliedParameterReference[] args = new SuppliedParameterReference[functionName.getArity()];
        Expression call = bind(functionName, args, staticContext, container);
        if (call == null) {
            return null;
        } else if (call instanceof SystemFunctionCall) {
            TypeHierarchy th = staticContext.getConfiguration().getTypeHierarchy();
            FunctionItemType type = ((SystemFunctionCall) call).getFunctionItemType(th);
            return new CallableFunctionItem(functionName, ((SystemFunctionCall) call).getConvertingCallable(), type);
        } else {
            throw new UnsupportedOperationException("Cannot create function item for function " + functionName);
        }
    }
//#endif


    /**
     * Test whether a function with a given name and arity is available
     * <p>This supports the function-available() function in XSLT.</p>
     *
     * @param functionName the qualified name of the function being called
     * @return true if a function of this name and arity is available for calling
     */
    public boolean isAvailable(SymbolicName functionName) {
        String uri = functionName.getComponentName().getURI();
        if (uri.equals(NamespaceConstant.FN)) {
            String local = functionName.getComponentName().getLocalPart();
            StandardFunction.Entry entry = StandardFunction.getFunction(local, functionName.getArity());
            return entry != null && (functionSet & entry.applicability) != 0 &&
                    entry.minArguments <= functionName.getArity() && entry.maxArguments >= functionName.getArity();
        } else {
            return false;
        }
    }

    /**
     * Check number of arguments. <BR>
     * A convenience routine for use in subclasses.
     *
     * @param numArgs the actual number of arguments (arity)
     * @param min     the minimum number of arguments allowed
     * @param max     the maximum number of arguments allowed
     * @param local   the local name of the function (for diagnostics)
     * @return the actual number of arguments
     * @throws net.sf.saxon.trans.XPathException
     *          if the number of arguments is out of range
     */

    private int checkArgumentCount(int numArgs, int min, int max, String local) throws XPathException {
        if (min == max && numArgs != min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have "
                    + pluralArguments(min), "XPST0017");
        }
        if (numArgs < min) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have at least "
                    + pluralArguments(min), "XPST0017");
        }
        if (numArgs > max) {
            throw new XPathException("Function " + Err.wrap(local, Err.FUNCTION) + " must have no more than "
                    + pluralArguments(max), "XPST0017");
        }
        return numArgs;
    }

    /**
     * Utility routine used in constructing error messages
     *
     * @param num the number of arguments
     * @return the string " argument" or "arguments" depending whether num is plural
     */

    private static String pluralArguments(int num) {
        if (num == 0) {
            return "zero arguments";
        }
        if (num == 1) {
            return "one argument";
        }
        return num + " arguments";
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