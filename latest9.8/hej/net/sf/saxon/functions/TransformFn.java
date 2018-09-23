////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.Version;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.*;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.ArrayItemType;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.serialize.CharacterMap;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.trans.StylesheetCache;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SpecificFunctionType;
import net.sf.saxon.value.*;
import net.sf.saxon.value.SequenceType;
import org.xml.sax.InputSource;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the function transform(), which is a standard function in XPath 3.1
 */
public class TransformFn extends SystemFunction implements Callable {


    private static String[] transformOptionNames30 = new String[]{
            "package-name", "package-version", "package-node", "package-location", "static-params", "global-context-item",
            "template-params", "tunnel-params", "initial-function", "function-params"
    };

    private final static String dummyBaseOutputUriScheme = "dummy";


    private boolean isTransformOptionName30(String string) {
        for (String s : transformOptionNames30) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }


    public static OptionsParameter makeOptionsParameter() {
        OptionsParameter op = new OptionsParameter();
        op.addAllowedOption("xslt-version", SequenceType.SINGLE_DECIMAL);
        op.addAllowedOption("stylesheet-location", SequenceType.SINGLE_STRING);
        op.addAllowedOption("stylesheet-node", SequenceType.SINGLE_NODE);
        op.addAllowedOption("stylesheet-text", SequenceType.SINGLE_STRING);
        op.addAllowedOption("stylesheet-base-uri", SequenceType.SINGLE_STRING);
        op.addAllowedOption("base-output-uri", SequenceType.SINGLE_STRING);
        op.addAllowedOption("stylesheet-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("initial-match-selection", SequenceType.ANY_SEQUENCE);
        op.addAllowedOption("source-node", SequenceType.SINGLE_NODE);
        op.addAllowedOption("source-location", SequenceType.SINGLE_STRING); // Saxon extension
        op.addAllowedOption("initial-mode", SequenceType.SINGLE_QNAME);
        op.addAllowedOption("initial-template", SequenceType.SINGLE_QNAME);
        op.addAllowedOption("delivery-format", SequenceType.SINGLE_STRING);
        op.addAllowedOption("serialization-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("vendor-options", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("cache", SequenceType.SINGLE_BOOLEAN);
        op.addAllowedOption("package-name", SequenceType.SINGLE_STRING);
        op.addAllowedOption("package-version", SequenceType.SINGLE_STRING);
        op.addAllowedOption("package-node", SequenceType.SINGLE_NODE);
        op.addAllowedOption("package-location", SequenceType.SINGLE_STRING);
        op.addAllowedOption("static-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("global-context-item", SequenceType.SINGLE_ITEM);
        op.addAllowedOption("template-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("tunnel-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("initial-function", SequenceType.SINGLE_QNAME);
        op.addAllowedOption("function-params", SequenceType.makeSequenceType(ArrayItemType.ANY_ARRAY_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("requested-properties", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        op.addAllowedOption("post-process", SequenceType.makeSequenceType(
                new SpecificFunctionType(new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.ANY_SEQUENCE}, SequenceType.ANY_SEQUENCE),
                StaticProperty.EXACTLY_ONE));
                // function(xs:string, item()*) as item()*
        return op;
    }

    /**
     * Check the options supplied:
     * 1. only allow XSLT 3.0 options if using an XSLT 3.0 processor (throw an error if any are supplied and not an XSLT 3.0 processor);
     * 2. ignore any other options not in the specs;
     * 3. validate the types of the option values supplied.
     * This method is called AFTER doing the standard type-checking on options parameters; on entry the map will only contain
     * options with recognized names and type-valid values.
     */

    private void checkTransformOptions(Map<String, Sequence> options, XPathContext context, boolean isXslt30Processor) throws XPathException {
        if (options.size() == 0) {
            throw new XPathException("No transformation options supplied", "FOXT0002");
        }

        for (String keyName : options.keySet()) {
            if (isTransformOptionName30(keyName) && !isXslt30Processor) {
                throw new XPathException("The transform option " + keyName + " is only available when using an XSLT 3.0 processor", "FOXT0002");
            }
        }
    }

    private String checkStylesheetMutualExclusion(Map<String, Sequence> map) throws XPathException {
        return exactlyOneOf(map, "stylesheet-location", "stylesheet-node", "stylesheet-text");
    }

    private String checkStylesheetMutualExclusion30(Map<String, Sequence> map) throws XPathException {
        String styleOption = exactlyOneOf(map, "stylesheet-location", "stylesheet-node", "stylesheet-text",
                                          "package-name", "package-node", "package-location");
        if (styleOption.equals("package-location")) {
            throw new XPathException("The transform option " + styleOption + " is not implemented in Saxon", "FOXT0002");
        }
        return styleOption;
    }

    private String checkInvocationMutualExclusion(Map<String, Sequence> options) throws XPathException {
        return oneOf(options, "initial-mode", "initial-template");
    }

    /**
     * Check that at most one of a set of keys is present in the map, and return the one
     * that is present.
     * @param map the map to be searched
     * @param keys the keys to look for
     * @return if one of the keys is present, return that key; otherwise return null
     * @throws XPathException if more than one of the keys is present
     */

    private String oneOf(Map<String, Sequence> map, String... keys) throws XPathException {
        String found = null;
        for (String s : keys) {
            if (map.get(s) != null) {
                if (found != null) {
                    throw new XPathException(
                            "The following transform options are mutually exclusive: " + enumerate(keys), "FOXT0002");
                } else {
                    found = s;
                }
            }
        }
        return found;
    }

    /**
     * Check that exactly one of a set of keys is present in the map, and return the one
     * that is present.
     *
     * @param map  the map to be searched
     * @param keys the keys to look for
     * @return if exactly one of the keys is present, return that key
     * @throws XPathException if none of the keys is present or if more than one of the keys is present
     */

    private String exactlyOneOf(Map<String, Sequence> map, String... keys) throws XPathException {
        String found = oneOf(map, keys);
        if (found == null) {
            throw new XPathException("One of the following transform options must be present: " + enumerate(keys));
        }
        return found;
    }

    private String enumerate(String... keys) {
        boolean first = true;
        FastStringBuffer buffer = new FastStringBuffer(256);
        for (String k : keys) {
            if (first) {
                first = false;
            } else {
                buffer.append(" | ");
            }
            buffer.append(k);
        }
        return buffer.toString();
    }

    private String checkInvocationMutualExclusion30(Map<String, Sequence> map) throws XPathException {
        return oneOf(map, "initial-mode", "initial-template", "initial-function");
    }

    private void unsuitable(String option, String value) throws XPathException {
        throw new XPathException("No XSLT processor is available with xsl:" + option + " = " + value, "FOXT0001");
    }

    private boolean asBoolean(AtomicValue value) throws XPathException {
        if (value instanceof BooleanValue) {
            return ((BooleanValue)value).getBooleanValue();
        } else if (value instanceof StringValue) {
            String s = Whitespace.normalizeWhitespace(value.getStringValue()).toString();
            if (s.equals("yes") || s.equals("true") || s.equals("1")) {
                return true;
            } else if (s.equals("no") || s.equals("false") || s.equals("0")) {
                return false;
            }
        }
        throw new XPathException("Unrecognized boolean value " + value.toString(), "FOXT0002");
    }

    private void setRequestedProperties(Map<String, Sequence> options, Processor processor) throws XPathException {
        MapItem requestedProps = (MapItem) options.get("requested-properties").head();
        AtomicIterator optionIterator = requestedProps.keys();
        while (true) {
            AtomicValue option = optionIterator.next();
            if (option != null) {
                StructuredQName optionName = ((QNameValue) option.head()).getStructuredQName();
                AtomicValue value = (AtomicValue)requestedProps.get(option).head();
                if (optionName.hasURI(NamespaceConstant.XSLT)) {
                    String localName = optionName.getLocalPart();
                    if (localName.equals("vendor-url")) {
                        if (!value.getStringValue().equals("Saxonica")) {
                            unsuitable("vendor-url", value.getStringValue());
                        }
                    } else if (localName.equals("vendor-url")) {
                        if (!value.getStringValue().contains("saxonica.com")) {
                            unsuitable("vendor-url", value.getStringValue());
                        }
                    } else if (localName.equals("product-name")) {
                        if (!value.getStringValue().equals("SAXON")) {
                            unsuitable("vendor-url", value.getStringValue());
                        }
                    } else if (localName.equals("product-version")) {
                        if (!Version.getProductVersion().startsWith(value.getStringValue())) {
                            unsuitable("product-version", value.getStringValue());
                        }
                    } else if (localName.equals("is-schema-aware")) {
                        boolean b = asBoolean(value);
                        if (b) {
                            if (processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
                                processor.setConfigurationProperty(FeatureKeys.XSLT_SCHEMA_AWARE, true);
                            } else {
                                unsuitable("is-schema-aware", value.getStringValue());
                            }
                        } else {
                            if (processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
                                unsuitable("is-schema-aware", value.getStringValue());
                            }
                        }
                    } else if (localName.equals("supports-serialization")) {
                        boolean b = asBoolean(value);
                        if (!b) {
                            unsuitable("supports-serialization", value.getStringValue());
                        }
                    } else if (localName.equals("supports-backwards-compatibility")) {
                        boolean b = asBoolean(value);
                        if (!b) {
                            unsuitable("supports-backwards-compatibility", value.getStringValue());
                        }
                    } else if (localName.equals("supports-namespace-axis")) {
                        boolean b = asBoolean(value);
                        if (!b) {
                            unsuitable("supports-namespace-axis", value.getStringValue());
                        }
                    } else if (localName.equals("supports-streaming")) {
                        boolean b = asBoolean(value);
                        if (b) {
                            if (!processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
                                unsuitable("supports-streaming", value.getStringValue());
                            }
                        } else {
                            if (processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
                                processor.setConfigurationProperty(FeatureKeys.STREAMABILITY, "off");
                            }
                        }
                    } else if (localName.equals("supports-dynamic-evaluation")) {
                        boolean b = asBoolean(value);
                        if (b) {
                            if (!processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                                unsuitable("supports-dynamic-evaluation", value.getStringValue());
                            }
                        } else {
                            if (processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                                processor.setConfigurationProperty(FeatureKeys.DISABLE_XSL_EVALUATE, true);
                            }
                        }
                    } else if (localName.equals("supports-higher-order-functions")) {
                        boolean b = asBoolean(value);
                        if (b != processor.getUnderlyingConfiguration().isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                            unsuitable("supports-higher-order-functions", value.getStringValue());
                        }
                    } else if (localName.equals("xpath-version")) {
                        String v = value.getStringValue();
                        try {
                            if (Double.parseDouble(v) > 3.1) {
                                unsuitable("xpath-version", value.getStringValue());
                            }
                        } catch (NumberFormatException nfe) {
                            unsuitable("xpath-version", value.getStringValue());
                        }
                    } else if (localName.equals("xsd-version")) {
                        String v = value.getStringValue();
                        try {
                            if (Double.parseDouble(v) > 1.1) {
                                unsuitable("xsd-version", value.getStringValue());
                            }
                        } catch (NumberFormatException nfe) {
                            unsuitable("xsd-version", value.getStringValue());
                        }
                    }
                }
            } else {
                break;
            }
        }
    }

    private void setStaticParams(Map<String, Sequence> options, XsltCompiler xsltCompiler, boolean allowTypedNodes) throws XPathException {
        MapItem staticParamsMap = (MapItem) options.get("static-params").head();
        AtomicIterator paramIterator = staticParamsMap.keys();
        while (true) {
            AtomicValue param = paramIterator.next();
            if (param != null) {
                if (!(param instanceof QNameValue)) {
                    throw new XPathException("Parameter names in static-params must be supplied as QNames", "FOXT0002");
                }
                QName paramName = new QName(((QNameValue) param).getStructuredQName());
                Sequence value = staticParamsMap.get(param);
                if (!allowTypedNodes) {
                    checkSequenceIsUntyped(value);
                }
                XdmValue paramVal = XdmValue.wrap(value);
                xsltCompiler.setParameter(paramName, paramVal);
            } else {
                break;
            }
        }
    }

    private XsltExecutable getStylesheet(Map<String, Sequence> options, XsltCompiler xsltCompiler, String styleOptionStr, XPathContext context) throws XPathException {
        Item styleOptionItem = options.get(styleOptionStr).head();
        URI styleBaseUri = null;
        Sequence seq;
        if ((seq = options.get("stylesheet-base-uri")) != null) {
            StringValue styleBase = (StringValue) seq.head();
            styleBaseUri = URI.create(styleBase.getStringValue());
            if (!styleBaseUri.isAbsolute()) {
                URI staticBase = getRetainedStaticContext().getStaticBaseUri();
                styleBaseUri = staticBase.resolve(styleBaseUri);
            }
        }
        final List<TransformerException> compileErrors = new ArrayList<TransformerException>();
        final ErrorListener originalListener = xsltCompiler.getErrorListener();
        xsltCompiler.setErrorListener(new ErrorListener() {
            @Override
            public void warning(TransformerException exception) throws TransformerException {
                originalListener.warning(exception);
            }

            @Override
            public void error(TransformerException exception) throws TransformerException {
                compileErrors.add(exception);
                originalListener.error(exception);
            }

            @Override
            public void fatalError(TransformerException exception) throws TransformerException {
                compileErrors.add(exception);
                originalListener.fatalError(exception);
            }
        });
        boolean cacheable = options.get("static-params") == null;
        if (options.get("cache") != null) {
            cacheable &= ((BooleanValue) options.get("cache").head()).getBooleanValue();
        }

        StylesheetCache cache = context.getController().getStylesheetCache();
        XsltExecutable executable = null;
        if (styleOptionStr.equals("stylesheet-location")) {
            String stylesheetLocation = styleOptionItem.getStringValue();
            if (cacheable) {
                executable = cache.getStylesheetByLocation(stylesheetLocation); // if stylesheet is already cached
            }
            if (executable == null) {
                Source style;
                try {
                    String base = getStaticBaseUriString();
                    style = xsltCompiler.getURIResolver().resolve(stylesheetLocation, base);
                    // returns null when stylesheetLocation is relative, and (QT3TestDriver) TestURIResolver
                    // is wrongly being used for URIResolver. Next step directs to correct URIResolver.
                    if (style == null) {
                        style = xsltCompiler.getProcessor().getUnderlyingConfiguration().getSystemURIResolver().resolve(stylesheetLocation, base);
                    }
//                    if (styleBaseUri != null) {
//                        style.setSystemId(styleBaseUri.getStringValue());
//                    }
                } catch (TransformerException e) {
                    throw new XPathException(e);
                }

                try {
                    executable = xsltCompiler.compile(style);
                } catch (SaxonApiException e) {
                    return reportCompileError(e, compileErrors);
                }
                if (cacheable) {
                    cache.setStylesheetByLocation(stylesheetLocation, executable);
                }
            }
        } else if (styleOptionStr.equals("stylesheet-node") || styleOptionStr.equals("package-node")) {
            NodeInfo stylesheetNode = (NodeInfo) styleOptionItem;

            if (styleBaseUri != null && !stylesheetNode.getBaseURI().equals(styleBaseUri.toASCIIString())) {

                // If the stylesheet is supplied as a node, and the stylesheet-base-uri option is supplied, and doesn't match
                // the base URIs of the nodes (tests fn-transform-19 and fn-transform-41), then we have a bit of a problem.
                // So copy the stylesheet to a new tree having the desired base URI.

                final String sysId = styleBaseUri.toASCIIString();
                Builder builder = context.getController().makeBuilder();
                builder.setSystemId(sysId);
                //builder.freezeSystemIdAndBaseURI();
                final ExplicitLocation fixedLocation = new ExplicitLocation(sysId, -1, -1);
                ProxyReceiver filter = new ProxyReceiver(builder) {
                    @Override
                    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
                        super.startElement(elemName, typeCode, fixedLocation, properties);
                    }

                    @Override
                    public void setSystemId(String systemId) {
                        super.setSystemId(sysId);
                    }
                };
                builder.open();
                stylesheetNode.copy(filter, 0, ExplicitLocation.UNKNOWN_LOCATION);
                builder.close();
                stylesheetNode = builder.getCurrentRoot();
            }

            if (cacheable) {
                executable = cache.getStylesheetByNode(stylesheetNode); // if stylesheet is already cached
            }
            if (executable == null) {
                Source source = stylesheetNode;
                if (styleBaseUri != null) {
                    source = AugmentedSource.makeAugmentedSource(source);
                    source.setSystemId(styleBaseUri.toASCIIString());
                }
                try {
                    executable = xsltCompiler.compile(source);
                } catch (SaxonApiException e) {
                    reportCompileError(e, compileErrors);
                }
                if (cacheable) {
                    cache.setStylesheetByNode(stylesheetNode, executable);
                }
            }
        } else if (styleOptionStr.equals("stylesheet-text")) {
            String stylesheetText = styleOptionItem.getStringValue();
            if (cacheable) {
                executable = cache.getStylesheetByText(stylesheetText); // if stylesheet is already cached
            }
            if (executable == null) {
                StringReader sr = new StringReader(stylesheetText);
                SAXSource style = new SAXSource(new InputSource(sr));
                if (styleBaseUri != null) {
                    style.setSystemId(styleBaseUri.toASCIIString());
                }
                try {
                    executable = xsltCompiler.compile(style);
                } catch (SaxonApiException e) {
                    reportCompileError(e, compileErrors);
                }
                if (cacheable) {
                    cache.setStylesheetByText(stylesheetText, executable);
                }
            }
        } else if (styleOptionStr.equals("package-name")) {
            String packageName = Whitespace.trim(styleOptionItem.getStringValue());
            String packageVersion = null;
            if (options.get("package-version") != null) {
                packageVersion = options.get("package-version").head().getStringValue();
            }
            try {
                XsltPackage pack = xsltCompiler.obtainPackage(packageName, packageVersion);
                if (pack == null) {
                    throw new XPathException("Cannot locate package " + packageName + " version " + packageVersion, "FOXT0002");
                }
                executable = pack.link(); // load the already compiled package
            } catch (SaxonApiException e) {
                if (e.getCause() instanceof XPathException) {
                    throw (XPathException) e.getCause();
                } else {
                    throw new XPathException(e);
                }
            }
        }
        return executable;
    }

    private XsltExecutable reportCompileError(SaxonApiException e, List<TransformerException> compileErrors) throws XPathException {
        for (TransformerException te : compileErrors) {
            // This is primarily so that the right error code is reported as required by the fn:transform spec
            if (te instanceof XPathException) {
                throw (XPathException)te;
            }
        }
        if (e.getCause() instanceof XPathException) {
            throw (XPathException) e.getCause();
        } else {
            throw new XPathException(e);
        }
    }


    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        Map<String, Sequence> options = getDetails().optionDetails.processSuppliedOptions((MapItem) arguments[0].head(), context);

        Sequence vendorOptionsValue = options.get("vendor-options");
        MapItem vendorOptions = vendorOptionsValue == null ? null : (MapItem) vendorOptionsValue.head();

        boolean allowTypedNodes = true;
        Configuration targetConfig = context.getConfiguration();
        if (vendorOptions != null) {
            Sequence targetConfigValue = vendorOptions.get(new QNameValue("", NamespaceConstant.SAXON, "configuration"));
            if (targetConfigValue != null) {
                NodeInfo configFile = (NodeInfo) targetConfigValue.head();
                targetConfig = Configuration.readConfiguration(configFile, targetConfig);
                targetConfig.importLicenseDetails(context.getConfiguration());
                if (!context.getConfiguration().getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
                    targetConfig.setBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, false);
                }
                allowTypedNodes = false;
            }
        }
        Processor processor = new Processor(true);
        processor.setConfigurationProperty(FeatureKeys.CONFIGURATION, targetConfig);
        boolean isXslt30Processor = true;
        checkTransformOptions(options, context, isXslt30Processor);
        boolean useXslt30Processor = isXslt30Processor;
        if (options.get("xslt-version") != null) {
            BigDecimalValue xsltVersion = (BigDecimalValue) options.get("xslt-version").head();
            if ((xsltVersion.compareTo(BigDecimalValue.THREE) >= 0 && !isXslt30Processor) || (xsltVersion.compareTo(BigDecimalValue.THREE) > 0 && isXslt30Processor)) {
                throw new XPathException("The transform option xslt-version is higher than the XSLT version supported by this processor", "FOXT0002");
            }
            useXslt30Processor = xsltVersion.compareTo(BigDecimalValue.THREE) == 0;
        }

        String principalInput = oneOf(options, "source-node", "source-location", "initial-match-selection");

        // Check the rules and restrictions for combinations of transform options
        String invocationOption;
        String invocationName = "invocation";
        String styleOption;

        invocationOption = checkInvocationMutualExclusion30(options);
        // if invocation option is not initial-function or initial-template then check for source-node
        if (invocationOption != null) {
            invocationName = invocationOption;
        }
        if (!invocationName.equals("initial-template") && !invocationName.equals("initial-function") && principalInput == null) {
            //throw new XPathException("A transform must have at least one of the following options: source-node|initial-template|initial-function", "FOXT0002");
            invocationName = "initial-template";
            options.put("initial-template", new QNameValue("", NamespaceConstant.XSLT, "initial-template"));
        }
        // if invocation option is initial-function, then check for function-params
        if (invocationName.equals("initial-function") && options.get("function-params") == null) {
            throw new XPathException("Use of the transform option initial-function requires the function parameters to be supplied using the option function-params", "FOXT0002");
        }
        // function-params should only be used if invocation option is initial-function
        if (!invocationName.equals("initial-function") && options.get("function-params") != null) {
            throw new XPathException("The transform option function-params can only be used if the option initial-function is also used", "FOXT0002");
        }
        styleOption = checkStylesheetMutualExclusion30(options);








        // Set the vendor options (configuration features) on the processor
        if (options.get("requested-properties") != null) {
            setRequestedProperties(options, processor);
        }
//        if (options.get("vendor-options") != null) {
//            setVendorOptions(options, processor);
//        }

        XsltCompiler xsltCompiler = processor.newXsltCompiler();
        xsltCompiler.setURIResolver(context.getURIResolver());
        xsltCompiler.setJustInTimeCompilation(false);

        // Set static params on XsltCompiler before compiling stylesheet (XSLT 3.0 processing only)
        if (options.get("static-params") != null) {
            setStaticParams(options, xsltCompiler, allowTypedNodes);
        }

        XsltExecutable sheet = getStylesheet(options, xsltCompiler, styleOption, context);
        Xslt30Transformer transformer = sheet.load30();

        //Destination destination = new XdmDestination();
        String deliveryFormat = "document";
        NodeInfo sourceNode = null;
        String sourceLocation = null;
        QName initialTemplate = null;
        QName initialMode = null;
        String baseOutputUri = null;
        Map<QName, XdmValue> stylesheetParams = new HashMap<QName, XdmValue>();
        MapItem serializationParamsMap = null;
        StringWriter serializedResult = null;
        File serializedResultFile = null;
        XdmItem globalContextItem = null;
        XdmValue initialMatchSelection = null;
        Map<QName, XdmValue> templateParams = new HashMap<QName, XdmValue>();
        Map<QName, XdmValue> tunnelParams = new HashMap<QName, XdmValue>();
        QName initialFunction = null;
        XdmValue[] functionParams = null;
        Function postProcessor = null;
        String principalResultKey = "output";
        int schemaValidation = Validation.DEFAULT;

        for (String name : options.keySet()) {
            Sequence value = options.get(name);
            Item head = value.head();
            if (name.equals("source-node")) {
                sourceNode = (NodeInfo) head;
                if (!allowTypedNodes) {
                    checkSequenceIsUntyped(sourceNode);
                }
            } else if (name.equals("source-location")) {
                sourceLocation = head.getStringValue();
            } else if (name.equals("initial-template")) {
                initialTemplate = new QName(((QNameValue) head).getStructuredQName());
            } else if (name.equals("initial-mode")) {
                initialMode = new QName(((QNameValue) head).getStructuredQName());
            } else if (name.equals("initial-match-selection")) {
                initialMatchSelection = XdmValue.wrap(value);
                if (!allowTypedNodes) {
                    checkSequenceIsUntyped(value);
                }
            } else if (name.equals("delivery-format")) {
                deliveryFormat = head.getStringValue();
                if (!deliveryFormat.equals("document") && !deliveryFormat.equals("serialized") && !deliveryFormat.equals("raw")) {
                    throw new XPathException("The transform option delivery-format should be one of: document|serialized|raw ", "FOXT0002");
                }
            } else if (name.equals("base-output-uri")) {
                baseOutputUri = head.getStringValue();
                principalResultKey = baseOutputUri;

            } else if (name.equals("serialization-params")) {
                serializationParamsMap = (MapItem) head;

            } else if (name.equals("stylesheet-params")) {
                MapItem params = (MapItem) head;
                processParams(params, stylesheetParams, allowTypedNodes);
            } else if (name.equals("global-context-item")) {
                if (useXslt30Processor) {
                    globalContextItem = (XdmItem)XdmValue.wrap(head);
                    if (!allowTypedNodes && globalContextItem instanceof NodeInfo && ((NodeInfo) globalContextItem).getTreeInfo().isTyped()) {
                        throw new XPathException("Schema-validated nodes cannot be passed to fn:transform() when it runs under a different Saxon Configuration", "FOXT0002");
                    }
                }
            } else if (name.equals("template-params")) {
                MapItem params = (MapItem) head;
                processParams(params, templateParams, allowTypedNodes);
            } else if (name.equals("tunnel-params")) {
                MapItem params = (MapItem) head;
                processParams(params, tunnelParams, allowTypedNodes);
            } else if (name.equals("initial-function")) {
                initialFunction = new QName(((QNameValue) head).getStructuredQName());
            } else if (name.equals("function-params")) {
                ArrayItem functionParamsArray = (ArrayItem) head;
                functionParams = new XdmValue[functionParamsArray.arrayLength()];
                for (int i = 0; i < functionParams.length; i++) {
                    Sequence argVal = functionParamsArray.get(i);
                    if (!allowTypedNodes) {
                        checkSequenceIsUntyped(argVal);
                    }
                    functionParams[i] = XdmValue.wrap(argVal);
                }
            } else if (name.equals("post-process")) {
                postProcessor = (Function)head;
            }
        }

        if (baseOutputUri == null) {
            baseOutputUri = getStaticBaseUriString();
        } else {
            try {
                URI base = new URI(baseOutputUri);
                if (!base.isAbsolute()) {
                    base = getRetainedStaticContext().getStaticBaseUri().resolve(baseOutputUri);
                    baseOutputUri = base.toASCIIString();
                }
            } catch (URISyntaxException err) {
                throw new XPathException("Invalid base output URI " + baseOutputUri, "FOXT0002");
            }
        }

        Deliverer deliverer = Deliverer.makeDeliverer(deliveryFormat);
        deliverer.setTransformer(transformer);
        deliverer.setBaseOutputUri(baseOutputUri);
        deliverer.setPrincipalResultKey(principalResultKey);
        deliverer.setPostProcessor(postProcessor, context);

        Controller controller = transformer.getUnderlyingController();
        controller.setOutputURIResolver(deliverer);

        Destination destination = deliverer.getPrimaryDestination(serializationParamsMap);
        Sequence result;
        try {
            transformer.setStylesheetParameters(stylesheetParams);
            transformer.setBaseOutputURI(baseOutputUri);
            transformer.setInitialTemplateParameters(templateParams, false);
            transformer.setInitialTemplateParameters(tunnelParams, true);

            if (schemaValidation == Validation.STRICT || schemaValidation == Validation.LAX) {
                if (sourceNode != null) {
                    sourceNode = validate(sourceNode, targetConfig, schemaValidation);
                } else if (sourceLocation != null) {
                    StreamSource ss = new StreamSource(sourceLocation);
                    ParseOptions parseOptions = new ParseOptions(targetConfig.getParseOptions());
                    parseOptions.setSchemaValidationMode(schemaValidation);
                    TreeInfo tree = targetConfig.buildDocumentTree(ss, parseOptions);
                    sourceNode = tree.getRootNode();
                    sourceLocation = null;
                }
                if (globalContextItem instanceof XdmNode) {
                    NodeInfo v = validate(((XdmNode)globalContextItem).getUnderlyingNode(), targetConfig, schemaValidation);
                    globalContextItem = (XdmNode)XdmValue.wrap(v);
                }
            }

            if (sourceNode != null && globalContextItem == null) {
                transformer.setGlobalContextItem(new XdmNode(sourceNode.getRoot()));
            }
            if (globalContextItem != null) {
                transformer.setGlobalContextItem(globalContextItem);
            }
            if (initialTemplate != null) {
                if (deliveryFormat.equals("raw")) {
                    result = transformer.callTemplate(initialTemplate).getUnderlyingValue();
                    result = deliverer.postProcess(principalResultKey, result);
                } else {
                    transformer.callTemplate(initialTemplate, destination);
                    result = deliverer.getPrimaryResult();
                }
            } else if (initialFunction != null) {
                if (deliveryFormat.equals("raw")) {
                    result = transformer.callFunction(initialFunction, functionParams).getUnderlyingValue();
                    result = deliverer.postProcess(principalResultKey, result);
                } else {
                    transformer.callFunction(initialFunction, functionParams, destination);
                    result = deliverer.getPrimaryResult();
                }
            } else {
                if (initialMode != null) {
                    transformer.setInitialMode(initialMode);
                }
                if (initialMatchSelection == null && sourceNode != null) {
                    initialMatchSelection = XdmValue.wrap(sourceNode);
                }
                if (initialMatchSelection == null) {
                    StreamSource stream = new StreamSource(sourceLocation);
                    if (deliveryFormat.equals("raw")) {
                        result = transformer.applyTemplates(stream).getUnderlyingValue();
                        result = deliverer.postProcess(principalResultKey, result);
                    } else {
                        transformer.applyTemplates(stream, destination);
                        result = deliverer.getPrimaryResult();
                    }
                } else {
                    if (deliveryFormat.equals("raw")) {
                        result = transformer.applyTemplates(initialMatchSelection).getUnderlyingValue();
                        result = deliverer.postProcess(principalResultKey, result);
                    } else {
                        transformer.applyTemplates(initialMatchSelection, destination);
                        result = deliverer.getPrimaryResult();
                    }
                }
            }
        } catch (SaxonApiException e) {
            XPathException e2;
            if (e.getCause() instanceof XPathException) {
                e2 = (XPathException) e.getCause();
                e2.setIsGlobalError(false);
                throw e2;
            } else {
                throw new XPathException(e);
            }
        }

        // Build map of secondary results

        HashTrieMap resultMap = new HashTrieMap();
        resultMap = deliverer.populateResultMap(resultMap);

        // Add primary result

        if (result != null) {
            result = SequenceTool.toGroundedValue(result);
            AtomicValue resultKey = new StringValue(principalResultKey);
            resultMap = resultMap.addEntry(resultKey, result);
        }
        return resultMap;

    }

    /**
     * Process options such as stylesheet-params, static-params, etc
     * @param suppliedParams an XDM map from QNames to arbitrary values
     * @param checkedParams a Java map which on return will contain the same data, but as a Java map,
     *                      and using s9api representations of the parameter names and values
     * @param allowTypedNodes true if the value of the parameter is allowed to contain schema-validated
     *                        nodes
     * @throws XPathException for example if a parameter is supplied as a string rather than as a QName
     */

    private void processParams(MapItem suppliedParams, Map<QName, XdmValue> checkedParams, boolean allowTypedNodes) throws XPathException {
        AtomicIterator paramIterator = suppliedParams.keys();
        while (true) {
            AtomicValue param = paramIterator.next();
            if (param != null) {
                if (!(param instanceof QNameValue)) {
                    throw new XPathException("The names of parameters must be supplied as QNames", "FOXT0002");
                }
                QName paramName = new QName(((QNameValue) param).getStructuredQName());
                Sequence value = suppliedParams.get(param);
                if (!allowTypedNodes) {
                    checkSequenceIsUntyped(value);
                }
                XdmValue paramVal = XdmValue.wrap(value);
                checkedParams.put(paramName, paramVal);
            } else {
                break;
            }
        }
    }

    private void checkSequenceIsUntyped(Sequence value) throws XPathException {
        SequenceIterator iter = value.iterate();
        Item item;
        while ((item = iter.next()) != null) {
            if (item instanceof NodeInfo && ((NodeInfo) item).getTreeInfo().isTyped()) {
                throw new XPathException("Schema-validated nodes cannot be passed to fn:transform() when it runs under a different Saxon Configuration", "FOXT0002");
            }
        }
    }

    private static NodeInfo validate(NodeInfo node, Configuration config, int validation) throws XPathException {
        ParseOptions options = new ParseOptions(config.getParseOptions());
        options.setSchemaValidationMode(validation);
        return config.buildDocumentTree(node, options).getRootNode();
    }

    /**
     * Deliverer is an abstraction of the common functionality of the various delivery formats
     */

    private static abstract class Deliverer extends StandardOutputResolver {

        protected Xslt30Transformer transformer;
        protected String baseOutputUri;
        protected String principalResultKey;
        protected Function postProcessor;
        protected XPathContext context;

        public static Deliverer makeDeliverer(String deliveryFormat) {
            if (deliveryFormat.equals("document")) {
                return new DocumentDeliverer();
            } else if (deliveryFormat.equals("serialized")) {
                return new SerializedDeliverer();
            } else if (deliveryFormat.equals("raw")) {
                return new RawDeliverer();
            } else {
                throw new IllegalArgumentException("delivery-format");
            }
        }

        public final void setTransformer(Xslt30Transformer transformer) {
            this.transformer = transformer;
        }

        public final void setPrincipalResultKey(String key) {
            this.principalResultKey = key;
        }

        public final void setBaseOutputUri(String uri) {
            this.baseOutputUri = uri;
        }

        public void setPostProcessor(Function postProcessor, XPathContext context) {
            this.postProcessor = postProcessor;
            this.context = context;
        }

        /**
         * Return a map containing information about all the secondary result documents
         *
         * @param resultMap a map to be populated, initially empty
         * @return a map containing one entry for each secondary result document that has been written
         * @throws XPathException if a failure occurs
         */

        public abstract HashTrieMap populateResultMap(HashTrieMap resultMap) throws XPathException;

        /**
         * Get the s9api Destination object to be used for the transformation
         *
         * @param serializationParamsMap the serialization parameters requested
         * @return a suitable destination object, or null in the case of raw mode
         * @throws XPathException if a failure occurs
         */

        public abstract Destination getPrimaryDestination(MapItem serializationParamsMap) throws XPathException;

        /**
         * Common code shared by subclasses to create a serializer
         *
         * @param serializationParamsMap the serialization options
         * @return a suitable Serializer
         */

        protected Serializer makeSerializer(MapItem serializationParamsMap) throws XPathException {
            Serializer serializer = transformer.newSerializer();
            if (serializationParamsMap != null) {
                AtomicIterator paramIterator = serializationParamsMap.keys();
                AtomicValue param;
                while ((param = paramIterator.next()) != null) {
                    // See bug 29440/29443. For the time being, accept both the old and new forms of serialization params
                    QName paramName;
                    if (param instanceof StringValue) {
                        paramName = new QName(param.getStringValue());
                    } else if (param instanceof QNameValue) {
                        paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                    } else {
                        throw new XPathException("Serialization parameters must be strings or QNames", "XPTY0004");
                    }
                    String paramValue = null;
                    GroundedValue supplied = (GroundedValue)serializationParamsMap.get(param);
                    if (supplied.getLength() > 0) {
                        if (supplied.getLength() == 1) {
                            Item val = supplied.itemAt(0);
                            if (val instanceof StringValue) {
                                paramValue = val.getStringValue();
                            } else if (val instanceof BooleanValue) {
                                paramValue = ((BooleanValue) val).getBooleanValue() ? "yes" : "no";
                            } else if (val instanceof DecimalValue) {
                                paramValue = val.getStringValue();
                            } else if (val instanceof QNameValue) {
                                paramValue = ((QNameValue)val).getClarkName();
                            } else if (val instanceof MapItem && paramName.getClarkName().equals(SaxonOutputKeys.USE_CHARACTER_MAPS)) {
                                CharacterMap charMap = Serialize.toCharacterMap((MapItem)val);
                                CharacterMapIndex charMapIndex = new CharacterMapIndex();
                                charMapIndex.putCharacterMap(charMap.getName(), charMap);
                                serializer.setCharacterMap(charMapIndex);
                                String existing = serializer.getCombinedOutputProperties().getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
                                if (existing == null) {
                                    serializer.setOutputProperty(Serializer.Property.USE_CHARACTER_MAPS,
                                                                 charMap.getName().getClarkName());
                                } else {
                                    serializer.setOutputProperty(Serializer.Property.USE_CHARACTER_MAPS,
                                                                 existing + " " + charMap.getName().getClarkName());
                                }
                                continue;
                            }
                        }
                        if (paramValue == null) {
                            // if more than one, the only possibility is a sequence of QNames
                            SequenceIterator iter = supplied.iterate();
                            Item it;
                            paramValue = "";
                            while ((it = iter.next()) != null) {
                                if (it instanceof QNameValue) {
                                    paramValue += " " + ((QNameValue)it).getClarkName();
                                } else {
                                    throw new XPathException("Value of serialization parameter " + paramName.getEQName() + " not recognized", "XPTY0004");
                                }
                            }
                        }
                        Serializer.Property prop = Serializer.getProperty(paramName);
                        if (paramName.getClarkName().equals(OutputKeys.CDATA_SECTION_ELEMENTS)
                                || paramName.getClarkName().equals(SaxonOutputKeys.SUPPRESS_INDENTATION)) {
                            String existing = serializer.getCombinedOutputProperties().getProperty(paramName.getLocalName());
                            if (existing == null) {
                                serializer.setOutputProperty(prop, paramValue);
                            } else {
                                serializer.setOutputProperty(prop, existing + paramValue);
                            }
                        } else {
                            serializer.setOutputProperty(prop, paramValue);
                        }
                    }

                }
            }
            return serializer;
        }

        /**
         * Get the primary result of the transformation, that is, the value to be included in the
         * entry of the result map that describes the principal result tree
         *
         * @return the primary result, or null if there is no primary result (after post-processing if any)
         */

        public abstract Sequence getPrimaryResult() throws XPathException;

        /**
         * Post-process the result if required
         */

        public Sequence postProcess(String uri, Sequence result) throws XPathException {
            if (postProcessor != null) {
                result = postProcessor.call(context.newCleanContext(), new Sequence[]{new StringValue(uri), result});
            }
            return SequenceTool.makeRepeatable(result);
        }
    }

    /**
     * Deliverer for delivery-format="document"
     */

    private static class DocumentDeliverer extends Deliverer {
        private Map<String, TreeInfo> results = new ConcurrentHashMap<String, TreeInfo>();
        private XdmDestination destination = new XdmDestination();

        public DocumentDeliverer() {
        }

        @Override
        public Destination getPrimaryDestination(MapItem serializationParamsMap) throws XPathException {
            return destination;
        }

        @Override
        public Sequence getPrimaryResult() throws XPathException {
            XdmNode node = destination.getXdmNode();
            return node == null ? null : postProcess(baseOutputUri, node.getUnderlyingNode());
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            Controller controller = transformer.getUnderlyingController();
            Builder builder = controller.makeBuilder();
            if (absoluteURI.getScheme().equals(dummyBaseOutputUriScheme)) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            builder.setSystemId(absoluteURI.toString());
            return builder;
        }

        @Override
        public void close(Result result) throws XPathException {
            NodeInfo doc = ((Builder) result).getCurrentRoot();
            results.put(doc.getSystemId(), doc.getTreeInfo());
        }

        public HashTrieMap populateResultMap(HashTrieMap resultMap) throws XPathException {
            for (Map.Entry<String, TreeInfo> entry : results.entrySet()) {
                String uri = entry.getKey();
                resultMap = resultMap.addEntry(new StringValue(uri),
                                               postProcess(uri, entry.getValue().getRootNode()));
            }
            return resultMap;
        }
    }

    /**
     * Deliverer for delivery-format="serialized"
     */

    private static class SerializedDeliverer extends Deliverer {
        private Map<String, String> results = new ConcurrentHashMap<String, String>();
        private Map<String, StringWriter> workInProgress = new ConcurrentHashMap<String, StringWriter>();
        private StringWriter primaryWriter;

        public SerializedDeliverer() {
        }

        @Override
        public Destination getPrimaryDestination(MapItem serializationParamsMap) throws XPathException {
            Serializer serializer = makeSerializer(serializationParamsMap);
            primaryWriter = new StringWriter();
            serializer.setOutputWriter(primaryWriter);
            return serializer;
        }

        @Override
        public Sequence getPrimaryResult() throws XPathException {
            String str = primaryWriter.toString();
            if (str.isEmpty()) {
                return null;
            }
            return postProcess(baseOutputUri, new StringValue(str));
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            StringWriter writer = new StringWriter();
            if (absoluteURI.getScheme().equals(dummyBaseOutputUriScheme)) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            workInProgress.put(absoluteURI.toString(), writer);
            StreamResult streamResult = new StreamResult(writer);
            streamResult.setSystemId(absoluteURI.toString());
            return streamResult;
        }

        @Override
        public void close(Result result) throws XPathException {
            StringWriter work = workInProgress.get(result.getSystemId());
            if (work != null) {
                String output = workInProgress.get(result.getSystemId()).toString();
                results.put(result.getSystemId(), output);
                workInProgress.remove(result.getSystemId());
            }
        }

        @Override
        public HashTrieMap populateResultMap(HashTrieMap resultMap) throws XPathException {
            for (Map.Entry<String, String> entry : results.entrySet()) {
                String uri = entry.getKey();
                resultMap = resultMap.addEntry(new StringValue(uri),
                                               postProcess(uri, new StringValue(entry.getValue())));
            }
            return resultMap;
        }
    }

    private static class RawDeliverer extends Deliverer {
        private Map<String, XdmValue> results = new ConcurrentHashMap<String, XdmValue>();

        public RawDeliverer() {
        }

        @Override
        public Destination getPrimaryDestination(MapItem serializationParamsMap) throws XPathException {
            return null;
        }

        @Override
        public Sequence getPrimaryResult() {
            return null;
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            if (absoluteURI.toString().contains("http://saxonica.com/output-raw/output")) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            return new StreamResult(new File(absoluteURI)); //TODO - what is the result?
        }

        @Override
        public void close(Result result) throws XPathException {
            /*XdmValue output = new XdmValue();
            results.put(result.getSystemId(), output);*/
        }

        @Override
        public HashTrieMap populateResultMap(HashTrieMap resultMap) throws XPathException {
            for (Map.Entry<String, XdmValue> entry : results.entrySet()) {
                String uri = entry.getKey();
                resultMap = resultMap.addEntry(new StringValue(uri),
                                               postProcess(uri, entry.getValue().getUnderlyingValue()));
            }
            return resultMap;
        }
    }


}
