////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.ma.map.*;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.*;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

/**
 * Implements the serialize-json function defined in Functions and Operators 3.0.
 */
public class SerializeJsonFn extends SystemFunction implements Callable {

    private static class Flags {
        public boolean escape = true;
        public boolean indent = false;
        public String spec = "RFC4627";
        public Function fallback = null;
        public int indentation = 0;
        public int depth = 0;
    }

//    /**
//     * Evaluate the function to produce a single item or an empty sequence
//     *
//     * @param context The context in which the expression is to be evaluated
//     * @return either an item, or null to denote the empty sequence
//     * @throws net.sf.saxon.trans.XPathException
//     *          if a failure occurs, e.g. bad JSON syntax
//     */
//
//    public StringValue evaluateItem(XPathContext context) throws XPathException {
//        SequenceIterator value = getArg(0).iterate(context);
//        MapItem options;
//        if (getArity() == 2) {
//            options = (MapItem) getArg(1).evaluateItem(context);
//        } else {
//            options = new HashTrieMap(context);
//        }
//        return evalSerializeJson(SequenceExtent.makeSequenceExtent(value), options, context);
//    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments
     * @return the result of the evaluation
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        MapItem options;
        if (getArity() == 2) {
            options = (MapItem) arguments[1].head();
        } else {
            options = new HashTrieMap(context);
        }
        return evalSerializeJson(arguments[0], options, context);
    }

    private StringValue evalSerializeJson(Sequence value, MapItem options, XPathContext context) throws XPathException {
        Flags flags = new Flags();
        flags.escape = getBooleanOption(options, "escape", context, true);
        flags.indent = getBooleanOption(options, "indent", context, false);
        String spec = getOption(options, "spec", context, "RFC4627");
        if (spec != null) {
            flags.spec = spec;
        }
        Sequence fv = options.get(new StringValue("fallback"));
        if (fv != null) {
            GroundedValue fvv = SequenceTool.toGroundedValue(fv);
            if (fvv.getLength() != 1) {
                throw new XPathException("fallback option in serialize-json() call must be a single item", "FOJS0002");
            }
            Item fvi = fvv.head();
            if (fvi instanceof Function) {
                flags.fallback = (Function) fvi;
            } else {
                throw new XPathException("fallback option in serialize-json() call must be a function item", "FOJS0002");
            }
        }

        FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.C256);
        writeSequence(value, flags, buffer, context);
        return new StringValue(buffer.condense());
    }

    /**
     * Serialize an arbitrary XDM sequence as JSON
     *
     * @param value   the value to be serialized
     * @param flags   serialization options
     * @param buffer  the buffer into which the result should be written
     * @param context the XPath dynamic evaluation context
     * @throws XPathException if an error occurs
     */

    private void writeSequence(Sequence value, Flags flags, FastStringBuffer buffer, XPathContext context) throws XPathException {
        GroundedValue val = SequenceTool.toGroundedValue(value);
        if (val.getLength() == 0) {
            testTopLevel(flags);
            buffer.append("null");
        } else if (val.getLength() == 1) {
            writeItem(SequenceTool.asItem(val), flags, buffer, context);
        } else {
            SequenceIterator iter = value.iterate();
            buffer.append('[');
            indent(flags, buffer);
            Item member;
            boolean first = true;
            while ((member = iter.next()) != null) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(',');
                    nextline(flags, buffer);
                }
                writeItem(member, flags, buffer, context);
            }
            outdent(flags, buffer);
            buffer.append(']');
        }
    }

    /**
     * Get the value of an option setting (as a string)
     *
     * @param options      the set of options provided
     * @param option       the name of the option required
     * @param context      XPath evaluation context
     * @param defaultValue the default to be used if the option has not been specified
     * @return the setting of the option
     * @throws net.sf.saxon.trans.XPathException
     *          if the options cannot be read
     */

    private static String getOption(MapItem options, String option, XPathContext context, String defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence opt = options.get(ov);
        if (opt == null) {
            return defaultValue;
        }
        try {
            Item val = SequenceTool.asItem(opt);
            return val.getStringValue();
        } catch (Exception err) {
            throw new XPathException("Value of " + option + " option must be an xs:string", "FOJS0002");
        }
    }

    private static boolean getBooleanOption(MapItem options, String option, XPathContext context, boolean defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence opt = options.get(ov);
        if (opt == null) {
            return defaultValue;
        }
        try {
            Item val = SequenceTool.asItem(opt);
            return ((BooleanValue) val).getBooleanValue();
        } catch (Exception err) {
            throw new XPathException("Value of " + option + " option must be an xs:boolean", "FOJS0002");
        }
    }

    /**
     * Serialize an item as JSON
     *
     * @param input   the item to be serialized
     * @param flags   serialization options
     * @param buffer  the buffer to hold the result
     * @param context XPath evaluation context
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs
     */

    private void writeItem(/*@Nullable*/ Item input, Flags flags, FastStringBuffer buffer, XPathContext context) throws XPathException {
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        if (input == null) {
            testTopLevel(flags);
            buffer.append("null");
            return;
        } else if (input instanceof BooleanValue) {
            testTopLevel(flags);
            buffer.append(((BooleanValue) input).getBooleanValue() ? "true" : "false");
            return;
        } else if (input instanceof NumericValue) {
            testTopLevel(flags);
            String s = input.getStringValue();
            if (s.equals("NaN") || s.equals("INF") || s.equals("-INF")) {
                buffer.append('"');
                buffer.append(s);
                buffer.append('"');
            } else {
                buffer.append(s);
            }
            return;
        } else if (input instanceof StringValue && !(input instanceof AnyURIValue)) {
            testTopLevel(flags);
            escape(input.getStringValue(), flags, buffer);
            return;
        } else if (input instanceof MapItem) {
            // A map can represent a JSON object (string keys), or a JSON array (integer keys)
            // An array (even an empty array) is marked by the presence of the integer key -1
            if (((MapItem)input).get(IntegerValue.MINUS_ONE) != null) {
                writeArray((MapItem) input, flags, buffer, context);
                return;
            } else if (((MapItem) input).size() == 0) {
                buffer.append("{}");
                return;
            } else {
                AtomicType keyType = ((MapItem) input).getKeyType();
                if (keyType.equals(BuiltInAtomicType.INTEGER)) {
                    writeArray((MapItem) input, flags, buffer, context);
                    return;
                } else if (keyType.equals(BuiltInAtomicType.STRING)) {
                    writeMap((MapItem) input, flags, buffer, context);
                    return;
                }
            }
        }

        // No mapping found: try to use fallback function

        if (flags.fallback != null) {
            Sequence result = flags.fallback.call(context, new Sequence[]{input});
            SequenceExtent value = new SequenceExtent(result.iterate());
            if (value.getLength() == 1) {
                // prevent infinite recursion
                Item iValue = SequenceTool.asItem(value);
                ItemType keyType;
                if (!(iValue instanceof StringValue || iValue instanceof NumericValue || iValue instanceof BooleanValue
                        || (iValue instanceof MapItem &&
                        ((keyType = ((MapItem) iValue).getKeyType()).equals(BuiltInAtomicType.INTEGER) ||
                                keyType.equals(BuiltInAtomicType.STRING))))) {
                    throw new XPathException("No JSON mapping defined for fallback value " +
                            UType.getUType(input), "FOJS0002");
                }
            }
            writeSequence(value, flags, buffer, context);
        } else {
            throw new XPathException("No JSON mapping defined for " +
                    UType.getUType(input), "FOJS0002");
        }

    }

    private void testTopLevel(Flags flags) throws XPathException {
        if (flags.depth == 0 && flags.spec.equals("RFC4627")) {
            throw new XPathException("Top level item in RFC 4627 JSON structure must be an object or array", "FOJS0002");
        }
    }

    private void escape(String string, Flags flags, FastStringBuffer buffer) {
        buffer.append('"');
        if (flags.escape) {
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                switch (c) {
                    case '"':
                        buffer.append("\\\"");
                        break;
                    case '\\':
                        buffer.append("\\\\");
                        break;
                    case '\b':
                        buffer.append("\\b");
                        break;
                    case '\f':
                        buffer.append("\\f");
                        break;
                    case '\n':
                        buffer.append("\\n");
                        break;
                    case '\r':
                        buffer.append("\\r");
                        break;
                    case '\t':
                        buffer.append("\\t");
                        break;
                    default:
                        if (UTF16CharacterSet.isSurrogate(c)) {
                            buffer.append("\\u");
                            // Has to be four hex digits, but surrogates always are
                            buffer.append(Integer.toHexString(c));
                        } else {
                            buffer.append(c);
                        }
                }
            }
        } else {
            // escape = false
            buffer.append(string);
        }
        buffer.append('"');
    }

    private void writeArray(MapItem input, Flags flags, FastStringBuffer buffer, XPathContext context) throws XPathException {
        buffer.append('[');
        indent(flags, buffer);
        AtomicValue key;
        long max = Long.MIN_VALUE;
        for (KeyValuePair pair : input) {
            long val = ((IntegerValue) pair.key).longValue();
            if (val == 0 || val < -1) {
                throw new XPathException("Cannot serialize a map with non-positive integer keys", "FOJS0002");
            }
            if (val > max) {
                max = val;
            }
        }
        for (long k = 1L; k <= max; k++) {
            if (k != 1L) {
                buffer.append(',');
                nextline(flags, buffer);
            }
            key = Int64Value.makeIntegerValue(k);
            Sequence val = input.get(key);
            if (val == null) {
                buffer.append("null");
            } else {
                writeSequence(val, flags, buffer, context);
            }
        }
        outdent(flags, buffer);
        buffer.append(']');
    }


    private void writeMap(MapItem input, Flags flags, FastStringBuffer buffer, XPathContext context) throws XPathException {
        buffer.append('{');
        indent(flags, buffer);
        boolean first = true;
        for (KeyValuePair pair : input) {
            if (!(pair.key instanceof StringValue)) {
                throw new XPathException("Key in map must be a string");
            }
            if (first) {
                first = false;
            } else {
                buffer.append(',');
                nextline(flags, buffer);
            }
            escape(pair.key.getStringValue(), flags, buffer);
            if (flags.indent) {
                buffer.append(" : ");
            } else {
                buffer.append(':');
            }
            writeSequence(pair.value, flags, buffer, context);
        }
        outdent(flags, buffer);
        buffer.append('}');

    }

    public void indent(Flags flags, FastStringBuffer buffer) {
        flags.depth++;
        if (flags.indent) {
            buffer.append('\n');
            flags.indentation += 2;
            for (int i = 0; i < flags.indentation; i++) {
                buffer.append(' ');
            }
        }
    }

    public void nextline(Flags flags, FastStringBuffer buffer) {
        if (flags.indent) {
            buffer.append('\n');
            for (int i = 0; i < flags.indentation; i++) {
                buffer.append(' ');
            }
        }
    }

    public void outdent(Flags flags, FastStringBuffer buffer) {
        if (flags.indent) {
            buffer.append('\n');
            flags.indentation -= 2;
            for (int i = 0; i < flags.indentation; i++) {
                buffer.append(' ');
            }
        }
        flags.depth--;
    }


}

// Copyright (c) 2011 Saxonica Limited. All rights reserved.