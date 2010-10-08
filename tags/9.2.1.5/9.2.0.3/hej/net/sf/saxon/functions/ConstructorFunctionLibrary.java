package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.CastExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;

/**
 * The ConstructorFunctionLibrary represents the collection of constructor functions for atomic types. These
 * are provided for the built-in types such as xs:integer and xs:date, and also for user-defined atomic types.
 */

public class ConstructorFunctionLibrary implements FunctionLibrary {

    private Configuration config;

    /**
     * Create a SystemFunctionLibrary
     * @param config the Configuration
     */

    public ConstructorFunctionLibrary(Configuration config) {
        this.config = config;
    }

    /**
     * Test whether a system function with a given name and arity is available, and return its signature. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param functionName the name of the function
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * @return if a function of this name and arity is available for calling, then the type signature of the
     * function, as an array of sequence types in which the zeroth entry represents the return type; otherwise null
     */

    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        if (arity != 1 && arity != -1) {
            return null;
        }
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (uri.equals(NamespaceConstant.SCHEMA)) {
            AtomicType type = (AtomicType)Type.getBuiltInItemType(uri, local);
            if (type != null && type.getFingerprint() != StandardNames.XS_NOTATION) {
                return new SequenceType[] {
                        SequenceType.makeSequenceType(type, StaticProperty.ALLOWS_ZERO_OR_ONE),
                        SequenceType.OPTIONAL_ATOMIC
                };
            } else {
                return null;
            }
        }

        int fingerprint = config.getNamePool().getFingerprint(uri, local);
        if (fingerprint == -1) {
            return null;
        } else {
            SchemaType st = config.getSchemaType(fingerprint);
            if (st != null && st.isAtomicType()) {
                return new SequenceType[] {
                        SequenceType.makeSequenceType((AtomicType)st, StaticProperty.ALLOWS_ZERO_OR_ONE),
                        SequenceType.OPTIONAL_ATOMIC
                };
            } else {
                return null;
            }
        }
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName
     * @param arguments  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] arguments, StaticContext env)
            throws XPathException {
        final String uri = functionName.getNamespaceURI();
        final String localName = functionName.getLocalName();
        String targetURI = uri;
        boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);
        if (builtInNamespace) {
            // it's a constructor function: treat it as shorthand for a cast expression
            if (arguments.length != 1) {
                throw new XPathException("A constructor function must have exactly one argument");
            }
            AtomicType type = (AtomicType)Type.getBuiltInItemType(targetURI, localName);
            if (type==null || type.getFingerprint() == StandardNames.XS_ANY_ATOMIC_TYPE ||
                    type.getFingerprint() == StandardNames.XS_NOTATION) {
                XPathException err = new XPathException("Unknown constructor function: {" + uri + '}' + localName);
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }

            return new CastExpression(arguments[0], type, true);
        }

        // Now see if it's a constructor function for a user-defined type

        if (arguments.length == 1) {
            int fp = config.getNamePool().getFingerprint(uri, localName);
            if (fp != -1) {
                SchemaType st = config.getSchemaType(fp);
                if (st != null && st.isAtomicType()) {
                    return new CastExpression(arguments[0], (AtomicType)st, true);
                }
            }
        }

        return null;
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
    }

}
//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//