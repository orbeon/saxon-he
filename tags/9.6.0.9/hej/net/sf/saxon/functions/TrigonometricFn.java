////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.SequenceType;


/**
 * Abstract class providing functionality common to functions math:sin(), math:cos(), math:sqrt() etc;
 * contains the concrete implementations of these functions as inner subclasses
 */
public abstract class TrigonometricFn extends ExtensionFunctionDefinition {

    private static StructuredQName SIN = new StructuredQName("math", NamespaceConstant.MATH, "sin");
    private static StructuredQName COS = new StructuredQName("math", NamespaceConstant.MATH, "cos");
    private static StructuredQName TAN = new StructuredQName("math", NamespaceConstant.MATH, "tan");
    private static StructuredQName ASIN = new StructuredQName("math", NamespaceConstant.MATH, "asin");
    private static StructuredQName ACOS = new StructuredQName("math", NamespaceConstant.MATH, "acos");
    private static StructuredQName ATAN = new StructuredQName("math", NamespaceConstant.MATH, "atan");
    private static StructuredQName SQRT = new StructuredQName("math", NamespaceConstant.MATH, "sqrt");
    private static StructuredQName LOG = new StructuredQName("math", NamespaceConstant.MATH, "log");
    private static StructuredQName LOG10 = new StructuredQName("math", NamespaceConstant.MATH, "log10");
    private static StructuredQName EXP = new StructuredQName("math", NamespaceConstant.MATH, "exp");
    private static StructuredQName EXP10 = new StructuredQName("math", NamespaceConstant.MATH, "exp10");

    /**
     * Get the minimum number of arguments required by the function
     * <p>The default implementation returns the number of items in the result of calling
     * {@link #getArgumentTypes}</p>
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    /**
     * Get the maximum number of arguments allowed by the function.
     * <p>The default implementation returns the value of {@link #getMinimumNumberOfArguments}
     *
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return 1;
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
        return new SequenceType[]{SequenceType.OPTIONAL_DOUBLE};
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
     * The function that does the work, which must be implemented in subclasses
     *
     * @param input the input value
     * @return the result
     */

    protected abstract double compute(double input);

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    /*@Nullable*/
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {
            public ZeroOrOne<DoubleValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
                DoubleValue arg = (DoubleValue) arguments[0].head();
                if (arg == null) {
                    return ZeroOrOne.empty();
                }
                double result = compute(arg.getDoubleValue());
                return new ZeroOrOne<DoubleValue>(new DoubleValue(result));
            }
        };
    }

    /**
     * Implement math:sin
     */

    public static class SinFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return SIN;
        }

        protected double compute(double input) {
            return Math.sin(input);
        }
    }

    /**
     * Implement math:cos
     */

    public static class CosFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return COS;
        }

        protected double compute(double input) {
            return Math.cos(input);
        }
    }

    /**
     * Implement math:tan
     */

    public static class TanFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return TAN;
        }

        protected double compute(double input) {
            return Math.tan(input);
        }
    }

    /**
     * Implement math:asin
     */

    public static class AsinFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return ASIN;
        }

        protected double compute(double input) {
            return Math.asin(input);
        }
    }

    /**
     * Implement math:acos
     */

    public static class AcosFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return ACOS;
        }

        protected double compute(double input) {
            return Math.acos(input);
        }
    }

    /**
     * Implement math:atan
     */

    public static class AtanFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return ATAN;
        }

        protected double compute(double input) {
            return Math.atan(input);
        }
    }

    /**
     * Implement math:sqrt
     */

    public static class SqrtFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return SQRT;
        }

        protected double compute(double input) {
            return Math.sqrt(input);
        }
    }

    /**
     * Implement math:log
     */

    public static class LogFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return LOG;
        }

        protected double compute(double input) {
            return Math.log(input);
        }
    }

    /**
     * Implement math:log10
     */

    public static class Log10Fn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return LOG10;
        }

        protected double compute(double input) {
            return Math.log10(input);
        }
    }

    /**
     * Implement math:exp
     */

    public static class ExpFn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return EXP;
        }

        protected double compute(double input) {
            return Math.exp(input);
        }
    }

    /**
     * Implement math:exp10
     */

    public static class Exp10Fn extends TrigonometricFn {

        public StructuredQName getFunctionQName() {
            return EXP10;
        }

        protected double compute(double input) {
            return Math.pow(10, input);
        }
    }


}

// Copyright (c) 2010 Saxonica Limited. All rights reserved.