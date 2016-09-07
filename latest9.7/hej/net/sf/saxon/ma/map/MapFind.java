////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.ArrayItemType;
import net.sf.saxon.ma.arrays.SimpleArrayItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the (new) XPath 3.1 function map:find(sequence, key) => array
 */
public class MapFind extends ExtensionFunctionDefinition {

    public final static StructuredQName name = new StructuredQName("map", NamespaceConstant.MAP_FUNCTIONS, "find");
    private final static SequenceType[] ARG_TYPES = new SequenceType[]{
            SequenceType.ANY_SEQUENCE, SequenceType.SINGLE_ATOMIC
    };

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the function name
     */

    public StructuredQName getFunctionQName() {
        return name;
    }

    /**
     * Get the minimum number of arguments required by the function
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 2;
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
        return SequenceType.makeSequenceType(ArrayItemType.ANY_ARRAY_TYPE, StaticProperty.ALLOWS_ONE);
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

    /*@Nullable*/
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                List<Sequence> result = new ArrayList<Sequence>();
                AtomicValue key = (AtomicValue) arguments[1].head();
                processSequence(arguments[0], key, result);
                return new SimpleArrayItem(result);
            }
        };
    }

    private void processSequence(Sequence in, AtomicValue key, List<Sequence> result) throws XPathException {
        SequenceIterator iter = in.iterate();
        Item item;
        while ((item = iter.next()) != null) {
            if (item instanceof ArrayItem) {
                for (Sequence sequence : (ArrayItem) item) {
                    processSequence(sequence, key, result);
                }
            } else if (item instanceof MapItem) {
                Sequence value = ((MapItem) item).get(key);
                if (value != null) {
                    result.add(value);
                }
                for (KeyValuePair entry : (MapItem) item) {
                    processSequence(entry.value, key, result);
                }
            }
        }
    }
}

// Copyright (c) 2016 Saxonica Limited. All rights reserved.

