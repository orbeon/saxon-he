////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.type.SpecificFunctionType;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the extension function array:for-each-pair(array, array, function) => array
 */
public class ArrayForEachPair extends ExtensionFunctionDefinition {

    private final static StructuredQName name = new StructuredQName("array", NamespaceConstant.ARRAY_FUNCTIONS, "for-each-pair");
    private final static SequenceType[] ARG_TYPES = new SequenceType[]{
            ArrayItem.SINGLE_ARRAY_TYPE, ArrayItem.SINGLE_ARRAY_TYPE, SequenceType.makeSequenceType(
            new SpecificFunctionType(
                    new SequenceType[]{SequenceType.ANY_SEQUENCE, SequenceType.ANY_SEQUENCE},
                    SequenceType.ANY_SEQUENCE),
            StaticProperty.EXACTLY_ONE)};

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the function name
     */
    @Override
    public StructuredQName getFunctionQName() {
        return name;
    }

    /**
     * Get the minimum number of arguments required by the function
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 3;
    }

    /**
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @return the required types of the arguments, as defined by the function signature. Normally
     * this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     * that allow a variable number of arguments, the array can be smaller than this, with the last
     * entry in the array providing the required type for all the remaining arguments.
     */
    @Override
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
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return ArrayItem.SINGLE_ARRAY_TYPE;
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
     *
     * @return an expression representing a call of this extension function
     */
    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                ArrayItem array1 = (ArrayItem) arguments[0].head();
                assert array1 != null;
                ArrayItem array2 = (ArrayItem) arguments[1].head();
                assert array2 != null;
                Function fn = (Function) arguments[2].head();
                List<Sequence> list = new ArrayList<Sequence>(1);
                int i;
                for (i=0; i < array1.size() && i < array2.size(); i++) {
                    list.add(fn.call(context, new Sequence[]{array1.get(i), array2.get(i)}));
                }
                return new SimpleArrayItem(list);
            }
        };
    }
}
