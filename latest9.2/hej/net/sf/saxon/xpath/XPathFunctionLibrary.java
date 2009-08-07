package net.sf.saxon.xpath;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

/**
 * The XPathFunctionLibrary is a FunctionLibrary that supports binding of XPath function
 * calls to instances of the JAXP XPathFunction interface returned by an XPathFunctionResolver.
 */

public class XPathFunctionLibrary implements FunctionLibrary {

    private XPathFunctionResolver resolver;

    /**
     * Construct a XPathFunctionLibrary
     */

    public XPathFunctionLibrary() {
    }

    /**
      * Set the resolver
      * @param resolver The XPathFunctionResolver wrapped by this FunctionLibrary
      */

    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        this.resolver = resolver;
    }

    /**
      * Get the resolver
      * @return the XPathFunctionResolver wrapped by this FunctionLibrary
      */
    
    public XPathFunctionResolver getXPathFunctionResolver() {
        return resolver;
    }

    /**
     * Test whether a function with a given name and arity is available; if so, return its signature.
     * This supports the function-available() function in XSLT; it is also used to support
     * higher-order functions introduced in XQuery 1.1. For this function library the method is never
     * used and therefore returns null.
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

    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        return null;
    }

    /**
     * Bind a function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name, arity, or signature.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        if (resolver == null) {
            return null;
        }
        QName name = new QName(functionName.getNamespaceURI(), functionName.getLocalName());
        XPathFunction function = resolver.resolveFunction(name, staticArgs.length);
        if (function == null) {
            return null;
        }
        XPathFunctionCall fc = new XPathFunctionCall(function);
        fc.setArguments(staticArgs);
        return fc;
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        XPathFunctionLibrary xfl = new XPathFunctionLibrary();
        xfl.resolver = resolver;
        return xfl;
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