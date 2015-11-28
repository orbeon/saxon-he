////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import com.saxonica.ee.stream.adjunct.NewMapAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;


/**
 * Implementation of the extension function map:new() => Map
 * Note this function has been dropped from the spec (May 2014) but is retained in Saxon to underpin
 * map constructor expression syntax. Its visible name has been changed to map:_new()
 * to wean users off it. Unlike map:merge(), this function rejects duplicate keys.
 */
public class MapNew extends ExtensionFunctionDefinition {

    public final static StructuredQName FUNCTION_NAME = new StructuredQName("map", NamespaceConstant.MAP_FUNCTIONS, "_new");
    private final static SequenceType[] ARG_TYPES = new SequenceType[]{
            SequenceType.makeSequenceType(MapType.ANY_MAP_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE)
    };

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the function name
     */

    public StructuredQName getFunctionQName() {
        return FUNCTION_NAME;
    }

    /**
     * Get the minimum number of arguments required by the function
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 0;
    }

    /**
     * Get the maximum number of arguments allowed by the function.
     * <p>The default implementation returns the value of {@link #getMinimumNumberOfArguments}
     *
     * @return the maximum number of arguments that may be supplied in a call to this function
     */
    @Override
    public int getMaximumNumberOfArguments() {
        return 1;
    }

    /**
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @return the required types of the arguments, as defined by the function signature. Normally
     *         this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     *         that allow a variable number of arguments, the array can be smaller than this, with the last
     *         entry in the array providing the required type for all the remaining arguments.
     */

    public SequenceType[] getArgumentTypes() {
        return ARG_TYPES;
    }

    /**
     * Get the type of the result of the function
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @param suppliedArgumentTypes the static types of the supplied arguments to the function.
     *                              This is provided so that a more precise result type can be returned in the common
     *                              case where the type of the result depends on the types of the arguments.
     * @return the return type of the function, as defined by its function signature
     */

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return HashTrieMap.SINGLE_MAP_TYPE;
    }

    /**
     * Ask whether the result actually returned by the function can be trusted,
     * or whether it should be checked against the declared type.
     *
     * @return true if the function implementation warrants that the value it returns will
     *         be an instance of the declared result type. The default value is false, in which case
     *         the result will be checked at run-time to ensure that it conforms to the declared type.
     *         If the value true is returned, but the function returns a value of the wrong type, the
     *         consequences are unpredictable.
     */

    public boolean trustResultType() {
        return true;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    public ExtensionFunctionCall makeCallExpression() {
        return new MapNewCall();
    }

    private class MapNewCall extends ExtensionFunctionCall {
        boolean selfContained = false;

        public void supplyStaticContext(StaticContext context, int locationId, Expression[] arguments) throws XPathException {
            // if the argument does not refer to any variables or functions, then any maps used as input to this one
            // can be destroyed in the process, because no-one else can ever use them.
            selfContained = arguments.length == 0 || !ExpressionTool.refersToVariableOrFunction(arguments[0]);
        }

        public void copyLocalData(ExtensionFunctionCall destination) {
            ((MapNewCall) destination).selfContained = selfContained;
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            if (arguments.length == 0) {
                return new HashTrieMap(context);
            } else {
                SequenceIterator iter = arguments[0].iterate();
                MapItem baseMap = (MapItem) iter.next();
                if (baseMap == null) {
                    return new HashTrieMap(context);
                } else {
                    if (!(baseMap instanceof HashTrieMap)) {
                        baseMap = HashTrieMap.copy(baseMap, context);
                    }
                    MapItem next;
                    while ((next = (MapItem) iter.next()) != null) {
                        for (KeyValuePair pair : next) {
                            if (baseMap.get(pair.key) != null) {
                                throw new XPathException("Map contains more than one entry for key '" +
                                    pair.key.getStringValue() + "'", "XQDY0137");
                            }
                            baseMap = ((HashTrieMap) baseMap).addEntry(pair.key, pair.value);
                        }
                    }
                    return baseMap;
                }
            }
        }

//#ifdefined STREAM
        /**
         * Get a streamable implementation of this extension function. This interface is provisional
         * and currently only really intended for internal use; it is subject to change. If the value
         * returned is non-null, then it must be an instance of com.saxonica.ee.stream.adjunct
         */
        @Override
        public Object getStreamingImplementation() {
            return new NewMapAdjunct();
        }
//#endif
    }
}

// Copyright (c) 2010-2014 Saxonica Limited. All rights reserved.