////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.ContextItemExpression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.lib.StandardModuleURIResolver;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.query.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import javax.xml.transform.stream.StreamSource;
import java.math.BigDecimal;
import java.util.*;

/**
 * This class implements the function load-xquery-module(), which is a standard function in XPath 3.1
 */
public class LoadXqueryModule extends SystemFunction implements Callable {

    private final static Map<String, SequenceType> requiredTypes = new HashMap<String, SequenceType>(20);

    static {
        requiredTypes.put("xquery-version", SequenceType.SINGLE_DECIMAL);
        requiredTypes.put("location-hints", SequenceType.STRING_SEQUENCE);
        requiredTypes.put("context-item", SequenceType.OPTIONAL_ITEM);
        requiredTypes.put("variables", SequenceType.makeSequenceType(new MapType(BuiltInAtomicType.QNAME, SequenceType.ANY_SEQUENCE), StaticProperty.EXACTLY_ONE)); // standard type?
        requiredTypes.put("vendor-options", SequenceType.makeSequenceType(new MapType(BuiltInAtomicType.QNAME, SequenceType.ANY_SEQUENCE), StaticProperty.EXACTLY_ONE));
    }

    /**
     * Check the options supplied:
     * 1. ignore any other options not in the specs;
     * 2. validate the types of the option values supplied.
     */

