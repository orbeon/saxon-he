package net.sf.saxon.functions;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * This abstract class is provided to allow user-written extension functions to be implemented
 * with the full capabilities of functions that are an intrinsic part of the Saxon product.
 * In particular, the class has the opportunity to save data from the static context and
 * to optimize itself at compile time.
 *
 * <p>There should be one class implementing this interface for each function name; if there
 * are several functions with the same name but different arity, the same class should implement
 * them all.</p>
 *
 * <p>Note that an IntegratedFunction is trusted; calls are allowed even if the configuration option
 * {@link net.sf.saxon.FeatureKeys#ALLOW_EXTERNAL_FUNCTIONS} is false. In cases where an IntegratedFunction
 * is used to load and execute untrusted code, it should check this configuration option before doing so.</p>
 *
 * @since 9.2
 */
public abstract class ExtensionFunctionDefinition implements Serializable {

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     * @return the function name
     */

    public abstract StructuredQName getFunctionQName();

    /**
     * Get the minimum number of arguments required by the function
     * <p>This method must be implemented in all subclasses</p>
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public abstract int getMinimumNumberOfArguments();

    /**
     * Get the maximum number of arguments allowed by the function.
     * <p>The default implementation returns the value of {@link #getMinimumNumberOfArguments}
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return getMinimumNumberOfArguments();
    }

    /**
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     * @return the required types of the arguments, as defined by the function signature. Normally
     * this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     * that allow a variable number of arguments, the array can be smaller than this, with the last
     * entry in the array providing the required type for all the remaining arguments.
     */

    public abstract SequenceType[] getArgumentTypes();

    /**
     * Get the type of the result of the function
     * <p>This method must be implemented in all subtypes.</p>
     * @return the return type of the function, as defined by its function signature
     * @param suppliedArgumentTypes the static types of the supplied arguments to the function.
     * This is provided so that a more precise result type can be returned in the common
     * case where the type of the result depends on the types of the arguments.
     */

    public abstract SequenceType getResultType(SequenceType[] suppliedArgumentTypes);

    /**
     * Ask whether the result actually returned by the function can be trusted,
     * or whether it should be checked against the declared type.
     * @return true if the function implementation warrants that the value it returns will
     * be an instance of the declared result type. The default value is false, in which case
     * the result will be checked at run-time to ensure that it conforms to the declared type.
     * If the value true is returned, but the function returns a value of the wrong type, the
     * consequences are unpredictable.
     */

    public boolean trustResultType() {
        return false;
    }

    /**
     * Ask whether the result of the function depends on the focus
     * @return true if the result of the function depends on the context item, position, or size.
     * The default implementation returns false. This must be set to true if the function
     * makes use of any of these values from the dynamic context. If this is not done, it is possible
     * that the function will be called with incorrect values for the focus.
     */

    public boolean dependsOnFocus() {
        return false;
    }

    /**
     * Ask whether the function has side-effects. If the function does have side-effects, the optimizer
     * will be less aggressive in moving or removing calls to the function. However, calls on functions
     * with side-effects can never be guaranteed.
     * @return true if the function has side-effects (including creation of new nodes, if the
     * identity of those nodes is significant). The default implementation returns false.
     */

    public boolean hasSideEffects() {
        return false;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     */

    public abstract ExtensionFunctionCall makeCallExpression();

    /**
     * Return an object capable of compiling this IntegratedFunction call to Java source code. The returned
     * object may be null, in which case Java code generation is not supported for this IntegratedFunction.
     * If the returned object is not null, it must implement the interface
     * com.saxonica.codegen.IntegratedFunctionCompiler. The default implementation returns null
     * @return an instance of com.saxonica.codegen.IntegratedFunctionCompiler that generates Java code
     * to implement the call on this extension function
     */

    public Object getCompilerForJava() {
        return null;
    }


}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//