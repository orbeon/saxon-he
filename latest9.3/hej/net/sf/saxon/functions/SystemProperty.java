package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.style.UseWhenStaticContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends SystemFunction {

    private NamespaceResolver nsContext;
    private StructuredQName propertyName;
    private transient boolean checked = false;
    private boolean isSchemaAware = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        if (argument[0] instanceof StringLiteral) {
            try {
                propertyName = StructuredQName.fromLexicalQName(
                        ((StringLiteral)argument[0]).getStringValue(),
                        false,
                        visitor.getConfiguration().getNameChecker(),
                        visitor.getStaticContext().getNamespaceResolver());
            } catch (XPathException e) {
                String code = e.getErrorCodeLocalPart();
                if (code==null || code.equals("FOCA0002") || code.equals("FONS0004")) {
                    e.setErrorCode("XTDE1390");
                    throw e;
                }
            }
            // Don't actually read the system property yet, it might be different at run-time
        } else {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
            isSchemaAware = visitor.getStaticContext().isSchemaAware();
        }
    }

    /**
     * preEvaluate: this method performs compile-time evaluation for properties in the XSLT namespace only
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (propertyName != null && NamespaceConstant.XSLT.equals(propertyName.getNamespaceURI())) {
            if (propertyName.getLocalName().equals("is-schema-aware")) {
                StaticContext env = visitor.getStaticContext();
                boolean aware;
                if (env instanceof UseWhenStaticContext) {
                    Configuration config = env.getConfiguration();
                    aware = "EE".equals(config.getEditionCode()) &&
                            config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
                } else {
                    aware = env.isSchemaAware();
                }
                return new StringLiteral(aware ? "yes" : "no");
            }
            return new StringLiteral(
                    getProperty(NamespaceConstant.XSLT, propertyName.getLocalName(), visitor.getConfiguration()));
        } else {
           return this;
        }
    }

    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        StructuredQName qName = propertyName;
        if (qName == null) {
            CharSequence name = argument[0].evaluateItem(context).getStringValueCS();
            try {
                qName = StructuredQName.fromLexicalQName(name,
                        false,
                        context.getConfiguration().getNameChecker(),
                        nsContext);
                if (NamespaceConstant.XSLT.equals(qName.getNamespaceURI()) &&
                        "is-schema-aware".equals(qName.getLocalName())) {
                    return new StringValue(isSchemaAware ? "yes" : "no");
                }
            } catch (XPathException err) {
                 dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
                 return null;
            }
        }
        return new StringValue(getProperty(
                qName.getNamespaceURI(), qName.getLocalName(), context.getConfiguration()));
    }

    /**
     * Here's the real code:
     * @param uri the namespace URI of the system property name
     * @param local the local part of the system property name
     * @param config the Saxon configuration
     * @return the value of the corresponding system property 
    */

    public static String getProperty(String uri, String local, Configuration config) {
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (local.equals("version")) {
                return Version.getXSLVersionString();
            } else if (local.equals("vendor")) {
                return Version.getProductTitle();
            } else if (local.equals("vendor-url")) {
                return Version.getWebSiteAddress();
            } else if (local.equals("product-name")) {
                return Version.getProductName();
            } else if (local.equals("product-version")) {
                return Version.getProductVariantAndVersion(config);
            } else if (local.equals("supports-serialization")) {
                return "yes";
            } else if (local.equals("supports-backwards-compatibility")) {
                return "yes";
            } else if (local.equals("supports-namespace-axis")) {  // Erratum E14
                return "yes";
            }
            return "";

        } else if (uri.length() == 0 && config.isAllowExternalFunctions()) {
	        String val = System.getProperty(local);
	        return val==null ? "" : val;
	    } else {
	    	return "";
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
