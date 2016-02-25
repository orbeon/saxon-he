////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.functions.extfn.EXPathArchive.Archive;
import com.saxonica.functions.extfn.EXPathBinary;
import com.saxonica.functions.extfn.EXPathFile;
import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.RetainedStaticContext;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;


/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends SystemFunction implements Callable {

    /**
     * Evaluate the function call
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {

        StringValue name = (StringValue) arguments[0].head();
        try {
            boolean is30 = getRetainedStaticContext().getXPathVersion() >= 30;
            StructuredQName qName = StructuredQName.fromLexicalQName(name.getStringValue(),
                    false, is30,
                    getRetainedStaticContext());

            return new StringValue(getProperty(
                        qName.getURI(), qName.getLocalPart(), getRetainedStaticContext()));

        } catch (XPathException err) {
            throw new XPathException("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
        }
    }

    /**
     * Here's the real code:
     *
     * @param uri         the namespace URI of the system property name
     * @param local       the local part of the system property name
     * @param rsc         context information
     * @return the value of the corresponding system property
     */

    public static String getProperty(String uri, String local, RetainedStaticContext rsc) {
        Configuration config = rsc.getConfiguration();
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (local.equals("version")) {
                return rsc.getXPathVersion() >= 30 ? "3.0" : "2.0";
            } else if (local.equals("vendor")) {
                return Version.getProductVendor();
            } else if (local.equals("vendor-url")) {
                return Version.getWebSiteAddress();
            } else if (local.equals("product-name")) {
                return Version.getProductName();
            } else if (local.equals("product-version")) {
                return Version.getProductVariantAndVersion(rsc.getConfiguration());
            } else if (local.equals("is-schema-aware")) {
                boolean schemaAware = rsc.getPackageData().isSchemaAware();
                return schemaAware ? "yes" : "no";
            } else if (local.equals("supports-serialization")) {
                return "yes";
            } else if (local.equals("supports-backwards-compatibility")) {
                return "yes";
            } else if (local.equals("supports-namespace-axis")) {
                return "yes";
            } else if (local.equals("supports-streaming")) {
                return rsc.getXPathVersion() >= 30 &&
                        config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT) &&
                        !config.getConfigurationProperty(FeatureKeys.STREAMABILITY).equals("off") ? "yes" : "no";
            } else if (local.equals("supports-dynamic-evaluation")) {
                return rsc.getXPathVersion() >= 30 &&
                        config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION) ? "yes" : "no";
            } else if (local.equals("supports-higher-order-functions")) {
                return rsc.getXPathVersion() >= 30 &&
                    config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION) ? "yes" : "no";
            } else if (local.equals("xpath-version")) {
                String v = rsc.getXPathVersion() + "";
                return v.charAt(0) + "." + v.charAt(1);
            } else if (local.equals("xsd-version")) {
                return rsc.getConfiguration().getXsdVersion() == Configuration.XSD10 ? "1.0" : "1.1";
            }
            return "";

        } else if (uri.isEmpty() && config.getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            String val = System.getProperty(local);
            return val == null ? "" : val;
//#if EE==true || PE==true
        } else if (uri.equals(EXPathBinary.NAMESPACE) && local.equals("version")) {
            return Double.toString(EXPathBinary.VERSION);
        } else if (uri.equals(EXPathFile.NAMESPACE) && local.equals("version")) {
            return EXPathFile.VERSION.toString();
        } else if (uri.equals(Archive.NAMESPACE) && local.equals("version")) {
            return Archive.VERSION.toString();
//#endif
        } else {
            return "";
        }
    }

}

