package net.sf.saxon.query;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.UserFunctionCall;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;

import java.util.HashMap;
import java.util.Iterator;

/**
 * An XQueryFunctionLibrary is a function library containing all the user-defined functions available for use within a
 * particular XQuery module: that is, the functions declared in that module, and the functions imported from other
 * modules. It also contains (transiently during compilation) a list of function calls within the module that have not
 * yet been bound to a specific function declaration.
*/

public class XQueryFunctionLibrary implements FunctionLibrary, XQueryFunctionBinder {

	private Configuration config;

    // The functions in this library are represented using a HashMap
    // The key of the hashmap is a Long whose two halves hold (a) the fingerprint, and (b) the arity
    // The valye in the hashmap is an XQueryFunction
    private HashMap functions = new HashMap(20);

	/**
	* Create an XQueryFunctionLibrary
	*/

	public XQueryFunctionLibrary(Configuration config) {
        this.config = config;
	}

    /**
     * Set the Configuration options
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Configuration options
     */

    public Configuration getConfiguration() {
        return config;
    }

    private NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Register a user-defined XQuery function
     */

    public void declareFunction(XQueryFunction function) throws StaticError {
        int fp = function.getFunctionFingerprint();
        int arity = function.getNumberOfArguments();
        Long keyObj = functionKey(fp, arity);
        if (functions.get(keyObj) != null) {
            XQueryFunction old = (XQueryFunction)functions.get(keyObj);
            StaticError err = new StaticError("Duplicate definition of function " +
                    getNamePool().getDisplayName(fp) +
                    " (see line " + old.getLineNumber() + " in " + old.getSystemId() + ')');
            err.setErrorCode("XQST0034");
            err.setLocator(function);
            throw err;
        }
        functions.put(keyObj, function);
    }

    /**
     * Compute a unique key for a function based on its name and arity
     * @param fp the fingerprint of the function name
     * @param arity the arity (number of arguments) of the function
     * @return a unique key used to identify the function
     */
    private static Long functionKey(int fp, int arity) {
        return new Long((((long)arity)<<32) + (long)fp);
    }

    /**
     * Test whether a function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        if (arity == -1) {
            // we'll just test arity 0..20. Since this method is supporting an XSLT-only interrogative
            // on an XQuery function library, the outcome isn't too important.
            for (int i=0; i<20; i++) {
                if (isAvailable(fingerprint, uri, local, i)) {
                    return true;
                }
            }
            return false;
        }

        return (functions.get(functionKey(fingerprint, arity)) != null);
    }

    /**
     * Identify a (namespace-prefixed) function appearing in the expression. This
     * method is called by the XQuery parser to resolve function calls found within
     * the query.
     * <p>Note that a function call may appear earlier in the query than the definition
     * of the function to which it is bound. Unlike XSLT, we cannot search forwards to
     * find the function definition. Binding of function calls is therefore a two-stage
     * process; at the time the function call is parsed, we simply register it as
     * pending; subsequently at the end of query parsing all the pending function
     * calls are resolved. Another consequence of this is that we cannot tell at the time
     * a function call is parsed whether it is a call to an internal (XSLT or XQuery)
     * function or to an extension function written in Java.
     * @return an Expression representing the function call. This will normally be
     * a FunctionCall, but it may be rewritten as some other expression.
     * @throws XPathException if the function call is invalid, either because it is
     * an unprefixed call to a non-system function, or because it is calling a system
     * function that is available in XSLT only. A prefixed function call that cannot
     * be recognized at this stage is assumed to be a forwards reference, and is bound
     * later when bindUnboundFunctionCalls() is called.
    */

    public Expression bind(int nameCode, String uri, String local, Expression[] arguments) throws XPathException {
        int fp = nameCode & 0xfffff;
        XQueryFunction fd = (XQueryFunction)functions.get(functionKey(fp, arguments.length));
        if (fd != null) {
            UserFunctionCall ufc = new UserFunctionCall();
            ufc.setFunctionNameCode(nameCode);
            ufc.setArguments(arguments);
            ufc.setStaticType(fd.getResultType());
            UserFunction fn = fd.getUserFunction();
            if (fn == null) {
                // not yet compiled
                fd.registerReference(ufc);
                ufc.setConfirmed(true);
            } else {
                ufc.setFunction(fn, fd.getStaticContext());
                ufc.checkFunctionCall(fn, fd.getStaticContext());
            }
            return ufc;
        } else {
            return null;
        }
    }

    /**
     * Get the function declaration corresponding to a given function name and arity
     * @return the XQueryFunction if there is one, or null if not.
     */

    public XQueryFunction getDeclaration(int nameCode, String uri, String local, Expression[] staticArgs) {
        return (XQueryFunction)functions.get(functionKey(nameCode & NamePool.FP_MASK, staticArgs.length));
    }

    /**
     * Get an iterator over the Functions defined in this module
     * @return an Iterator, whose items are {@link XQueryFunction} objects. It returns
     * all function known to this module including those imported from elsewhere; they
     * can be distinguished by their namespace.
     */

    public Iterator getFunctionDefinitions() {
        return functions.values().iterator();
    }

    /**
     * Fixup all references to global functions. This method is called
     * on completion of query parsing. Each XQueryFunction is required to
     * bind all references to that function to the object representing the run-time
     * executable code of the function.
     * <p>
     * This method is for internal use.
     */

    protected void fixupGlobalFunctions(StaticQueryContext env) throws XPathException {
        Iterator iter = functions.values().iterator();
        while (iter.hasNext()) {
            XQueryFunction fn = (XQueryFunction)iter.next();
            fn.compile(env);
        }
        iter = functions.values().iterator();
        while (iter.hasNext()) {
            XQueryFunction fn = (XQueryFunction)iter.next();
            fn.checkReferences(env);
        }
    }

    /**
     * Output "explain" information about each declared function
     */

     public void explainGlobalFunctions() throws XPathException {
        Iterator iter = functions.values().iterator();
        while (iter.hasNext()) {
            XQueryFunction fn = (XQueryFunction)iter.next();
            fn.explain(getConfiguration());
        }
    }

    /**
     * Get the function with a given name and arity. This method is provided so that XQuery functions
     * can be called directly from a Java application. Note that there is no type checking or conversion
     * of arguments when this is done: the arguments must be provided in exactly the form that the function
     * signature declares them.
     * @param uri the uri of the function name
     * @param localName the local part of the function name
     * @param arity the number of arguments.
     */

    public UserFunction getUserDefinedFunction(String uri, String localName, int arity) {
        int fp = getNamePool().allocate("", uri, localName) & 0xfffff;
        XQueryFunction function = (XQueryFunction)functions.get(functionKey(fp, arity));
        if (function==null) {
            return null;
        }
        return function.getUserFunction();
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        XQueryFunctionLibrary qfl = new XQueryFunctionLibrary(config);
        qfl.functions = new HashMap(functions);
        return qfl;
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