    private Sequence checkOption(MapItem map, String keyName, XPathContext context) throws XPathException {
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();

        StringValue key = new StringValue(keyName);
        if (map.get(key) != null) {
            RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.OPTION, keyName, 0);
            role.setErrorCode("FOQM0007");
            Sequence converted = th.applyFunctionConversionRules(
                map.get(key), requiredTypes.get(keyName), role, ExplicitLocation.UNKNOWN_LOCATION);
            converted = SequenceTool.toGroundedValue(converted);
            return converted;
        } else {
            return null;
        }
    }

    /**
     * Invoke the function
     *
     * @param context the XPath dynamic evaluation context
     * @param args    the actual arguments to be supplied
     * @return the result of invoking the function
     * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs within the function
     */

    public Sequence call(XPathContext context, Sequence[] args) throws XPathException {
        MapItem options;
        if (args.length == 2) {
            options = (MapItem) args[1].head();
        } else {
            options = new HashTrieMap(context);
        }
        Sequence xqueryVersionOption = checkOption(options, "xquery-version", context); // TODO What should we do with this?
        Sequence locationHintsOption = checkOption(options, "location-hints", context);
        Sequence variablesOption = checkOption(options, "variables", context);
        Sequence contextItemOption = checkOption(options, "context-item", context);
        Sequence vendorOptionsOption = checkOption(options, "vendor-options", context);

        int qv = 31;
        if (xqueryVersionOption != null) {
            BigDecimal decimalVn = ((DecimalValue) xqueryVersionOption.head()).getDecimalValue();
            if (decimalVn.equals(new BigDecimal("1.0")) || decimalVn.equals(new BigDecimal("3.0")) || decimalVn.equals(new BigDecimal("3.1"))) {
                qv = (decimalVn.multiply(BigDecimal.TEN)).intValue();
            } else {
                throw new XPathException("Unsupported XQuery version " + decimalVn, "FOQM0006");
            }
        }

        String moduleUri = args[0].head().getStringValue();
        if (moduleUri.isEmpty()) {
            throw new XPathException("First argument of fn:load-xquery-module() must not be a zero length string", "FOQM0001");
        }
        List<String> locationHints = new ArrayList<String>(); // location hints are currently ignored by QT3TestDriver?
        if (locationHintsOption != null) {
            SequenceIterator iterator = locationHintsOption.iterate();
            Item hint;
            while ((hint = iterator.next()) != null) {
                locationHints.add(hint.getStringValue());
            }
        }

        // Set the vendor options (configuration features) -- at the moment none supported
        /*if (vendorOptionsOption != null) {
            MapItem vendorOptions = (MapItem) options.get(new StringValue("vendor-options")).head();
        }*/

        Configuration config = context.getConfiguration();
        StaticQueryContext staticQueryContext = config.newStaticQueryContext();
        ModuleURIResolver moduleURIResolver = config.getModuleURIResolver();
        if (moduleURIResolver == null) {
            moduleURIResolver = new StandardModuleURIResolver(config);
        }
        staticQueryContext.setModuleURIResolver(moduleURIResolver);
        staticQueryContext.setLanguageVersion(qv);
        String baseURI = getRetainedStaticContext().getStaticBaseUriString();
        staticQueryContext.setBaseURI(baseURI);
        StreamSource[] streamSources;
        try {
            String[] hints = locationHints.toArray(new String[locationHints.size()]);
            streamSources = staticQueryContext.getModuleURIResolver().resolve(moduleUri, baseURI, hints);
            if (streamSources == null) {
                streamSources = new StandardModuleURIResolver(config).resolve(moduleUri, baseURI, hints);
            }
        } catch (XPathException e) {
            e.maybeSetErrorCode("FOQM0002");
            throw e;
        }
        if (streamSources.length == 0) {
            throw new XPathException("No library module found with specified target namespace " + moduleUri, "FOQM0002");
        }


        try {
            // TODO only first streamSource so far
            String sourceQuery = QueryReader.readSourceQuery(streamSources[0], config.getValidCharacterChecker() );
            staticQueryContext.compileLibrary(sourceQuery);
        } catch (XPathException e) {
            throw new XPathException(e.getMessage(), "FOQM0003"); // catch when module is invalid
        }
        QueryLibrary lib = staticQueryContext.getCompiledLibrary(moduleUri);
        if (lib == null) {
            throw new XPathException("The library module located does not have the expected namespace " + moduleUri, "FOQM0002");
        }
        QueryModule main = new QueryModule(staticQueryContext); // module to be loaded is a library module not a main module
        // so use alternative constructor?
        main.setPackageData(lib.getPackageData());
        main.setExecutable(lib.getExecutable());
        lib.link(main);
        XQueryExpression xqe = new XQueryExpression(new ContextItemExpression(), main, false);
        DynamicQueryContext dqc = new DynamicQueryContext(context.getConfiguration());

        // Get the external variables and set parameters on DynamicQueryContext dqc
        if (variablesOption != null) {
            MapItem extVariables = (HashTrieMap) variablesOption.head();
            AtomicIterator iterator = extVariables.keys();
            AtomicValue key;
            while ((key = iterator.next()) != null) {
                dqc.setParameter(((QNameValue) key).getStructuredQName(), extVariables.get(key));
            }
        }
        Controller newController;
        try {
            newController = xqe.newController(dqc);
        } catch (XPathException e) {
            if (e.getErrorCodeLocalPart().equals("XPDY0002")) {
                throw new XPathException(e.getMessage(), "FOQM0004"); // catches when external variables have not been set
            } else {
                throw e; // when there is a dynamic error, the error code should not be changed
            }
        }

        // Get the context item supplied, and set it on the newController
        if (contextItemOption != null) {
            newController.setGlobalContextItem(contextItemOption.head());
        }
        XPathContext newContext = newController.newXPathContext();

        // Evaluate the global variables, and add values to the result.

        MapItem variablesMap = new HashTrieMap(newContext);
        for (GlobalVariable var : lib.getGlobalVariables()) {
            Sequence value;
            QNameValue qNameValue = new QNameValue(var.getVariableQName(), BuiltInAtomicType.QNAME);
            if (qNameValue.getNamespaceURI().equals(moduleUri)) {
                try {
                    value = var.evaluateVariable(newContext);
                } catch (XPathException e) {
                    e.setIsGlobalError(false);  // to make it catchable
                    if (e.getErrorCodeLocalPart().equals("XPTY0004")) {
                        throw new XPathException(e.getMessage(), "FOQM0005"); // catches when external variables have wrong type
                    } else if (e.getErrorCodeLocalPart().equals("XPDY0002")) {
                        throw new XPathException(e.getMessage(), "FOQM0004"); // catches when external variables have not been set
                    } else {
                        throw e;
                    }
                }
                variablesMap = ((HashTrieMap) variablesMap).addEntry(qNameValue, value);
            }
        }
        // Add functions to the result.
        HashTrieMap functionsMap = new HashTrieMap(newContext);
        XQueryFunctionLibrary functionLib = lib.getGlobalFunctionLibrary();
        Iterator<XQueryFunction> functionIterator = functionLib.getFunctionDefinitions();
        if (functionIterator.hasNext()) {
            XQueryFunction function;
            HashTrieMap newMap;
            QNameValue functionQName;
            while (functionIterator.hasNext()) {
                function = functionIterator.next();
                functionQName = new QNameValue(function.getFunctionName(), BuiltInAtomicType.QNAME);
                if (functionQName.getNamespaceURI().equals(moduleUri)) {
                    UserFunction userFunction = function.getUserFunction();
                    userFunction.setPreallocatedController(newController);
                    if (functionsMap.get(functionQName) != null) {
                        newMap = ((HashTrieMap) functionsMap.get(functionQName)).addEntry(new Int64Value(function.getNumberOfArguments()), function.getUserFunction());
                    } else {
                        newMap = HashTrieMap.singleton(new Int64Value(function.getNumberOfArguments()), function.getUserFunction(), context);
                    }
                    functionsMap = functionsMap.addEntry(functionQName, newMap);
                }
            }
        }

        HashTrieMap map = new HashTrieMap(context);
        map = map.addEntry(new StringValue("variables"), variablesMap);
        map = map.addEntry(new StringValue("functions"), functionsMap);

        return map;
    }
}
