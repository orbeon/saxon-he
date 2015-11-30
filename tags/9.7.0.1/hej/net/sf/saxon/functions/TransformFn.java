////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.lib.AugmentedSource;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.StandardOutputResolver;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.ArrayItemType;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.StylesheetCache;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import org.xml.sax.InputSource;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the function transform(), which is a standard function in XPath 3.1
 */
public class TransformFn extends SystemFunction implements Callable {


    private String[] transformOptionNames = new String[]{
            "xslt-version", "stylesheet-location", "stylesheet-node", "stylesheet-text", "stylesheet-base-uri", "base-output-uri", "stylesheet-params",
            "source-node", "initial-mode", "initial-template", "delivery-format", "serialization-params", "vendor-options", "cache",
            "package-name", "package-version", "package-node", "package-location", "static-params", "global-context-item",
            "template-params", "tunnel-params", "initial-function", "function-params"
    };

    private String[] transformOptionNames30 = new String[]{
            "package-name", "package-version", "package-node", "package-location", "static-params", "global-context-item",
            "template-params", "tunnel-params", "initial-function", "function-params"
    };

    private boolean isTransformOptionName(String string) {
        for (String s : transformOptionNames) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTransformOptionName30(String string) {
        for (String s : transformOptionNames30) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private final static Map<String, SequenceType> requiredTypes = new HashMap<String, SequenceType>(40);

    static {
        requiredTypes.put("xslt-version", SequenceType.SINGLE_DECIMAL);
        requiredTypes.put("stylesheet-location", SequenceType.SINGLE_STRING);
        requiredTypes.put("stylesheet-node", SequenceType.SINGLE_NODE);
        requiredTypes.put("stylesheet-text", SequenceType.SINGLE_STRING);
        requiredTypes.put("stylesheet-base-uri", SequenceType.SINGLE_STRING);
        requiredTypes.put("base-output-uri", SequenceType.SINGLE_STRING);
        requiredTypes.put("stylesheet-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("source-node", SequenceType.SINGLE_NODE);
        requiredTypes.put("initial-mode", SequenceType.SINGLE_QNAME);
        requiredTypes.put("initial-template", SequenceType.SINGLE_QNAME);
        requiredTypes.put("delivery-format", SequenceType.SINGLE_STRING);
        requiredTypes.put("serialization-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("vendor-options", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("cache", SequenceType.SINGLE_BOOLEAN);
        requiredTypes.put("package-name", SequenceType.SINGLE_STRING);
        requiredTypes.put("package-version", SequenceType.SINGLE_DECIMAL);
        requiredTypes.put("package-node", SequenceType.SINGLE_NODE);
        requiredTypes.put("package-location", SequenceType.SINGLE_STRING);
        requiredTypes.put("static-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("global-context-item", SequenceType.SINGLE_ITEM);
        requiredTypes.put("template-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("tunnel-params", SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE));
        requiredTypes.put("initial-function", SequenceType.SINGLE_QNAME);
        requiredTypes.put("function-params", SequenceType.makeSequenceType(ArrayItemType.ANY_ARRAY_TYPE, StaticProperty.EXACTLY_ONE));
    }

    /**
     * Check the options supplied:
     * 1. only allow XSLT 3.0 options if using an XSLT 3.0 processor (throw an error if any are supplied and not an XSLT 3.0 processor);
     * 2. ignore any other options not in the specs;
     * 3. validate the types of the option values supplied.
     */

    private MapItem checkTransformOptions(MapItem map, XPathContext context, boolean isXslt30Processor) throws XPathException {
        HashTrieMap result = new HashTrieMap(context);
        if (map.size() == 0) {
            if (!isXslt30Processor) {
                throw new XPathException("A transform must be supplied with some options: exactly one of stylesheet-location|stylesheet-node|stylesheet-text, and source-node|initial-template", "FOXT0002");
            } else { // TODO - how much information in this message?
                throw new XPathException("A transform must be supplied with some options: providing some stylesheet or package, and invocation method", "FOXT0002");
            }
        }
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();

        AtomicIterator keysIterator = map.keys();
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String keyName = key.getStringValue();
                if (isTransformOptionName30(keyName) && !isXslt30Processor) {
                    throw new XPathException("The transform option " + keyName + " is only available when using an XSLT 3.0 processor", "FOXT0002");
                }
                if (isTransformOptionName(keyName)) {
                    RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.OPTION, keyName, 0);
                    role.setErrorCode("FOXT0002");
                    Sequence converted = th.applyFunctionConversionRules(
                        map.get(key), requiredTypes.get(keyName), role, ExplicitLocation.UNKNOWN_LOCATION);
                    converted = SequenceTool.toGroundedValue(converted);
                    result = result.addEntry(key, converted);
                }
            } else {
                break;
            }
        }
        return result;
    }

    private AtomicValue checkStylesheetMutualExclusion(AtomicIterator keysIterator) throws XPathException {
        boolean stylesheetOptionFound = false;
        AtomicValue styleOption = null;
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String name = key.getStringValue();
                if (name.equals("stylesheet-location") || name.equals("stylesheet-node") || name.equals("stylesheet-text")) {
                    if (stylesheetOptionFound) {
                        throw new XPathException("The following transform options are mutually exclusive: stylesheet-location|stylesheet-node|stylesheet-text", "FOXT0002");
                    }
                    stylesheetOptionFound = true;
                    styleOption = key;
                }
            } else {
                break;
            }
        }
        if (styleOption == null) {
            throw new XPathException("A transform must be supplied with one of: stylesheet-location|stylesheet-node|stylesheet-text", "FOXT0002");
        }
        return styleOption;
    }

    private AtomicValue checkStylesheetMutualExclusion30(AtomicIterator keysIterator) throws XPathException {
        boolean stylesheetOptionFound = false;
        AtomicValue styleOption = null;
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String name = key.getStringValue();
                if (name.equals("stylesheet-location") || name.equals("stylesheet-node") || name.equals("stylesheet-text") ||
                        name.equals("package-name") || name.equals("package-node") || name.equals("package-location")) {
                    if (stylesheetOptionFound) {
                        throw new XPathException("The following transform options are mutually exclusive: stylesheet-location|stylesheet-node|stylesheet-text|package-name|package-node|package-location", "FOXT0002");
                    }
                    stylesheetOptionFound = true;
                    styleOption = key;
                }
            } else {
                break;
            }
        }
        if (styleOption == null) {
            throw new XPathException("A transform must be supplied with one of: stylesheet-location|stylesheet-node|stylesheet-text|package-name|package-node|package-location", "FOXT0002");
        }
        if (styleOption.getStringValue().equals("package-node") || styleOption.getStringValue().equals("package-location")) {
            throw new XPathException("The transform option " + styleOption.getStringValue() + " is not implemented in Saxon", "FOXT0002");
        }
        return styleOption;
    }

    private AtomicValue checkInvocationMutualExclusion(AtomicIterator keysIterator) throws XPathException {
        boolean invocationOptionFound = false;
        AtomicValue invocationOption = null;
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String name = key.getStringValue();
                if (name.equals("initial-mode") || name.equals("initial-template")) {
                    if (invocationOptionFound) {
                        throw new XPathException("The following transform options are mutually exclusive: initial-mode|initial-template", "FOXT0002");
                    }
                    invocationOptionFound = true;
                    invocationOption = key;
                }
            } else {
                break;
            }
        }
        return invocationOption;
    }

    private AtomicValue checkInvocationMutualExclusion30(AtomicIterator keysIterator) throws XPathException {
        boolean invocationOptionFound = false;
        AtomicValue invocationOption = null;
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String name = key.getStringValue();
                if (name.equals("initial-mode") || name.equals("initial-template") || name.equals("initial-function")) {
                    if (invocationOptionFound) {
                        throw new XPathException("The following transform options are mutually exclusive: initial-mode|initial-template|initial-function", "FOXT0002");
                    }
                    invocationOptionFound = true;
                    invocationOption = key;
                }
            } else {
                break;
            }
        }
        return invocationOption;
    }

    private void setVendorOptions(MapItem map, Processor processor) throws XPathException {
        MapItem options = (MapItem) map.get(new StringValue("vendor-options")).head();
        AtomicIterator optionIterator = options.keys();
        while (true) {
            AtomicValue option = optionIterator.next();
            if (option != null) {
                QName optionName = new QName(((QNameValue) option.head()).getStructuredQName());
                XdmValue optionVal = XdmValue.wrap(options.get(option));
                if (((QNameValue) option.head()).getNamespaceURI().equals("http://saxon.sf.net/feature/")) {
                    processor.setConfigurationProperty(optionName.getNamespaceURI().concat(optionName.getLocalName()), optionVal.toString());
                    //booleans can be accepted as strings, anything other than strings or booleans will be ignored
                }
            } else {
                break;
            }
        }
    }

    private void setStaticParams(MapItem map, XsltCompiler xsltCompiler) throws XPathException {
        MapItem staticParamsMap = (MapItem) map.get(new StringValue("static-params")).head();
        AtomicIterator paramIterator = staticParamsMap.keys();
        while (true) {
            AtomicValue param = paramIterator.next();
            if (param != null) {
                QName paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                XdmValue paramVal = XdmValue.wrap(staticParamsMap.get(param));
                xsltCompiler.setParameter(paramName, paramVal);
            } else {
                break;
            }
        }
    }

    private XsltExecutable getStylesheet(MapItem map, XsltCompiler xsltCompiler, AtomicValue styleOption, XPathContext context) throws XPathException {
        String styleOptionStr = styleOption.getStringValue();
        Item styleOptionItem = map.get(styleOption).head();
        StringValue styleBaseUri = null;
        Sequence seq;
        if ((seq = map.get(new StringValue("stylesheet-base-uri"))) != null) {
            styleBaseUri = (StringValue) seq.head();
            URI styleBase = URI.create(styleBaseUri.getStringValue());
            if (!styleBase.isAbsolute()) {
                throw new XPathException("The transform option stylesheet-base-uri is not an absolute URI", "FOXT0002");
            }
        }
        BooleanValue cacheBool = BooleanValue.TRUE;
        if (map.get(new StringValue("cache")) != null) {
            cacheBool = (BooleanValue) map.get(new StringValue("cache")).head();
        }

        StylesheetCache cache = context.getController().getStylesheetCache();
        XsltExecutable executable = null;
        if (styleOptionStr.equals("stylesheet-location")) {
            String stylesheetLocation = styleOptionItem.getStringValue();
            executable = cache.getStylesheetByLocation(stylesheetLocation); // if stylesheet is already cached
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
                    if (styleBaseUri != null) {
                        style.setSystemId(styleBaseUri.getStringValue());
                    }
                } catch (TransformerException e) {
                    throw new XPathException(e);
                }
                try {
                    executable = xsltCompiler.compile(style);
                } catch (SaxonApiException e) {
                    if (e.getCause() instanceof XPathException) {
                        throw (XPathException) e.getCause();
                    } else {
                        throw new XPathException(e);
                    }
                }
                if (cacheBool == BooleanValue.TRUE) {
                    cache.setStylesheetByLocation(stylesheetLocation, executable);
                }
            }
        } else if (styleOptionStr.equals("stylesheet-node")) {
            NodeInfo stylesheetNode = (NodeInfo) styleOptionItem;

            if (styleBaseUri != null && !stylesheetNode.getBaseURI().equals(styleBaseUri.getStringValue())) {

                // If the stylesheet is supplied as a node, and the stylesheet-base-uri option is supplied, and doesn't match
                // the base URIs of the nodes (tests fn-transform-19 and fn-transform-41), then we have a bit of a problem.
                // So copy the stylesheet to a new tree having the desired base URI.

                final String sysId = styleBaseUri.getStringValue();
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

            executable = cache.getStylesheetByNode(stylesheetNode); // if stylesheet is already cached
            if (executable == null) {
                Source source = stylesheetNode;
                if (styleBaseUri != null) {
                    source = AugmentedSource.makeAugmentedSource(source);
                    source.setSystemId(styleBaseUri.getStringValue());
                }
                try {
                    executable = xsltCompiler.compile(source);
                } catch (SaxonApiException e) {
                    if (e.getCause() instanceof XPathException) {
                        throw (XPathException) e.getCause();
                    } else {
                        throw new XPathException(e);
                    }
                }
                if (cacheBool == BooleanValue.TRUE) {
                    cache.setStylesheetByNode(stylesheetNode, executable);
                }
            }
        } else if (styleOptionStr.equals("stylesheet-text")) {
            String stylesheetText = styleOptionItem.getStringValue();
            executable = cache.getStylesheetByText(stylesheetText); // if stylesheet is already cached
            if (executable == null) {
                StringReader sr = new StringReader(stylesheetText);
                SAXSource style = new SAXSource(new InputSource(sr));
                if (styleBaseUri != null) {
                    style.setSystemId(styleBaseUri.getStringValue());
                }
                try {
                    executable = xsltCompiler.compile(style);
                } catch (SaxonApiException e) {
                    if (e.getCause() instanceof XPathException) {
                        throw (XPathException) e.getCause();
                    } else {
                        throw new XPathException(e);
                    }
                }
                if (cacheBool == BooleanValue.TRUE) {
                    cache.setStylesheetByText(stylesheetText, executable);
                }
            }
        } else if (styleOptionStr.equals("package-name")) {
            String packageName = styleOptionItem.getStringValue();
            /*URI packageNameUri = URI.create(packageName); // TODO do we want this or not? package identifier should be a URI but Saxon accepts any string
            if (!packageNameUri.isAbsolute()) {
                throw new XPathException("The transform option package-name is not an absolute URI", "FOXT0002");
            }*/
            String packageVersion = null;
            if (map.get(new StringValue("package-version")) != null) {
                packageVersion = map.get(new StringValue("package-version")).head().getStringValue();
            }
            // retrieve the compiled package (as an XsltPackage) from the PackageLibrary
            /*StylesheetPackage stylesheetPackage = context.getConfiguration().getDefaultXsltCompilerInfo().getPackageLibrary().getPackage(packageName, new PackageVersionRanges(packageVersion));
            XsltPackage xsltPackage = new XsltPackage(xsltCompiler.getProcessor(), stylesheetPackage);
            try {
                executable = xsltPackage.link(); // load the already compiled package
            } catch (SaxonApiException e) {
                if (e.getCause() instanceof XPathException) {
                    throw (XPathException) e.getCause();
                } else {
                    throw new XPathException(e);
                }
            }*/
        }
        return executable;
    }


    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        MapItem map = (MapItem) arguments[0].head();
        assert map != null;

        Processor processor = new Processor(false);
        processor.setConfigurationProperty(FeatureKeys.CONFIGURATION, context.getConfiguration());
        boolean isXslt30Processor = context.getConfiguration().isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION);
        map = checkTransformOptions(map, context, isXslt30Processor);

        StringValue xsltVersionStr = new StringValue("xslt-version");
        if (map.get(xsltVersionStr) != null) {
            DecimalValue xsltVersion = (DecimalValue) map.get(xsltVersionStr).head();
            if ((xsltVersion.compareTo(DecimalValue.THREE) >= 0 && !isXslt30Processor) || (xsltVersion.compareTo(DecimalValue.THREE) > 0 && isXslt30Processor)) {
                throw new XPathException("The transform option xslt-version is higher than the XSLT version supported by this processor", "FOXT0002");
            }
        }
        // Check the rules and restrictions for combinations of transform options
        AtomicValue invocationOption;
        String invocationName = new String("invocation");
        AtomicValue styleOption;
        if (isXslt30Processor) {
            invocationOption = checkInvocationMutualExclusion30(map.keys());
            // if invocation option is not initial-function or initial-template then check for source-node
            if (invocationOption != null) {
                invocationName = invocationOption.getStringValue();
            }
            if (!invocationName.equals("initial-template") && !invocationName.equals("initial-function") && map.get(new StringValue("source-node")) == null) {
                throw new XPathException("A transform must have at least one of the following options: source-node|initial-template|initial-function", "FOXT0002");
            }
            // if invocation option is initial-function, then check for function-params
            if (invocationName.equals("initial-function") && map.get(new StringValue("function-params")) == null) {
                throw new XPathException("Use of the transform option initial-function requires the function parameters to be supplied using the option function-params", "FOXT0002");
            }
            // function-params should only be used if invocation option is initial-function
            if (!invocationName.equals("initial-function") && map.get(new StringValue("function-params")) != null) {
                throw new XPathException("The transform option function-params can only be used if the option initial-function is also used", "FOXT0002");
            }
            styleOption = checkStylesheetMutualExclusion30(map.keys());
        } else {
            invocationOption = checkInvocationMutualExclusion(map.keys());
            // if invocation option is not initial-template then check for source-node
            if (invocationOption != null) {
                invocationName = invocationOption.getStringValue();
            }
            if (!invocationName.equals("initial-template") && map.get(new StringValue("source-node")) == null) {
                throw new XPathException("A transform must have at least one of the following options: source-node|initial-template", "FOXT0002");
            }
            styleOption = checkStylesheetMutualExclusion(map.keys());
        }
        // Set the vendor options (configuration features) on the processor
        if (map.get(new StringValue("vendor-options")) != null) {
            setVendorOptions(map, processor);
        }

        XsltCompiler xsltCompiler = processor.newXsltCompiler();
        xsltCompiler.setURIResolver(context.getURIResolver());
        // Set static params on XsltCompiler before compiling stylesheet (XSLT 3.0 processing only)
        if (map.get(new StringValue("static-params")) != null) {
            setStaticParams(map, xsltCompiler);
        }

        XsltExecutable sheet = getStylesheet(map, xsltCompiler, styleOption, context);
        Xslt30Transformer transformer = sheet.load30();

        Destination destination = new XdmDestination();
        String deliveryFormat = "document";
        NodeInfo sourceNode = null;
        QName initialTemplate = null;
        QName initialMode = null;
        StringValue baseOutputUri = null;
        Map<QName, XdmValue> stylesheetParams = new HashMap<QName, XdmValue>();
        MapItem serializationParamsMap = null;
        StringWriter serializedResult = null;
        File serializedResultFile = null;
        XdmItem globalContextItem = null;
        Map<QName, XdmValue> templateParams = new HashMap<QName, XdmValue>();
        Map<QName, XdmValue> tunnelParams = new HashMap<QName, XdmValue>();
        QName initialFunction = null;
        XdmValue[] functionParams = null;
        XdmValue rawResult = null;

        AtomicIterator keysIterator = map.keys();
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String name = key.getStringValue();
                if (name.equals("source-node")) {
                    Sequence source = map.get(key);
                    sourceNode = (NodeInfo) source.head();
                } else if (name.equals("initial-template")) {
                    initialTemplate = new QName(((QNameValue) map.get(key).head()).getStructuredQName());
                } else if (name.equals("initial-mode")) {
                    initialMode = new QName(((QNameValue) map.get(key).head()).getStructuredQName());
                } else if (name.equals("delivery-format")) {
                    deliveryFormat = map.get(key).head().getStringValue();
                    if (!isXslt30Processor) {
                        if (!deliveryFormat.equals("document") && !deliveryFormat.equals("serialized") && !deliveryFormat.equals("saved")) {
                            throw new XPathException("The transform option delivery-format should be one of: document|serialized|saved ", "FOXT0002");
                        }
                    } else if (!deliveryFormat.equals("document") && !deliveryFormat.equals("serialized") && !deliveryFormat.equals("saved") && !deliveryFormat.equals("raw")) {
                        throw new XPathException("The transform option delivery-format should be one of: document|serialized|saved|raw ", "FOXT0002");
                    }
                } else if (name.equals("base-output-uri")) {
                    baseOutputUri = (StringValue) map.get(key).head();
                } else if (name.equals("serialization-params")) {
                    serializationParamsMap = (MapItem) map.get(key).head();
                } else if (name.equals("stylesheet-params")) {
                    MapItem params = (MapItem) map.get(key).head(); //Check this map?? (i.e. validate type of keys)
                    AtomicIterator paramIterator = params.keys();
                    while (true) {
                        AtomicValue param = paramIterator.next();
                        if (param != null) {
                            QName paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                            XdmValue paramVal = XdmValue.wrap(params.get(param));
                            stylesheetParams.put(paramName, paramVal);
                        } else {
                            break;
                        }
                    }
                } else if (name.equals("global-context-item")) {
                    globalContextItem = (XdmItem) map.get(key).head();
                } else if (name.equals("template-params")) {
                    MapItem params = (MapItem) map.get(key).head();
                    AtomicIterator paramIterator = params.keys();
                    while (true) {
                        AtomicValue param = paramIterator.next();
                        if (param != null) {
                            QName paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                            XdmValue paramVal = XdmValue.wrap(params.get(param));
                            templateParams.put(paramName, paramVal);
                        } else {
                            break;
                        }
                    }
                } else if (name.equals("tunnel-params")) {
                    MapItem params = (MapItem) map.get(key).head();
                    AtomicIterator paramIterator = params.keys();
                    while (true) {
                        AtomicValue param = paramIterator.next();
                        if (param != null) {
                            QName paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                            XdmValue paramVal = XdmValue.wrap(params.get(param));
                            tunnelParams.put(paramName, paramVal);
                        } else {
                            break;
                        }

                    }
                } else if (name.equals("initial-function")) {
                    initialFunction = new QName(((QNameValue) map.get(key).head()).getStructuredQName());
                } else if (name.equals("function-params")) {
                    ArrayItem functionParamsArray = (ArrayItem) map.get(key).head();
                    functionParams = new XdmValue[functionParamsArray.size()];
                    for (int i = 0; i < functionParams.length; i++) {
                        functionParams[i] = XdmValue.wrap(functionParamsArray.get(i));
                    }
                }
            }
        }

        Controller controller = transformer.getUnderlyingController();
        final Map<String, TreeInfo> secondaryResultsForDocument = new ConcurrentHashMap<String, TreeInfo>();
        final Map<String, String> secondaryResultsForSerialized = new ConcurrentHashMap<String, String>();
        final Map<String, String> secondaryResultsForSaved = new ConcurrentHashMap<String, String>();
        final Map<String, XdmValue> secondaryResultsForRaw = new ConcurrentHashMap<String, XdmValue>();
        if (deliveryFormat.equals("document")) {
            controller.setOutputURIResolver(new SecondaryOutputResolverForDocument(controller, secondaryResultsForDocument));
        } else if (deliveryFormat.equals("serialized")) {
            controller.setOutputURIResolver(new SecondaryOutputResolverForSerialized(secondaryResultsForSerialized));
        } else if (deliveryFormat.equals("saved")) {
            controller.setOutputURIResolver(new SecondaryOutputResolverForSaved(secondaryResultsForSaved));
        } else if (deliveryFormat.equals("raw")) {
            controller.setOutputURIResolver(new SecondaryOutputResolverForRaw(secondaryResultsForRaw));
        }

        try {
            transformer.setStylesheetParameters(stylesheetParams);
            transformer.setInitialTemplateParameters(templateParams, false);
            transformer.setInitialTemplateParameters(tunnelParams, true);
            if (baseOutputUri != null) {
                transformer.setBaseOutputURI(baseOutputUri.getStringValue());
            }
            if (baseOutputUri == null && deliveryFormat.equals("document")) {
                transformer.setBaseOutputURI("http://saxonica.com/output-document/output");
            }
            if (deliveryFormat.equals("serialized") || deliveryFormat.equals("saved")) {
                Serializer serializer = processor.newSerializer();
                if (serializationParamsMap != null) {  // TODO should serialization params do something if delivery format is document or raw?
                    AtomicIterator paramIterator = serializationParamsMap.keys();
                    while (true) {
                        AtomicValue param = paramIterator.next();
                        if (param != null) {
                            QName paramName = new QName(((QNameValue) param.head()).getStructuredQName());
                            StringValue paramVal = (StringValue) serializationParamsMap.get(param);
                            Serializer.Property prop = serializer.getProperty(paramName);
                            serializer.setOutputProperty(prop, paramVal.getStringValue());
                        } else {
                            break;
                        }
                    }
                }
                if (deliveryFormat.equals("serialized")) {
                    if (baseOutputUri == null) {
                        URI outputUri = URI.create("http://saxonica.com/output-serialized/output");
                        transformer.setBaseOutputURI(outputUri.toString());
                    }
                    serializedResult = new StringWriter();
                    serializer.setOutputWriter(serializedResult);
                } else if (deliveryFormat.equals("saved")) {
                    if (baseOutputUri == null) {
                        // If baseOutputUri is not supplied, then stylesheet must have xsl:result-document elements with href with *absolute* URIs.
                        // Catch error when this is not the case: by using a recognisable dummy base URI.
                        URI outputUri = URI.create("file:///saxon-dummy-output-file-984hvds3sfmixxak22/output");
                        serializedResultFile = new File(outputUri);
                        // This file is a kind of dummy, no such file will actually be created;
                        // but it prepares the serializer to produce files (as secondary result documents).
                        // TODO this was not working! A file *is* being produced. Check conditions changed line 720. Is this right?
                    } else if (baseOutputUri != null) {
                        URI outputUri = URI.create(baseOutputUri.getStringValue());
                        if (!outputUri.isAbsolute()) {
                            throw new XPathException("The transform option base-output-uri is not an absolute URI", "FOXT0002");
                        }
                        serializedResultFile = new File(outputUri);
                    }
                    serializer.setOutputFile(serializedResultFile);
                }
                destination = serializer;
            } else if (deliveryFormat.equals("raw")) {
                transformer.setBaseOutputURI("http://saxonica.com/output-raw/output");
            }
            if (initialTemplate != null) {
                /*if (globalContextItem != null) {
                    transformer.setGlobalContextItem(globalContextItem);
                } else*/ //TODO is this right? then what about sourceNode?
                //transformer.setInitialContextNode(new XdmNode(sourceNode)); is this what we want instead?
                if (sourceNode != null) {
                    transformer.setGlobalContextItem(new XdmNode(sourceNode));
                }
                if (deliveryFormat.equals("raw")) {
                    rawResult = transformer.callTemplate(initialTemplate);
                } else {
                    transformer.callTemplate(initialTemplate, destination);
                }
            } else if (initialFunction != null) {
                /*if (globalContextItem != null) {
                    transformer.setGlobalContextItem(globalContextItem);
                } else*/ //TODO is this right? then what about sourceNode?
                if (sourceNode != null) {
                    transformer.setGlobalContextItem(new XdmNode(sourceNode));
                }
                if (deliveryFormat.equals("raw")) {
                    rawResult = transformer.callFunction(initialFunction, functionParams);
                } else {
                    transformer.callFunction(initialFunction, functionParams, destination);
                }
            } else {
                if (initialMode != null) {
                    transformer.setInitialMode(initialMode);
                }
                if (deliveryFormat.equals("raw")) {
                    rawResult = transformer.applyTemplates(sourceNode);
                } else {
                    transformer.applyTemplates(sourceNode, destination);
                }
            }
        } catch (SaxonApiException e) {
            if (e.getCause() instanceof XPathException) {
                throw (XPathException) e.getCause();
            } else {
                throw new XPathException(e);
            }
        }

        /* Result value in result map should be a DocumentInfo if delivery-format is document;
         * a String (of serialized result) if delivery-format is serialized;
         * a String (absolute URI of location of saved result) if delivery-format is saved.
         * an XdmValue if delivery-format is raw (XSLT 3.0 transforms only)
         * */

        HashTrieMap resultMap = new HashTrieMap(context);
        if (deliveryFormat.equals("document")) {
            for (Map.Entry<String, TreeInfo> entry : secondaryResultsForDocument.entrySet()) {
                resultMap = resultMap.addEntry(new StringValue(entry.getKey()), entry.getValue().getRootNode());
            }
        } else if (deliveryFormat.equals("serialized")) {
            for (Map.Entry<String, String> entry : secondaryResultsForSerialized.entrySet()) {
                resultMap = resultMap.addEntry(new StringValue(entry.getKey()), new StringValue(entry.getValue()));
            }
        } else if (deliveryFormat.equals("saved")) {
            for (Map.Entry<String, String> entry : secondaryResultsForSaved.entrySet()) {
                resultMap = resultMap.addEntry(new StringValue(entry.getKey()), new StringValue(entry.getValue()));
            }
        } else if (deliveryFormat.equals("raw")) {
            for (Map.Entry<String, XdmValue> entry : secondaryResultsForRaw.entrySet()) {
                resultMap = resultMap.addEntry(new StringValue(entry.getKey()), entry.getValue().getUnderlyingValue());
            }
        }


        AtomicValue resultKey;
        if (baseOutputUri == null) {
            resultKey = new StringValue("output");
        } else {
            resultKey = baseOutputUri;
        }
        Sequence value = null;
        if (deliveryFormat.equals("document")) {
            XdmNode xdmNode = ((XdmDestination) destination).getXdmNode();
            if (xdmNode != null) {
                value = xdmNode.getUnderlyingNode();
            }
        } else if (deliveryFormat.equals("serialized")) {
            String result = serializedResult.toString();
            if (!result.equals("")) {
                value = new StringValue(result);
            }
        } else if (deliveryFormat.equals("saved")) {
            if (serializedResultFile.isFile() && !serializedResultFile.getAbsolutePath().contains("saxon-dummy-output-file-984hvds3sfmixxak22")) {
                value = resultKey;
                // 2nd condition changed from serializedResultFile != null
                // Previously: serializedResultFile is null in the case that only secondary result documents are produced,
                // and serializedResultFile was just a dummy. Then value remains null, and no new entry to result map.
            }
        } else if (deliveryFormat.equals("raw")) {
            value = rawResult.getUnderlyingValue();
        }
        if (value != null) {
            resultMap = resultMap.addEntry(resultKey, value);
        }
        return resultMap;

    }

    private static class SecondaryOutputResolverForDocument extends StandardOutputResolver {
        private Controller controller;
        private Map<String, TreeInfo> results;

        public SecondaryOutputResolverForDocument(Controller controller, Map<String, TreeInfo> results) {
            this.controller = controller;
            this.results = results;
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            Builder builder = controller.makeBuilder();
            if (absoluteURI.toString().contains("http://saxonica.com/output-document")) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            builder.setSystemId(absoluteURI.toString());
            return builder;
        }

        /**
         * Signal completion of the result document. This method is called by the system
         * when the result document has been successfully written. It allows the resolver
         * to perform tidy-up actions such as closing output streams, or firing off
         * processes that take this result tree as input. Note that the OutputURIResolver
         * is stateless, so the original href is supplied to identify the document
         * that has been completed.
         *
         * @param result
         */
        @Override
        public void close(Result result) throws XPathException {
            NodeInfo doc = ((Builder) result).getCurrentRoot();
            results.put(doc.getSystemId(), doc.getTreeInfo());
        }
    }

    private static class SecondaryOutputResolverForSerialized extends StandardOutputResolver {
        private Map<String, String> results;
        private Map<String, StringWriter> workInProgress = new ConcurrentHashMap<String, StringWriter>();

        public SecondaryOutputResolverForSerialized(Map<String, String> results) {
            this.results = results;
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            StringWriter writer = new StringWriter();
            if (absoluteURI.toString().contains("http://saxonica.com/output-serialized")) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            workInProgress.put(absoluteURI.toString(), writer);
            StreamResult streamResult = new StreamResult(writer);
            streamResult.setSystemId(absoluteURI.toString());
            return streamResult;
        }

        /**
         * Signal completion of the result document. This method is called by the system
         * when the result document has been successfully written. It allows the resolver
         * to perform tidy-up actions such as closing output streams, or firing off
         * processes that take this result tree as input. Note that the OutputURIResolver
         * is stateless, so the original href is supplied to identify the document
         * that has been completed.
         *
         * @param result
         */
        @Override
        public void close(Result result) throws XPathException {
            String output = workInProgress.get(result.getSystemId()).toString();
            results.put(result.getSystemId(), output);
            workInProgress.remove(result.getSystemId());
        }
    }

    private static class SecondaryOutputResolverForSaved extends StandardOutputResolver {
        private Map<String, String> results;
        //private Map<String, StringWriter> workInProgress = new ConcurrentHashMap<String, StringWriter>();

        public SecondaryOutputResolverForSaved(Map<String, String> results) {
            this.results = results;
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            if (absoluteURI.toString().contains("/saxon-dummy-output-file-984hvds3sfmixxak22/")) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            return new StreamResult(new File(absoluteURI));
        }

        /**
         * Signal completion of the result document. This method is called by the system
         * when the result document has been successfully written. It allows the resolver
         * to perform tidy-up actions such as closing output streams, or firing off
         * processes that take this result tree as input. Note that the OutputURIResolver
         * is stateless, so the original href is supplied to identify the document
         * that has been completed.
         *
         * @param result
         */
        @Override
        public void close(Result result) throws XPathException {
            results.put(result.getSystemId(), result.getSystemId());
            //workInProgress.remove(result.getSystemId());
        }
    }

    private static class SecondaryOutputResolverForRaw extends StandardOutputResolver {
        private Map<String, XdmValue> results;

        public SecondaryOutputResolverForRaw(Map<String, XdmValue> results) {
            this.results = results;
        }

        @Override
        protected Result createResult(URI absoluteURI) throws XPathException, IOException {
            if (absoluteURI.toString().contains("http://saxonica.com/output-raw/output")) {
                throw new XPathException("The location of output documents is undefined: use the transform option base-output-uri", "FOXT0002");
            }
            return new StreamResult(new File(absoluteURI)); //TODO - what is the result?
        }

        /**
         * Signal completion of the result document. This method is called by the system
         * when the result document has been successfully written. It allows the resolver
         * to perform tidy-up actions such as closing output streams, or firing off
         * processes that take this result tree as input. Note that the OutputURIResolver
         * is stateless, so the original href is supplied to identify the document
         * that has been completed.
         *
         * @param result
         */
        @Override
        public void close(Result result) throws XPathException {
            /*XdmValue output = new XdmValue();
            results.put(result.getSystemId(), output);*/
        }
    }


}
