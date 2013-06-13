package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SavedNamespaceContext;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;

/**
 * The ConstructorFunctionLibrary represents the collection of constructor functions for atomic types. These
 * are provided for the built-in types such as xs:integer and xs:date, and also for user-defined atomic types.
 */

public class ConstructorFunctionLibrary implements FunctionLibrary {

    private Configuration config;

    /**
     * Create a SystemFunctionLibrary
     *
     * @param config the Configuration
     */

    public ConstructorFunctionLibrary(Configuration config) {
        this.config = config;
    }

    /**
     * Test whether a system function with a given name and arity is available, and return its signature. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     *
     * @param functionName the name of the function
     * @param arity        The number of arguments. This is set to -1 in the case of the single-argument
     * @return if a function of this name and arity is available for calling, then the type signature of the
     *         function, as an array of sequence types in which the zeroth entry represents the return type; otherwise null
     */

    /*@Nullable*/
    public SequenceType[] getFunctionSignature(StructuredQName functionName, int arity) {
        if (arity != 1 && arity != -1) {
            return null;
        }
        String uri = functionName.getURI();
        String local = functionName.getLocalPart();
        if (uri.equals(NamespaceConstant.SCHEMA)) {
            SimpleType type = Type.getBuiltInSimpleType(uri, local);
            if (type != null) {
                if (type.isAtomicType()) {
                    if (((AtomicType)type).isAbstract()) {
                        return null;
                    } else {
                        return new SequenceType[]{
                                SequenceType.makeSequenceType((AtomicType)type, StaticProperty.ALLOWS_ZERO_OR_ONE),
                                SequenceType.OPTIONAL_ATOMIC
                        };
                    }
                } else {
                    assert type.isListType();
                    return new SequenceType[]{
                            SequenceType.makeSequenceType((AtomicType)((ListType)type).getItemType(), StaticProperty.ALLOWS_ZERO_OR_MORE),
                            SequenceType.OPTIONAL_ATOMIC
                    };
                }
            } else {
                return null;
            }
        }

        int fingerprint = config.getNamePool().getFingerprint(uri, local);
        if (fingerprint == -1) {
            return null;
        } else {
            SchemaType schemaType = config.getSchemaType(fingerprint);
            if (schemaType instanceof SimpleType) {
                SimpleType st = (SimpleType)schemaType;
                if (st instanceof AtomicType) {
                    return new SequenceType[]{
                            SequenceType.makeSequenceType((AtomicType) st, StaticProperty.ALLOWS_ZERO_OR_ONE),
                            SequenceType.OPTIONAL_ATOMIC
                    };
                } else if (st.isListType()) {
                    SimpleType sType = ((ListType) st).getItemType();
                    if (sType instanceof AtomicType) {
                        return new SequenceType[]{
                                SequenceType.makeSequenceType((AtomicType) sType, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                SequenceType.OPTIONAL_ATOMIC
                        };
                    }
                    return new SequenceType[]{
                            SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                            SequenceType.OPTIONAL_ATOMIC
                    };

                } else if (st.isUnionType()) {
                    return new SequenceType[]{
                            SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                            SequenceType.OPTIONAL_ATOMIC
                    };
                }
            }
            return null;
        }
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     *
     * @param functionName
     * @param arguments    The expressions supplied statically in the function call. The intention is
     *                     that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     *                     be used as part of the binding algorithm.
     * @param env
     * @param container
     * @return An object representing the extension function to be called, if one is found;
     *         null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function; or if this function library "owns" the namespace containing
     *          the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] arguments, StaticContext env, Container container)
            throws XPathException {
        final String uri = functionName.getURI();
        final String localName = functionName.getLocalPart();
        String targetURI = uri;
        boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);
        if (builtInNamespace) {
            // it's a constructor function: treat it as shorthand for a cast expression
            if (arguments.length != 1) {
                throw new XPathException("A constructor function must have exactly one argument");
            }
            SimpleType type = Type.getBuiltInSimpleType(uri, localName);
            if (type != null) {
                if (type.isAtomicType()) {
                    if (((AtomicType)type).isAbstract()) {
                        XPathException err = new XPathException("Abstract type used in constructor function: {" + uri + '}' + localName);
                        err.setErrorCode("XPST0017");
                        err.setIsStaticError(true);
                        throw err;
                    } else {
                        CastExpression cast = new CastExpression(arguments[0], (AtomicType)type, true);
                        if (arguments[0] instanceof StringLiteral) {
                            cast.setOperandIsStringLiteral(true);
                        }
                        if (type.isNamespaceSensitive()) {
                            cast.setNamespaceResolver(new SavedNamespaceContext(env.getNamespaceResolver()));
                        }
                        cast.setContainer(container);
                        return cast;
                    }
                } else if (type == ErrorType.getInstance()) {
                    XPathException err = new XPathException("Unsupported constructor function: {" + uri + '}' + localName);
                    err.setErrorCode("XPST0017");
                    err.setIsStaticError(true);
                    throw err;
                }  else {
                    assert type.isListType();
                    Expression exp = env.getConfiguration().obtainOptimizer().makeCastToList(arguments[0], (ListType) type, true);
                    exp.setContainer(container);
                    return exp;
                }
            } else {
                XPathException err = new XPathException("Unknown constructor function: {" + uri + '}' + localName);
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
                throw err;
            }

        }

        // Now see if it's a constructor function for a user-defined type

        if (arguments.length == 1) {
            int fp = config.getNamePool().getFingerprint(uri, localName);
            if (fp != -1) {
                SchemaType st = config.getSchemaType(fp);
                if (st instanceof AtomicType) {
                    Expression cast = new CastExpression(arguments[0], (AtomicType) st, true);
                    cast.setContainer(container);
                    return cast;
                } else if (st instanceof ListType && DecimalValue.THREE.equals(env.getXPathLanguageLevel())) {
                    return env.getConfiguration().obtainOptimizer().makeCastToList(arguments[0], (ListType) st, true);
                } else if (st instanceof ItemType && ((ItemType) st).isPlainType() &&
                        DecimalValue.THREE.equals(env.getXPathLanguageLevel())) {
                    // we have a cast to a union type
                    Expression cast = env.getConfiguration().obtainOptimizer().makeCastToUnion(arguments[0], st, true);
                    cast.setContainer(container);
                    return cast;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//