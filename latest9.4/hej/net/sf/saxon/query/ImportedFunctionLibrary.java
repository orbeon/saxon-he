package net.sf.saxon.query;

import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This implementation of FunctionLibrary contains all the functions imported into a Query Module.
 * It is implemented as a view of the "global" XQueryFunctionLibrary for the whole query, selecting
 * only those functions that are in an imported namespace.
 */

public class ImportedFunctionLibrary implements FunctionLibrary, XQueryFunctionBinder {

    private transient QueryModule importingModule;
    private XQueryFunctionLibrary baseLibrary;
    /*@NotNull*/ private HashSet namespaces = new HashSet(5);

    /**
     * Create an imported function library
     * @param importingModule the module importing the library
     * @param baseLibrary the function library of which this is a subset view
     */

    public ImportedFunctionLibrary(QueryModule importingModule, XQueryFunctionLibrary baseLibrary) {
        this.importingModule = importingModule;
        this.baseLibrary = baseLibrary;
    }

    /**
     * Add an imported namespace
     * @param namespace the imported namespace
     */

    public void addImportedNamespace(String namespace) {
        namespaces.add(namespace);
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     *
     * @param functionName the name of the function to be bound
     * @param staticArgs The expressions supplied statically in arguments to the function call.
     *                   The length of this array represents the arity of the function. The intention is
     *                   that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     *                   be used as part of the binding algorithm. In some cases it may be possible for the function
     *                   to be pre-evaluated at compile time, for example if these expressions are all constant values.
     *                   <p/>
     *                   The conventions of the XPath language demand that the results of a function depend only on the
     *                   values of the expressions supplied as arguments, and not on the form of those expressions. For
     *                   example, the result of f(4) is expected to be the same as f(2+2). The actual expression is supplied
     *                   here to enable the binding mechanism to select the most efficient possible implementation (including
     *                   compile-time pre-evaluation where appropriate).
     * @param env
     * @param container
     * @return An object representing the function to be called, if one is found;
     *         null if no function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function.
     */

    /*@Nullable*/ public Expression bind(/*@NotNull*/ StructuredQName functionName, Expression[] staticArgs, StaticContext env, Container container) throws XPathException {
        final String uri = functionName.getURI();
        if (namespaces.contains(uri)) {
            Expression call = baseLibrary.bind(functionName, staticArgs, env, container);
            if (call != null) {
                // Check that the result type and all the argument types are in the static context of the
                // calling module
                XQueryFunction def = baseLibrary.getDeclaration(functionName, staticArgs);
                importingModule.checkImportedFunctionSignature(def);
            }
            return call;
        } else {
            return null;
        }
    }

    /**
     * Get the function declaration corresponding to a given function name and arity
     * @return the XQueryFunction if there is one, or null if not.
     */

    /*@Nullable*/ public XQueryFunction getDeclaration(/*@NotNull*/ StructuredQName functionName, Expression[] staticArgs) {
        String uri = functionName.getURI();
        if (namespaces.contains(uri)) {
            return baseLibrary.getDeclaration(functionName, staticArgs);
        } else {
            return null;
        }
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    /*@NotNull*/ public FunctionLibrary copy() {
        ImportedFunctionLibrary lib = new ImportedFunctionLibrary(importingModule, baseLibrary);
        Iterator iter = namespaces.iterator();
        while (iter.hasNext()) {
            String ns = (String)iter.next();
            lib.addImportedNamespace(ns);
        }
        return lib;
    }

    /**
     * Set the module that imports this function libary
     * @param importingModule the importing module
     */

    public void setImportingModule(QueryModule importingModule) {
        this.importingModule = importingModule;
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
     *                     function-available() function; in this case the method should return true if there is some
     *                     function of this name available for calling.
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; or a zero-length
     * array if the function exists but the signature is not known; or null if the function does not exist
     */

    /*@Nullable*/ public SequenceType[] getFunctionSignature(/*@NotNull*/ StructuredQName functionName, int arity) {
        if (namespaces.contains(functionName.getURI())) {
            return baseLibrary.getFunctionSignature(functionName, arity);
        } else {
            return null;
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//