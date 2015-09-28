////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
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
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.style.UseWhenStaticContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.StringValue;


/**
 * Implementation of the XSLT system-property() function
 */

public class SystemProperty extends SystemFunctionCall implements Callable {

    private NamespaceResolver nsContext;
    private StructuredQName propertyName;
    private transient boolean checked = false;
    private boolean isSchemaAware = false;
    // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        if (checked) {
            return;
        }
        checked = true;
        super.checkArguments(visitor);
        if (argument[0] instanceof StringLiteral) {
            try {
                boolean is30 = visitor.getStaticContext().getXPathLanguageLevel().equals(DecimalValue.THREE);
                propertyName = StructuredQName.fromLexicalQName(
                        ((StringLiteral) argument[0]).getStringValue(),
                        false, is30,
                        visitor.getStaticContext().getNamespaceResolver());
            } catch (XPathException e) {
                String code = e.getErrorCodeLocalPart();
                if (code == null || code.equals("FOCA0002") || code.equals("FONS0004")) {
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
     *
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (propertyName != null && propertyName.hasURI(NamespaceConstant.XSLT)) {
            if (propertyName.getLocalPart().equals("is-schema-aware")) {
                StaticContext env = visitor.getStaticContext();
                boolean aware;
                if (env instanceof UseWhenStaticContext) {
                    Configuration config = env.getConfiguration();
                    aware = "EE".equals(config.getEditionCode()) &&
                            config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
                } else {
                    aware = env.isSchemaAware();
                }
                return new StringLiteral(aware ? "yes" : "no", getContainer());
            }
            return new StringLiteral(
                    getProperty(NamespaceConstant.XSLT, propertyName.getLocalPart(), getContainer().getPackageData()),
                    getContainer());
        } else {
            return this;
        }
    }

    /**
     * Evaluate the function at run-time
     */

    /*@Nullable*/
    public StringValue evaluateItem(XPathContext context) throws XPathException {

        StructuredQName qName = propertyName;
        if (qName == null) {
            CharSequence name = argument[0].evaluateItem(context).getStringValueCS();
            try {
                boolean is30 = context.getController().getExecutable().isAllowXPath30();
                qName = StructuredQName.fromLexicalQName(name,
                        false, is30,
                        nsContext);
                if (qName.hasURI(NamespaceConstant.XSLT) &&
                        "is-schema-aware".equals(qName.getLocalPart())) {
                    return new StringValue(isSchemaAware ? "yes" : "no");
                }
            } catch (XPathException err) {
                dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
                return null;
            }
        }
        return new StringValue(getProperty(
                qName.getURI(), qName.getLocalPart(), getContainer().getPackageData()));
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        if (propertyName != null) {
            return evaluateItem(context);
        } else {
            StringValue name = (StringValue) arguments[0].head();
            try {
                boolean is30 = context.getController().getExecutable().isAllowXPath30();
                StructuredQName qName = StructuredQName.fromLexicalQName(name.getStringValue(),
                        false, is30,
                        nsContext);
                if (qName.hasURI(NamespaceConstant.XSLT) &&
                        "is-schema-aware".equals(qName.getLocalPart())) {
                    return new StringValue(isSchemaAware ? "yes" : "no");
                } else {
                    return new StringValue(getProperty(
                            qName.getURI(), qName.getLocalPart(), getContainer().getPackageData()));
                }
            } catch (XPathException err) {
                dynamicError("Invalid system property name. " + err.getMessage(), "XTDE1390", context);
                return null;
            }
        }
    }

    /**
     * Here's the real code:
     *
     * @param uri         the namespace URI of the system property name
     * @param local       the local part of the system property name
     * @param packageData the containing package (always an XSLT stylesheet)
     * @return the value of the corresponding system property
     */

    public static String getProperty(String uri, String local, PackageData packageData) {
        Configuration config = packageData.getConfiguration();
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (local.equals("version")) {
                return packageData.isAllowXPath30() ? "3.0" : "2.0";
                //return Version.getXSLVersionString();
            } else if (local.equals("vendor")) {
                return Version.getProductVendor();
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

        } else if (uri.length() == 0 && config.getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
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

