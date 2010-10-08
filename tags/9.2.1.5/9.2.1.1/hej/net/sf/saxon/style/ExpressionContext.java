package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.IntegratedFunctionLibrary;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;

import javax.xml.transform.SourceLocator;
import java.util.Set;

/**
* An ExpressionContext represents the context for an XPath expression written
* in the stylesheet.
*/

public class ExpressionContext implements XSLTStaticContext {

	private StyleElement element;
	private NamePool namePool;
    private NamespaceResolver namespaceResolver = null;

    /**
     * Create a static context for XPath expressions in an XSLT stylesheet
     * @param styleElement the element on which the XPath expression appears
     */

    public ExpressionContext(StyleElement styleElement) {
		element = styleElement;
		namePool = styleElement.getNamePool();
	}

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return element.getConfiguration();
    }

    /**
     * Get the executable
     * @return the executable
     */

    public Executable getExecutable() {
        return element.getExecutable();
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration(),
                element.getPrincipalStylesheet().getCollationMap());
    }


    /**
     * Get the location map
     */

    public LocationMap getLocationMap() {
        return element.getPrincipalStylesheet().getLocationMap();
    }

    /**
    * Issue a compile-time warning
    */

    public void issueWarning(String s, SourceLocator locator) {
        element.issueWarning(s, locator);
    }

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
    * Get the System ID of the entity containing the expression (used for diagnostics)
    */

    public String getSystemId() {
    	return element.getSystemId();
    }

    /**
    * Get the line number of the expression within its containing entity
    * Returns -1 if no line number is available
    */

    public int getLineNumber() {
    	return element.getLineNumber();
    }

    /**
    * Get the Base URI of the element containing the expression, for resolving any
    * relative URI's used in the expression.
    * Used by the document() function.
    */

    public String getBaseURI() {
        return element.getBaseURI();
    }

    /**
    * Get the URI for a prefix, using this Element as the context for namespace resolution.
    * The default namespace will not be used when the prefix is empty.
    * @param prefix The prefix
    * @throws XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = element.getURIForPrefix(prefix, false);
        if (uri == null) {
            XPathException err = new XPathException("Undeclared namespace prefix " + Err.wrap(prefix));
            err.setErrorCode("XPST0081");
            err.setIsStaticError(true);
            throw err;
        }
        return uri;
    }

    /**
     * Get a copy of the NamespaceResolver suitable for saving in the executable code
     * @return a NamespaceResolver
    */


    public NamespaceResolver getNamespaceResolver() {
        if (namespaceResolver == null) {
            namespaceResolver = element.makeNamespaceContext();
        }
        return namespaceResolver;
    }

    /**
     * Get a DecimalFormatManager to resolve the names of decimal formats used in calls
     * to the format-number() function.
     * @return the decimal format manager for this static context, or null if named decimal
     *         formats are not supported in this environment.
     */

    public DecimalFormatManager getDecimalFormatManager() {
        return element.getPreparedStylesheet().getDecimalFormatManager();
    }

    /**
    * Get a fingerprint for a name, using this as the context for namespace resolution
    * @param qname The name as written, in the form "[prefix:]localname"
    * @param useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    * @return -1 if the name is not already present in the name pool
    */

    public int getFingerprint(String qname, boolean useDefault) throws XPathException {

        String[] parts;
        try {
            parts = getConfiguration().getNameChecker().getQNameParts(qname);
        } catch (QNameException err) {
            throw new XPathException(err.getMessage());
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            String uri = "";

            if (useDefault) {
                uri = getURIForPrefix(prefix);
            }

			return namePool.getFingerprint(uri, qname);

        } else {

            String uri = getURIForPrefix(prefix);
			return namePool.getFingerprint(uri, parts[1]);
        }
    }

    /**
    * Get a StructuredQName for a name, using this as the context for namespace resolution
    * @param qname The name as written, in the form "[prefix:]localname"
    * @param useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    * @return -1 if the name is not already present in the name pool
    */

    public StructuredQName getStructuredQName(String qname, boolean useDefault) throws XPathException {

        String[] parts;
        try {
            parts = getConfiguration().getNameChecker().getQNameParts(qname);
        } catch (QNameException err) {
            throw new XPathException(err.getMessage());
        }
        String prefix = parts[0];
        if (prefix.length() == 0) {
            String uri = "";

            if (useDefault) {
                uri = getURIForPrefix(prefix);
            }

			return new StructuredQName("", uri, qname);

        } else {

            String uri = getURIForPrefix(prefix);
			return new StructuredQName(prefix, uri, parts[1]);
        }
    }

    private static StructuredQName[] errorVariables = {
            new StructuredQName("err", NamespaceConstant.ERR, "code"),
            new StructuredQName("err", NamespaceConstant.ERR, "description"),
            new StructuredQName("err", NamespaceConstant.ERR, "value"),
            new StructuredQName("err", NamespaceConstant.ERR, "module"),
            new StructuredQName("err", NamespaceConstant.ERR, "line-number"),
            new StructuredQName("err", NamespaceConstant.ERR, "column-number")
    };

    /**
     * Bind a variable to an object that can be used to refer to it
     * @param qName the name of the variable
     * @return a VariableDeclaration object that can be used to identify it in the Bindery,
     * @throws XPathException if the variable has not been declared
    */

    public Expression bindVariable(StructuredQName qName) throws XPathException {
        XSLVariableDeclaration xslVariableDeclaration = element.bindVariable(qName);
        if (xslVariableDeclaration == null) {
            if (NamespaceConstant.ERR.equals(qName.getNamespaceURI())) {
                AxisIterator catchers = element.iterateAxis(Axis.ANCESTOR_OR_SELF,
                        new NameTest(Type.ELEMENT, NamespaceConstant.SAXON, "catch", getNamePool()));
                StyleElement catcher = (StyleElement)catchers.next();
                if (catcher != null) {
                    for (int i=0; i<errorVariables.length; i++) {
                        if (errorVariables[i].getLocalName().equals(qName.getLocalName())) {
                            IntegratedFunctionLibrary lib = getConfiguration().getVendorFunctionLibrary();
                            StructuredQName functionName =
                                    new StructuredQName("saxon", NamespaceConstant.SAXON, "dynamic-error-info");
                            Expression[] args = new Expression[]{new StringLiteral(qName.getLocalName())};
                            return lib.bind(functionName, args, this);
                        }
                    }
                }
            }
            XPathException err = new XPathException("Variable " + qName.getDisplayName() +
                    " has not been declared");
            err.setErrorCode("XPST0008");
            err.setIsStaticError(true);
            throw err;
        }
        VariableReference var = (xslVariableDeclaration.isGlobal()
                                    ? new VariableReference()
                                    : new LocalVariableReference());
        xslVariableDeclaration.registerReference(var);
        return var;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return element.getPrincipalStylesheet().getFunctionLibrary();
    }

    /**
    * Determine if an extension element is available
    * @throws XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException {
        try {
            String[] parts = getConfiguration().getNameChecker().getQNameParts(qname);
            String uri;
            if (parts[0].length() == 0) {
                uri = getDefaultElementNamespace();
            } else {
                uri = getURIForPrefix(parts[0]);
            }
            return element.getPreparedStylesheet().
                                getStyleNodeFactory().isElementAvailable(uri, parts[1]);
        } catch (QNameException e) {
            XPathException err = new XPathException("Invalid element name. " + e.getMessage());
            err.setErrorCode("XTDE1440");
            throw err;
        }
    }

    /**
    * Get a named collation.
    * @param name The name of the required collation. Supply null to get the default collation.
    * @return the collation; or null if the required collation is not found.
    */

    public StringCollator getCollation(String name) {
        if (name == null) {
            return getCollation(getDefaultCollationName());
        } else {
            return element.getPrincipalStylesheet().findCollation(name);
        }
    }

    /**
    * Get the default collation. Return null if no default collation has been defined
    */

    public String getDefaultCollationName() {
        return element.getDefaultCollationName();
    }

    /**
     * Get the default XPath namespace for elements and types
     * Return NamespaceConstant.NULL for the non-namespace
    */

    public String getDefaultElementNamespace() {
        return element.getDefaultXPathNamespace();
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
        return element.backwardsCompatibleModeIsEnabled();
    }

    /**
     * Test whether a schema has been imported for a given namespace
     * @param namespace the target namespace of the required schema
     * @return true if a schema for this namespace has been imported
     */

    public boolean isImportedSchema(String namespace) {
        return getXSLStylesheet().isImportedSchema(namespace);
    }

    /**
     * Get the set of imported schemas
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return getXSLStylesheet().getImportedSchemaTable();
    }

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     *
     * @param type the supposedly built-in type. This will always be a type in the
     *                    XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(BuiltInAtomicType type) {
        Configuration config = getConfiguration();
        if (type.getFingerprint() == StandardNames.XS_DATE_TIME_STAMP) {
            return config.getXsdVersion() == Configuration.XSD11;
        }
        return type.isAllowedInBasicXSLT() ||
                config.isLicensedFeature(Configuration.LicenseFeature.SAXON_EXTENSION_FUNCTIONS);
    }

    /**
     * Get the XSLStylesheet object
     * @return the XSLStylesheet object representing the outermost element of the stylesheet module
    */

    public XSLStylesheet getXSLStylesheet() {
        return element.getPrincipalStylesheet();
    }

    /**
     * Get the stylesheet element containing this XPath expression
     * @return the element in the tree representation of the source stylesheet
     */

    public StyleElement getStyleElement() {
        return element;
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
