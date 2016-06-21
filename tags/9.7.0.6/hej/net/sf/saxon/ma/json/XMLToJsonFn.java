////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.event.DocumentValidator;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.StartTagBuffer;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Implement the XML to JSON conversion as a built-in function - fn:xml-to-json()
 */
public class XMLToJsonFn extends SystemFunction {

    /**
     * Throw an error
     *
     * @param message the error message
     * @param code    the error code to be used
     * @throws net.sf.saxon.trans.XPathException
     *          always
     */

    private void error(String message, String code)
            throws XPathException {
        throw new XPathException(message, code);
    }

    private final static Map<String, SequenceType> requiredTypes = new HashMap<String, SequenceType>(20);

    static {
        requiredTypes.put("indent", SequenceType.SINGLE_BOOLEAN);
    }

    private static String[] optionNames = new String[]{
            "indent"
    };

    private static boolean isOptionName(String string) {
        for (String s : optionNames) {
            if (s.equals(string)) {
                return true;
            }
        }
        return false;
    }

    public static MapItem checkOptions(MapItem map, XPathContext context) throws XPathException {
        HashTrieMap result = new HashTrieMap(context);
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();

        AtomicIterator keysIterator = map.keys();
        AtomicValue key;
        while ((key = keysIterator.next()) != null) {
            if (key instanceof StringValue) {
                String keyName = key.getStringValue();
                if (isOptionName(keyName)) {
                    RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.OPTION, keyName, 0);
                    role.setErrorCode("XPTY0004");
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


    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo xml = (NodeInfo) arguments[0].head();
        if (xml == null) {
            return EmptySequence.getInstance();
        }
        MapItem options;
        if (getArity() == 2) {
            options = (MapItem) arguments[1].head();   // check it is a map
        } else {
            options = new HashTrieMap(context);
        }
        boolean indent = false;
        options = checkOptions(options, context);
        Sequence val = options.get(new StringValue("indent"));
        if (val != null) {
            indent = ExpressionTool.effectiveBooleanValue(val.head());
        }

        /*val = options.get(new StringValue("indent-spaces"));
        long indentSpaces = val != null && val instanceof Int64Value ? ((Int64Value) val).longValue() : -1;*/

        PipelineConfiguration pipe = context.getController().makePipelineConfiguration();
        JsonReceiver receiver = new JsonReceiver(pipe);
        receiver.setIndenting(indent);
        Receiver r = receiver;
        if (xml.getNodeKind() == Type.DOCUMENT) {
            r = new DocumentValidator(r, "FOJS0006");
        }
        StartTagBuffer stb = new StartTagBuffer(r);
        pipe.setComponent(StartTagBuffer.class.getName(), stb);
        stb.setPipelineConfiguration(pipe);
        stb.open();
        xml.copy(stb, NodeInfo.NO_NAMESPACES, ExplicitLocation.UNKNOWN_LOCATION);
        stb.close();
        return new StringValue(receiver.getJsonString());
    }

    /**
     * Get the value of an option setting (as a string)
     *
     * @param options      the set of options provided
     * @param option       the name of the option required
     * @param context      XPath evaluation context
     * @param defaultValue the default to be used if the option has not been specified
     * @return the setting of the option
     * @throws net.sf.saxon.trans.XPathException if the options cannot be read
     */

    private String getOption(MapItem options, String option, XPathContext context, String defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence val = options.get(ov);
        if (val == null) {
            return defaultValue;
        } else if (val instanceof AtomicValue) {
            return ((AtomicValue) val).getStringValue();
        } else {
            error("Value of option '" + option + "' is not xs:string", "XXJJ9999");
            return defaultValue;
        }
    }

    /**
     * Get the value of an option setting (as a boolean)
     *
     * @param options      the set of options provided
     * @param option       the name of the option required
     * @param context      XPath evaluation context
     * @param defaultValue the default to be used if the option has not been specified
     * @return the setting of the option
     * @throws net.sf.saxon.trans.XPathException if the options cannot be read
     */
    private boolean getOption(MapItem options, String option, XPathContext context, boolean defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence val = options.get(ov);
        if (val == null) {
            return defaultValue;
            //} else if (val instanceof AtomicValue) {
        } else if (val instanceof BooleanValue) {
            return ((BooleanValue) val).getBooleanValue();
        } else {
            error("Value of option '" + option + "' is not xs:boolean", "XXJJ9999");
            return defaultValue;
        }
    }

}
