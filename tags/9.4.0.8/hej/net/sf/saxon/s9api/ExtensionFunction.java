package net.sf.saxon.s9api;

/**
 * This is an interface for simple external/extension functions. Users can implement this
 * interface and register the implementation with the {@link Processor}; the function will
 * then be available for calling from all queries, stylesheets, and XPath expressions compiled
 * under this Processor.
 *
 * Extension functions implemented using this interface are expected to be free of side-effects,
 * and to have no dependencies on the static or dynamic context. A richer interface for extension
 * functions is provided via the {@link net.sf.saxon.lib.ExtensionFunctionDefinition} class.
 */
public interface ExtensionFunction {

    /**
     * Return the name of the external function
     * @return the name of the function, as a QName.
     */
    public QName getName();

    /**
     * Declare the result type of the external function
     * @return the result type of the external function
     */

    public SequenceType getResultType();

    /**
     * Declare the types of the arguments
     * @return a sequence of SequenceType objects, one for each argument to the function,
     * representing the expected types of the arguments
     */

    public SequenceType[] getArgumentTypes();

    /**
     * Call the function. The implementation of this method represents the body of the external function.
     * @param arguments the arguments, as supplied in the XPath function call. These will always be of
     * the declared types. Arguments are converted to the required types according to the standard XPath
     * function conversion rules - for example, if the expected type is atomic and a node is supplied in the
     * call, the node will be atomized
     * @return the result of the function. This must be an instance of the declared return type; if it is not,
     * a dynamic error will be reported
     * @throws SaxonApiException can be thrown if the implementation of the function detects a dynamic error
     */

    /*@NotNull*/ public XdmValue call(XdmValue[] arguments) throws SaxonApiException;
}

///
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//