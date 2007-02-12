package net.sf.saxon.functions;
import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;


public class SystemProperty extends SystemFunction implements XSLTFunction {

    private NamespaceResolver nsContext;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        if (!(argument[0] instanceof StringLiteral)) {
            // we need to save the namespace context
            nsContext = env.getNamespaceResolver();
        }
    }

    /**
    * preEvaluate: this method performs compile-time evaluation
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        String name = ((StringLiteral)argument[0]).getStringValue().trim();

        try {
            String[] parts = env.getConfiguration().getNameChecker().getQNameParts(name);
            String prefix = parts[0];
            String lname = parts[1];
            String uri;
            if (prefix.equals("")) {
                uri = "";
            } else {
                try {
                    uri = env.getURIForPrefix(prefix);
                } catch (XPathException e) {
                    e.setErrorCode("XTDE1390");
                    throw e;
                }
            }
            return new StringLiteral(getProperty(uri, lname, env.getConfiguration()));
        } catch (QNameException e) {
            StaticError err = new StaticError("Invalid system property name. " + e.getMessage());
            err.setErrorCode("XTDE1390");
            throw err;
        }
    }


    /**
    * Evaluate the function at run-time
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        CharSequence name = argument[0].evaluateItem(context).getStringValueCS();

        try {
            String[] parts = context.getConfiguration().getNameChecker().getQNameParts(name);
            String prefix = parts[0];
            String lname = parts[1];
            String uri;
            if (prefix.equals("")) {
                uri = "";
            } else {
                uri = nsContext.getURIForPrefix(prefix, false);
            }
            return new StringValue(getProperty(uri, lname, context.getConfiguration()));
        } catch (QNameException e) {
            dynamicError("Invalid system property name. " + e.getMessage(), "XTDE1390", context);
            return null;
        }
    }

    /**
    * Here's the real code:
    */

    public static String getProperty(String uri, String local, Configuration config) {
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (local.equals("version"))
                return Version.getXSLVersionString();
            if (local.equals("vendor"))
                return Version.getProductTitle();
            if (local.equals("vendor-url"))
                return Version.getWebSiteAddress();
            if (local.equals("product-name"))
                return Version.getProductName();
            if (local.equals("product-version"))
                return config.isSchemaAware(Configuration.XSLT) ?
                        Version.getSchemaAwareProductVersion() :
                        Version.getProductVersion();
            if (local.equals("is-schema-aware"))
                return config.isSchemaAware(Configuration.XSLT) ? "yes" : "no";
            if (local.equals("supports-serialization"))
                return "yes";
            if (local.equals("supports-backwards-compatibility"))
                return "yes";
            return "";

        } else if (uri.equals("")) {
	        String val = System.getProperty(local);
	        if (val==null) val="";
	        return val;
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
