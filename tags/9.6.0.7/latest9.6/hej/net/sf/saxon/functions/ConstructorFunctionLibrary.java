////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.functions.hof.AtomicConstructorFunctionItem;
import com.saxonica.functions.hof.CallableFunctionItem;
import com.saxonica.functions.hof.SpecificFunctionType;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SavedNamespaceContext;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

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

//#ifdefined HOF

    /**
     * Test whether a function with a given name and arity is available; if so, return a function
     * item that can be dynamically called.
     * <p/>
     * <p>This supports the function-lookup() function in XPath 3.0.</p>
     *
     * @param functionName  the qualified name of the function being called
     * @param staticContext the static context to be used by the function, in the event that
     *                      it is a system function with dependencies on the static context
     * @param container     the container for the new function item
     * @return if a function of this name and arity is available for calling, then a corresponding
     *         function item; or null if the function does not exist
     * @throws net.sf.saxon.trans.XPathException
     *          in the event of certain errors, for example attempting to get a function
     *          that is private
     */
    public FunctionItem getFunctionItem(SymbolicName functionName, StaticContext staticContext, Container container) throws XPathException {
        if (functionName.getArity() != 1) {
            return null;
        }
        final String uri = functionName.getComponentName().getURI();
        final String localName = functionName.getComponentName().getLocalPart();
        int fp = config.getNamePool().allocate("", uri, localName);
        final SchemaType type = config.getSchemaType(fp);
        if (type == null || type.isComplexType()) {
            return null;
        }
        final NamespaceResolver resolver = ((SimpleType) type).isNamespaceSensitive() ? staticContext.getNamespaceResolver() : null;
        if (type instanceof AtomicType) {
            return new AtomicConstructorFunctionItem((AtomicType) type, resolver);
        } else if (type instanceof ListType) {
            SimpleType itemType = ((ListType) type).getItemType();
            Callable callable = new Callable() {
                public AtomicSequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                    AtomicValue value = (AtomicValue) arguments[0].head();
                    if (value == null) {
                        return AtomicArray.EMPTY_ATOMIC_ARRAY;
                    }
                    if (value instanceof StringValue && !(value instanceof AnyURIValue)) {
                        return CastToList.cast(value.getStringValue(), (ListType) type, resolver, context.getConfiguration().getConversionRules());
                    }
                    throw new XPathException("When casting to list, the input must be of type xs:string or xs:untypedAtomic", "XPTY0004");
                }
            };
            SpecificFunctionType functionType;
            if (itemType.isAtomicType()) {
                functionType = new SpecificFunctionType(
                        new SequenceType[]{SequenceType.OPTIONAL_ATOMIC},
                        SequenceType.makeSequenceType((AtomicType) itemType, StaticProperty.ALLOWS_ZERO_OR_MORE));
            } else {
                functionType = new SpecificFunctionType(
                        new SequenceType[]{SequenceType.OPTIONAL_ATOMIC},
                        SequenceType.ATOMIC_SEQUENCE);
            }
            return new CallableFunctionItem(1, callable, functionType);
        } else {
            return staticContext.getConfiguration().obtainOptimizer().makeCastToUnion((UnionType) type, resolver);
        }
    }

    //#endif


    public boolean isAvailable(SymbolicName functionName) {
        if (functionName.getArity() != 1) {
            return false;
        }
        final String uri = functionName.getComponentName().getURI();
        final String localName = functionName.getComponentName().getLocalPart();
        int fp = config.getNamePool().allocate("", uri, localName);
        final SchemaType type = config.getSchemaType(fp);
        if (type == null || type.isComplexType()) {
            return false;
        }
        if (type.isAtomicType() && ((AtomicType) type).isAbstract()) {
            return false;
        }
        return type != AnySimpleType.getInstance();
    }

    /**
     * Bind a static function call, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     *
     * @param functionName The QName of the function
     * @param arguments    The expressions supplied statically in the function call. The intention is
     *                     that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     *                     be used as part of the binding algorithm.
     * @param env          The static context
     * @param container    A container to provide location information for the expression
     * @return An object representing the constructor function to be called, if one is found;
     *         null if no constructor function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function; or if this function library "owns" the namespace containing
     *          the function call, but no function was found.
     */

    public Expression bind(SymbolicName functionName, Expression[] arguments, StaticContext env, Container container)
            throws XPathException {
        final String uri = functionName.getComponentName().getURI();
        final String localName = functionName.getComponentName().getLocalPart();
        boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);
        if (builtInNamespace) {
            // it's a constructor function: treat it as shorthand for a cast expression
            if (functionName.getArity() != 1) {
                throw new XPathException("A constructor function must have exactly one argument");
            }
            SimpleType type = Type.getBuiltInSimpleType(uri, localName);
            if (type != null) {
                if (type.isAtomicType()) {
                    if (((AtomicType) type).isAbstract()) {
                        XPathException err = new XPathException("Abstract type used in constructor function: {" + uri + '}' + localName);
                        err.setErrorCode("XPST0017");
                        err.setIsStaticError(true);
                        throw err;
                    } else {
                        CastExpression cast = new CastExpression(arguments[0], (AtomicType) type, true);
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
                    return new CastToUnion(arguments[0], ErrorType.getInstance(), true);
                } else {
                    assert type.isListType();
                    return new CastToList(arguments[0], (ListType) type, true);
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
                if (st instanceof SimpleType) {
                    if (st instanceof AtomicType) {
                        Expression cast = new CastExpression(arguments[0], (AtomicType) st, true);
                        cast.setContainer(container);
                        return cast;
                    } else if (st instanceof ListType && DecimalValue.THREE.equals(env.getXPathLanguageLevel())) {
                        return new CastToList(arguments[0], (ListType) st, true);
                    } else if (((SimpleType) st).isUnionType() && DecimalValue.THREE.equals(env.getXPathLanguageLevel())) {
                        // we have a cast to a union type
                        Expression cast = new CastToUnion(arguments[0], (UnionType) st, true);
                        cast.setContainer(container);
                        return cast;
                    }
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