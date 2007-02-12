package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.CollationMap;
import net.sf.saxon.expr.EarlyEvaluationContext;
import net.sf.saxon.expr.VariableReference;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.ConstructorFunctionLibrary;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.functions.SystemFunctionLibrary;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.util.Collections;
import java.util.Set;

/**
 * This class implements the static context used for evaluating use-when expressions in XSLT 2.0
 */

public class UseWhenStaticContext implements XSLTStaticContext {

    public Configuration config;
    public NamespaceResolver namespaceContext;
    public FunctionLibrary functionLibrary;
    public LocationMap locationMap;
    public StyleNodeFactory nodeFactory;
    public String baseURI;
    public String defaultXPathNamespace;

    public UseWhenStaticContext(Configuration config, NamespaceResolver namespaceContext) {
        this.config = config;
        this.namespaceContext = namespaceContext;
        this.locationMap = new LocationMap();

        FunctionLibraryList lib = new FunctionLibraryList();
        lib.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.USE_WHEN));
        lib.addFunctionLibrary(getConfiguration().getVendorFunctionLibrary());
        lib.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
        if (config.isAllowExternalFunctions()) {
            Configuration.getPlatform().addFunctionLibraries(lib, config);
        }
        functionLibrary = lib;
    }

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration(), new CollationMap(getConfiguration()));
    }


    /**
     * Get the location map
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Issue a compile-time warning
     */

    public void issueWarning(String s, SourceLocator locator) {
        StaticError err = new StaticError(s);
        err.setLocator(locator);
        try {
            config.getErrorListener().warning(err);
        } catch (TransformerException e) {
            // ignore response
        }
    }

    /**
     * Get the System ID of the container of the expression. This is the containing
     * entity (file) and is therefore useful for diagnostics. Use getBaseURI() to get
     * the base URI, which may be different.
     */

    public String getSystemId() {
        return baseURI;
    }

    /**
     * Get the line number of the expression within its containing entity
     * Returns -1 if no line number is available
     */

    public int getLineNumber() {
        return -1;
    }

    /**
     * Get the Base URI of the stylesheet element, for resolving any relative URI's used
     * in the expression.
     * Used by the document(), doc(), resolve-uri(), and base-uri() functions.
     * May return null if the base URI is not known.
     */

    public String getBaseURI() {
        return baseURI;
    }

    /**
     * Get the URI for a namespace prefix. The default namespace is NOT used
     * when the prefix is empty.
     *
     * @param prefix The prefix
     * @throws net.sf.saxon.trans.XPathException
     *          if the prefix is not declared
     */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = namespaceContext.getURIForPrefix(prefix, false);
        if (uri == null) {
            StaticError err = new StaticError("Namespace prefix '" + prefix + "' has not been declared");
            err.setErrorCode("XTDE0290");
            throw err;
        }
        return uri;
    }

    /**
     * Get the NamePool used for compiling expressions
     */

    public NamePool getNamePool() {
        return getConfiguration().getNamePool();
    }

    /**
     * Bind a variable used in this element to the XSLVariable element in which it is declared
     */

    public VariableReference bindVariable(int fingerprint) throws StaticError {
        StaticError err = new StaticError("Variables cannot be used in a use-when expression");
        err.setErrorCode("XPST0008");
        throw err;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Get a named collation.
     *
     * @param name The name of the required collation. Supply null to get the default collation.
     * @return the collation; or null if the required collation is not found.
     */

    public StringCollator getCollation(String name) {
        return null;
    }

    /**
     * Get the name of the default collation.
     *
     * @return the name of the default collation; or the name of the codepoint collation
     *         if no default collation has been defined
     */

    public String getDefaultCollationName() {
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    /**
     * Get the default XPath namespace for elements and types
     * Return NamespaceConstant.NULL for the non-namespace
    */

    public String getDefaultElementNamespace() {
        return defaultXPathNamespace;
    }

    /**
     * Get the default function namespace
     */

    public String getDefaultFunctionNamespace() {
        return NamespaceConstant.FN;
    }

    /**
     * Determine whether Backwards Compatible Mode is used
     */

    public boolean isInBackwardsCompatibleMode() {
        return false;
    }

    /**
     * Determine whether a Schema for a given target namespace has been imported. Note that the
     * in-scope element declarations, attribute declarations and schema types are the types registered
     * with the (schema-aware) configuration, provided that their namespace URI is registered
     * in the static context as being an imported schema namespace. (A consequence of this is that
     * within a Configuration, there can only be one schema for any given namespace, including the
     * null namespace).
     */

    public boolean isImportedSchema(String namespace) {
        return false;
    }

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return Collections.EMPTY_SET;
    }

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     *
     * @param type the supposedly built-in type. This will always be a type in the
     *                    XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(AtomicType type) {
        if (getConfiguration().isSchemaAware(Configuration.XSLT)) {
            return true;
        } else if (type instanceof BuiltInAtomicType) {
            return ((BuiltInAtomicType)type).isAllowedInBasicXSLT();
        } else {
            return false;
        }
    }

    /**
     * Get a namespace resolver to resolve the namespaces declared in this static context.
     *
     * @return a namespace resolver.
     */

    public NamespaceResolver getNamespaceResolver() {
        return namespaceContext;
    }

    /**
    * Determine if an extension element is available
    * @throws net.sf.saxon.trans.XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException {
        try {
            String[] parts = getConfiguration().getNameChecker().getQNameParts(qname);
            String uri;
            if (parts[0].equals("")) {
                uri = getDefaultElementNamespace();
            } else {
                uri = getURIForPrefix(parts[0]);
            }
            if (nodeFactory == null) {
                nodeFactory = new StyleNodeFactory(config, config.getErrorListener());
            }
            return nodeFactory.isElementAvailable(uri, parts[1]);
        } catch (QNameException e) {
            StaticError err = new StaticError("Invalid element name. " + e.getMessage());
            err.setErrorCode("XTDE1440");
            throw err;
        }
    }

    /**
     * Set the base URI
     */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
     * Set the default namespace for elements and types
     */

    public void setDefaultElementNamespace(String uri) {
        defaultXPathNamespace = uri;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
