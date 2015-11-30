////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;


/**
 * Abstract class providing functionality common to functions math:sin(), math:cos(), math:sqrt() etc;
 * contains the concrete implementations of these functions as inner subclasses
 */
public class PowFn extends ExtensionFunctionDefinition {

    public static String NAMESPACE = "http://www.w3.org/2005/xpath-functions/math";
    private static StructuredQName POW = new StructuredQName("math", NAMESPACE, "pow");

    /**
     * Get the name of the function, as a QName.
     *
     * @return the function name
     */

    public StructuredQName getFunctionQName() {
        return POW;
    }

    /**
     * Get the minimum number of arguments required by the function
     * <p>The default implementation returns the number of items in the result of calling
     * {@link #getArgumentTypes}</p>
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 2;
    }

    /**
     * Get the maximum number of arguments allowed by the function.
     * <p>The default implementation returns the value of {@link #getMinimumNumberOfArguments}
     *
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return 2;
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
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @return the required types of the arguments, as defined by the function signature. Normally
     *         this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     *         that allow a variable number of arguments, the array can be smaller than this, with the last
     *         entry in the array providing the required type for all the remaining arguments.
     */

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.OPTIONAL_DOUBLE, SequenceType.SINGLE_DOUBLE};
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
        return SequenceType.OPTIONAL_DOUBLE;
    }


    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    /*@Nullable*/
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            public ZeroOrOne<DoubleValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
                DoubleValue x = (DoubleValue) arguments[0].head();
                DoubleValue result;
                if (x == null) {
                    result = null;
                } else {
                    double dx = x.getDoubleValue();
                    if (dx == 1) {
                        result = x;
                    } else {
                        NumericValue y = (NumericValue) arguments[1].head();
                        assert y != null;
                        double dy = y.getDoubleValue();
                        if (dx == -1 && Double.isInfinite(dy)) {
                            result = new DoubleValue(1.0e0);
                        } else {
                            result = new DoubleValue(Math.pow(dx, dy));
                        }
                    }
                }
                return new ZeroOrOne<DoubleValue>(result);
            }
        };
    }

}

// Copyright (c) 2010-2014 Saxonica Limited. All rights reserved.