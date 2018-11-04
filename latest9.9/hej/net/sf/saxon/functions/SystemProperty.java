////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
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
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RetainedStaticContext;
import net.sf.saxon.lib.Feature;
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
     * Allow the function to create an optimized call based on the values of the actual arguments
     *
     * @param visitor     the expression visitor
     * @param contextInfo information about the context item
     * @param arguments   the supplied arguments to the function call. Note: modifying the contents
     *                    of this array should not be attempted, it is likely to have no effect.
     * @return either a function call on this function, or an expression that delivers
     * the same result, or null indicating that no optimization has taken place
     * @throws XPathException if an error is detected
     */
    @Override
    public Expression makeOptimizedFunctionCall(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, Expression... arguments) throws XPathException {
        if (arguments[0] instanceof Literal) {
            try {
                StringValue name = (StringValue) ((Literal) arguments[0]).getValue();
                StructuredQName qName = StructuredQName.fromLexicalQName(name.getStringValue(),
                                                                         false, true,
                                                                         getRetainedStaticContext());
                String uri = qName.getURI();
                String local = qName.getLocalPart();
                if (uri.equals(NamespaceConstant.XSLT) &&
                        (local.equals("version") || local.equals("vendor") ||
                                local.equals("vendor-url") || local.equals("product-name") ||
                                local.equals("product-version") || local.equals("supports-backwards-compatibility") ||
                                local.equals("xpath-version") || local.equals("xsd-version"))) {
                    String result = getProperty(uri, local, getRetainedStaticContext());
                    return new StringLiteral(result);
                }
            } catch (XPathException e) {
                // no action
            }
        }
        return null;
    }

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
            StructuredQName qName = StructuredQName.fromLexicalQName(name.getStringValue(),
                    false, true,
                    getRetainedStaticContext());

            return new StringValue(getProperty(
                        qName.getURI(), qName.getLocalPart(), getRetainedStaticContext()));

        } catch (XPathException err) {
            throw new XPathException("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
        }
    }

    private boolean allowsEarlyEvaluation(Sequence[] arguments, XPathContext context) throws XPathException {
        StringValue name = (StringValue) arguments[0].head();
        try {
            StructuredQName qName = StructuredQName.fromLexicalQName(name.getStringValue(),
                                                                     false, true,
                                                                     getRetainedStaticContext());
            String uri = qName.getURI();
            String local = qName.getLocalPart();
            return uri.equals(NamespaceConstant.XSLT) &&
                    (local.equals("version") || local.equals("vendor") ||
                            local.equals("vendor-url") || local.equals("product-name") ||
                            local.equals("product-version") || local.equals("supports-backwards-compatibility") ||
                            local.equals("xpath-version") || local.equals("xsd-version"));

        } catch (XPathException err) {
            throw new XPathException("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
        }

    }

    public static String yesOrNo(boolean whatever) {
        return whatever ? "yes" : "no";
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
        String edition = rsc.getPackageData().getTargetEdition();
        if (uri.equals(NamespaceConstant.XSLT)) {
            switch (local) {
                case "version":
                    return "3.0";
                case "vendor":
                    return Version.getProductVendor();
                case "vendor-url":
                    return Version.getWebSiteAddress();
                case "product-name":
                    return Version.getProductName();
                case "product-version":
                    return Version.getProductVariantAndVersion(edition);
                case "is-schema-aware":
                    boolean schemaAware = rsc.getPackageData().isSchemaAware();
                    return yesOrNo(schemaAware);
                case "supports-serialization":
                    return yesOrNo(!"JS".equals(edition));
                case "supports-backwards-compatibility":
                    return "yes";
                case "supports-namespace-axis":
                    return "yes";
                case "supports-streaming":
                    return yesOrNo(
                            "EE".equals(edition) &&
                                    config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT) &&
                                    !config.getConfigurationProperty(Feature.STREAMABILITY).equals("off"));
                case "supports-dynamic-evaluation":
                    return yesOrNo(
                            !"JS".equals(edition) &&
                                    config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION) &&
                                    !config.getBooleanProperty(Feature.DISABLE_XSL_EVALUATE));
                case "supports-higher-order-functions":
                    return yesOrNo(
                            !"JS".equals(edition) &&
                                    !"HE".equals(edition) &&
                                    config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION));
                case "xpath-version":
                    return "3.1";
                case "xsd-version":
                    return rsc.getConfiguration().getXsdVersion() == Configuration.XSD10 ? "1.0" : "1.1";
            }
            return "";

        } else if (uri.isEmpty() && config.getBooleanProperty(Feature.ALLOW_EXTERNAL_FUNCTIONS)) {
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

