package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.functions.StringFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import javax.xml.transform.SourceLocator;

/**
 * This class provides Saxon's type checking capability. It contains a static method,
 * staticTypeCheck, which is called at compile time to perform type checking of
 * an expression. This class is never instantiated.
 */

public final class TypeChecker {

    // Class is not instantiated
    private TypeChecker() {}

    /**
     * Check an expression against a required type, modifying it if necessary.
     *
     * <p>This method takes the supplied expression and checks to see whether it is
     * known statically to conform to the specified type. There are three possible
     * outcomes. If the static type of the expression is a subtype of the required
     * type, the method returns the expression unchanged. If the static type of
     * the expression is incompatible with the required type (for example, if the
     * supplied type is integer and the required type is string) the method throws
     * an exception (this results in a compile-time type error being reported). If
     * the static type is a supertype of the required type, then a new expression
     * is constructed that evaluates the original expression and checks the dynamic
     * type of the result; this new expression is returned as the result of the
     * method.</p>
     *
     * <p>The rules applied are those for function calling in XPath, that is, the rules
     * that the argument of a function call must obey in relation to the signature of
     * the function. Some contexts require slightly different rules (for example,
     * operands of polymorphic operators such as "+"). In such cases this method cannot
     * be used.</p>
     *
     * <p>Note that this method does <b>not</b> do recursive type-checking of the
     * sub-expressions.</p>
     *
     * @param supplied      The expression to be type-checked
     * @param req           The required type for the context in which the expression is used
     * @param backwardsCompatible
     *                      True if XPath 1.0 backwards compatibility mode is applicable
     * @param role          Information about the role of the subexpression within the
     *                      containing expression, used to provide useful error messages
     * @param env           The static context containing the types being checked. At present
     *                      this is used only to locate a NamePool
     * @return              The original expression if it is type-safe, or the expression
     *                      wrapped in a run-time type checking expression if not.
     * @throws net.sf.saxon.trans.StaticError if the supplied type is statically inconsistent with the
     *                      required type (that is, if they have no common subtype)
     */

    public static Expression staticTypeCheck(Expression supplied,
                                             SequenceType req,
                                             boolean backwardsCompatible,
                                             RoleLocator role,
                                             StaticContext env)
    throws StaticError {

        // System.err.println("Static Type Check on expression (requiredType = " + req + "):"); supplied.display(10);

        Expression exp = supplied;
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        ItemType reqItemType = req.getPrimaryType();
        int reqCard = req.getCardinality();
        boolean allowsMany = Cardinality.allowsMany(reqCard);

        ItemType suppliedItemType = null;
            // item type of the supplied expression: null means not yet calculated
        int suppliedCard = -1;
            // cardinality of the supplied expression: -1 means not yet calculated

        boolean cardOK = (reqCard == StaticProperty.ALLOWS_ZERO_OR_MORE);
        // Unless the required cardinality is zero-or-more (no constraints).
        // check the static cardinality of the supplied expression
        if (!cardOK) {
            suppliedCard = exp.getCardinality();
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                // May later find that cardinality is not OK after all, if atomization takes place
        }

        boolean itemTypeOK = reqItemType instanceof AnyItemType;
        // Unless the required item type and content type are ITEM (no constraints)
        // check the static item type against the supplied expression.
        // NOTE: we don't currently do any static inference regarding the content type
        if (!itemTypeOK) {
            suppliedItemType = exp.getItemType(th);
            if (suppliedItemType instanceof EmptySequenceTest) {
                // supplied type is empty-sequence(): this can violate a cardinality constraint but not an item type constraint
                itemTypeOK = true;
            } else {
                if (reqItemType == null || suppliedItemType == null) {
                    throw new NullPointerException();
                }
                int relation = th.relationship(reqItemType, suppliedItemType);
                itemTypeOK = relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMES;
            }
        }


        // Handle the special rules for 1.0 compatibility mode
        if (backwardsCompatible && !allowsMany) {
            // rule 1
            if (Cardinality.allowsMany(suppliedCard)) {
                Expression cexp = new FirstItemExpression(exp);
                cexp.adoptChildExpression(exp);
                exp = cexp;
                suppliedCard = StaticProperty.ALLOWS_ZERO_OR_ONE;
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
            if (!itemTypeOK) {
                // rule 2
                if (reqItemType.equals(BuiltInAtomicType.STRING)) {
                    StringFn fn = (StringFn) SystemFunction.makeSystemFunction("string", 1, env.getNamePool());
                    fn.setParentExpression(exp.getParentExpression());
                    Expression[] args = {exp};
                    fn.setArguments(args);
                    try {
                        exp = fn.simplify(env).typeCheck(env, AnyItemType.getInstance());
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    suppliedItemType = BuiltInAtomicType.STRING;
                    suppliedCard = StaticProperty.EXACTLY_ONE;
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                    itemTypeOK = true;
                }
                // rule 3
                if (reqItemType.equals(BuiltInAtomicType.NUMERIC) || reqItemType.equals(BuiltInAtomicType.DOUBLE)) {
                    NumberFn fn = (NumberFn)SystemFunction.makeSystemFunction("number", 1, env.getNamePool());
                    fn.setParentExpression(exp.getParentExpression());
                    Expression[] args = {exp};
                    fn.setArguments(args);
                    try {
                        exp = fn.simplify(env).typeCheck(env, AnyItemType.getInstance());
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    suppliedItemType = BuiltInAtomicType.DOUBLE;
                    suppliedCard = StaticProperty.EXACTLY_ONE;
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                    itemTypeOK = true;
                }
            }
        }

        if (!itemTypeOK) {
            // Now apply the conversions needed in 2.0 mode

            if (reqItemType.isAtomicType()) {

                // rule 1: Atomize
                if (!(suppliedItemType.isAtomicType()) &&
                        !(suppliedCard == StaticProperty.EMPTY)) {
                    Expression cexp = new Atomizer(exp, env.getConfiguration());
                    exp = cexp;
                    suppliedItemType = exp.getItemType(th);
                    suppliedCard = exp.getCardinality();
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                }

                // rule 2: convert untypedAtomic to the required type

                //   2a: all supplied values are untyped atomic. Convert if necessary, and we're finished.

                if ((suppliedItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC))
                        && !(reqItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) || reqItemType.equals(BuiltInAtomicType.ANY_ATOMIC))) {

                    Expression cexp = new UntypedAtomicConverter(exp, (AtomicType)reqItemType, true);
                    try {
                        if (exp instanceof Literal) {
                            exp = Literal.makeLiteral(
                                    new SequenceExtent(cexp.iterate(env.makeEarlyEvaluationContext())).simplify());
                        } else {
                            exp = cexp;
                        }
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    itemTypeOK = true;
                    suppliedItemType = reqItemType;
                }

                //   2b: some supplied values are untyped atomic. Convert these to the required type; but
                //   there may be other values in the sequence that won't convert and still need to be checked

                if ((suppliedItemType.equals(BuiltInAtomicType.ANY_ATOMIC))
                    && !(reqItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) || reqItemType.equals(BuiltInAtomicType.ANY_ATOMIC))) {

                    Expression cexp = new UntypedAtomicConverter(exp, (AtomicType)reqItemType, false);
                    try {
                        if (exp instanceof Literal) {
                            exp = Literal.makeLiteral(
                                    new SequenceExtent(cexp.iterate(env.makeEarlyEvaluationContext())).simplify());
                        } else {
                            exp = cexp;
                        }
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                }

                // Rule 3a: numeric promotion decimal -> float -> double

                int rt = ((AtomicType)reqItemType).getFingerprint();
                if (rt == StandardNames.XS_DOUBLE || rt == StandardNames.XS_FLOAT) {
                    if (th.relationship(suppliedItemType, BuiltInAtomicType.NUMERIC) != TypeHierarchy.DISJOINT) {
                        exp = new NumericPromoter(exp, (BuiltInAtomicType)reqItemType.getPrimitiveItemType());
                        try {
                            exp = exp.simplify(env).typeCheck(env, AnyItemType.getInstance());
                        } catch (XPathException err) {
                            throw err.makeStatic();
                        }
                        suppliedItemType = (rt == StandardNames.XS_DOUBLE ? BuiltInAtomicType.DOUBLE : BuiltInAtomicType.FLOAT);
                        suppliedCard = -1;
                    }
                }

                // Rule 3b: promotion from anyURI -> string

                if (rt == Type.STRING && th.isSubType(suppliedItemType, BuiltInAtomicType.ANY_URI)) {
                    suppliedItemType = BuiltInAtomicType.STRING;
                    itemTypeOK = true;
                        // we don't generate code to do a run-time type conversion; rather, we rely on
                        // operators and functions that accept a string to also accept an xs:anyURI. This
                        // is straightforward, because anyURIValue is a subclass of StringValue
                }

            }
        }

        // If both the cardinality and item type are statically OK, return now.
        if (itemTypeOK && cardOK) {
            return exp;
        }

        // If we haven't evaluated the cardinality of the supplied expression, do it now
        if (suppliedCard == -1) {
            suppliedCard = exp.getCardinality();
            if (!cardOK) {
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
        }

        // If an empty sequence was explicitly supplied, and empty sequence is allowed,
        // then the item type doesn't matter
        if (cardOK && suppliedCard==StaticProperty.EMPTY) {
            return exp;
        }

        // If the supplied value is () and () isn't allowed, fail now
        if (suppliedCard==StaticProperty.EMPTY && ((reqCard & StaticProperty.ALLOWS_ZERO) == 0) ) {
            StaticError err = new StaticError(
                        "An empty sequence is not allowed as the " + role.getMessage(),
                        getLocator(supplied, role));
            err.setErrorCode(role.getErrorCode());
            err.setIsTypeError(true);
            throw err;
        }

        // Try a static type check. We only throw it out if the call cannot possibly succeed.

        int relation = (itemTypeOK ? TypeHierarchy.SUBSUMED_BY : th.relationship(suppliedItemType, reqItemType));
        if (relation == TypeHierarchy.DISJOINT) {
            // The item types may be disjoint, but if both the supplied and required types permit
            // an empty sequence, we can't raise a static error. Raise a warning instead.
            if (Cardinality.allowsZero(suppliedCard) &&
                    Cardinality.allowsZero(reqCard)) {
                if (suppliedCard != StaticProperty.EMPTY) {
                    String msg = "Required item type of " + role.getMessage() +
                            " is " + reqItemType.toString(env.getNamePool()) +
                            "; supplied value has item type " +
                            suppliedItemType.toString(env.getNamePool()) +
                            ". The expression can succeed only if the supplied value is an empty sequence.";
                    env.issueWarning(msg, getLocator(supplied, role));
                }
            } else {
                StaticError err = new StaticError(
                    "Required item type of " + role.getMessage() +
                        " is " + reqItemType.toString(env.getNamePool()) +
                        "; supplied value has item type " +
                        suppliedItemType.toString(env.getNamePool()),
                    getLocator(supplied, role));
                err.setErrorCode(role.getErrorCode());
                err.setIsTypeError(true);
                throw err;
            }
        }

        // Unless the type is guaranteed to match, add a dynamic type check,
        // unless the value is already known in which case we might as well report
        // the error now.

        if (!(relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY)) {
            if (exp instanceof Literal) {
                StaticError err = new StaticError(
                    "Required item type of " + role.getMessage() +
                        " is " + reqItemType.toString(env.getNamePool()) +
                        "; supplied value has item type " +
                        suppliedItemType.toString(env.getNamePool()),
                    getLocator(supplied, role));
                err.setErrorCode(role.getErrorCode());
                err.setIsTypeError(true);
                throw err;
            }
            Expression cexp = new ItemChecker(exp, reqItemType, role);
            cexp.adoptChildExpression(exp);
            exp = cexp;
        }

        if (!cardOK) {
            if (exp instanceof Literal) {
                StaticError err = new StaticError (
                    "Required cardinality of " + role.getMessage() +
                        " is " + Cardinality.toString(reqCard) +
                        "; supplied value has cardinality " +
                        Cardinality.toString(suppliedCard),
                    getLocator(supplied, role));
                err.setIsTypeError(true);
                err.setErrorCode(role.getErrorCode());
                throw err;
            } else {
                Expression cexp = CardinalityChecker.makeCardinalityChecker(exp, reqCard, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
            }
        }

        return exp;
    }

     /**
     * Check an expression against a required type, modifying it if necessary. This
     * is a variant of the method {@link #staticTypeCheck} used for expressions that
     * declare variables in XQuery. In these contexts, conversions such as numeric
     * type promotion and atomization are not allowed.
     *
     * @param supplied      The expression to be type-checked
     * @param req           The required type for the context in which the expression is used
     * @param role          Information about the role of the subexpression within the
     *                      containing expression, used to provide useful error messages
     * @param env           The static context containing the types being checked. At present
     *                      this is used only to locate a NamePool
     * @return              The original expression if it is type-safe, or the expression
     *                      wrapped in a run-time type checking expression if not.
     * @throws net.sf.saxon.trans.StaticError if the supplied type is statically inconsistent with the
     *                      required type (that is, if they have no common subtype)
     */

    public static Expression strictTypeCheck(Expression supplied,
                                             SequenceType req,
                                             RoleLocator role,
                                             StaticContext env)
    throws StaticError {

        // System.err.println("Strict Type Check on expression (requiredType = " + req + "):"); supplied.display(10);

        Expression exp = supplied;
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        ItemType reqItemType = req.getPrimaryType();
        int reqCard = req.getCardinality();

        ItemType suppliedItemType = null;
            // item type of the supplied expression: null means not yet calculated
        int suppliedCard = -1;
            // cardinality of the supplied expression: -1 means not yet calculated

        boolean cardOK = (reqCard == StaticProperty.ALLOWS_ZERO_OR_MORE);
        // Unless the required cardinality is zero-or-more (no constraints).
        // check the static cardinality of the supplied expression
        if (!cardOK) {
            suppliedCard = exp.getCardinality();
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);
        }

        boolean itemTypeOK = req.getPrimaryType() instanceof AnyItemType;
        // Unless the required item type and content type are ITEM (no constraints)
        // check the static item type against the supplied expression.
        // NOTE: we don't currently do any static inference regarding the content type
        if (!itemTypeOK) {
            suppliedItemType = exp.getItemType(th);
            int relation = th.relationship(reqItemType, suppliedItemType);
            itemTypeOK = relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMES;
        }

        // If both the cardinality and item type are statically OK, return now.
        if (itemTypeOK && cardOK) {
            return exp;
        }

        // If we haven't evaluated the cardinality of the supplied expression, do it now
        if (suppliedCard == -1) {
            if (suppliedItemType instanceof EmptySequenceTest) {
                suppliedCard = StaticProperty.EMPTY;
            } else {
                suppliedCard = exp.getCardinality();
            }
            if (!cardOK) {
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
        }

        // If an empty sequence was explicitly supplied, and empty sequence is allowed,
        // then the item type doesn't matter
        if (cardOK && suppliedCard==StaticProperty.EMPTY) {
            return exp;
        }

        // If we haven't evaluated the item type of the supplied expression, do it now
        if (suppliedItemType == null) {
            suppliedItemType = exp.getItemType(th);
        }

        if (suppliedCard==StaticProperty.EMPTY && ((reqCard & StaticProperty.ALLOWS_ZERO) == 0) ) {
            StaticError err = new StaticError(
                        "An empty sequence is not allowed as the " + role.getMessage(),
                        getLocator(supplied, role));
            err.setErrorCode(role.getErrorCode());
            err.setIsTypeError(true);
            throw err;
        }

        // Try a static type check. We only throw it out if the call cannot possibly succeed.

        int relation = th.relationship(suppliedItemType, reqItemType);
        if (relation == TypeHierarchy.DISJOINT) {
            // The item types may be disjoint, but if both the supplied and required types permit
            // an empty sequence, we can't raise a static error. Raise a warning instead.
            if (Cardinality.allowsZero(suppliedCard) &&
                    Cardinality.allowsZero(reqCard)) {
                if (suppliedCard != StaticProperty.EMPTY) {
                    String msg = "Required item type of " + role.getMessage() +
                            " is " + reqItemType.toString(env.getNamePool()) +
                            "; supplied value has item type " +
                            suppliedItemType.toString(env.getNamePool()) +
                            ". The expression can succeed only if the supplied value is an empty sequence.";
                    env.issueWarning(msg, getLocator(supplied, role));
                }
            } else {
                StaticError err = new StaticError(
                    "Required item type of " + role.getMessage() +
                        " is " + reqItemType.toString(env.getNamePool()) +
                        "; supplied value has item type " +
                        suppliedItemType.toString(env.getNamePool()),
                    getLocator(supplied, role));
                err.setErrorCode(role.getErrorCode());
                err.setIsTypeError(true);
                throw err;
            }
        }

        // Unless the type is guaranteed to match, add a dynamic type check,
        // unless the value is already known in which case we might as well report
        // the error now.

        if (!(relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY)) {
                Expression cexp = new ItemChecker(exp, reqItemType, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
        }

        if (!cardOK) {
            if (exp instanceof Literal) {
                StaticError err = new StaticError (
                    "Required cardinality of " + role.getMessage() +
                        " is " + Cardinality.toString(reqCard) +
                        "; supplied value has cardinality " +
                        Cardinality.toString(suppliedCard),
                    getLocator(supplied, role));
                err.setIsTypeError(true);
                err.setErrorCode(role.getErrorCode());
                throw err;
            } else {
                Expression cexp = CardinalityChecker.makeCardinalityChecker(exp, reqCard, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
            }
        }

        return exp;
    }

    /**
     * Get a SourceLocator given an expression and a role
     */

    private static SourceLocator getLocator(Expression supplied, RoleLocator role) {
        SourceLocator loc = ExpressionTool.getLocator(supplied);
        if (loc == null) {
            loc = role.getSourceLocator();
        }
        return loc;
    }

    /**
     * Test whether a given value conforms to a given type
     * @param val the value
     * @param requiredType the required type
     * @param context
     * @return a DynamicError describing the error condition if the value doesn't conform;
     * or null if it does.
     */

    public static DynamicError testConformance(Value val, SequenceType requiredType, XPathContext context)
    throws XPathException {
        ItemType reqItemType = requiredType.getPrimaryType();
        final Configuration config = context.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        SequenceIterator iter = val.iterate();
        int count = 0;
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            count++;
            if (!reqItemType.matchesItem(item, false, config)) {
                DynamicError err = new DynamicError (
                    "External parameter requires type " + reqItemType +
                    "; supplied value has type " + val.getItemType(th));
                err.setIsTypeError(true);
                return err;
            }
        }

        int reqCardinality = requiredType.getCardinality();
        if (count == 0 && !Cardinality.allowsZero(reqCardinality)) {
            DynamicError err = new DynamicError (
                    "External parameter requires non-empty sequence; supplied value is empty");
            err.setIsTypeError(true);
            return err;
        }
        if (count > 1 && !Cardinality.allowsMany(reqCardinality)) {
            DynamicError err = new DynamicError (
                    "External parameter requires singleton sequence; supplied value contains " + count + " items");
            err.setIsTypeError(true);
            return err;
        }
        if (count > 0 && reqCardinality == StaticProperty.EMPTY) {
            DynamicError err = new DynamicError (
                    "External parameter requires empty sequence; supplied value is non-empty");
            err.setIsTypeError(true);
            return err;
        }
        return null;
    }

    /**
     * Test whether a given expression is capable of returning a value that has an effective boolean
     * value.
     * @return null if the expression is OK (optimistically), an exception object if not
     */

    public static XPathException ebvError(Expression exp, TypeHierarchy th) {
        if (Cardinality.allowsZero(exp.getCardinality())) {
            return null;
        }
        ItemType t = exp.getItemType(th);
        if (th.relationship(t, Type.NODE_TYPE) == TypeHierarchy.DISJOINT &&
                th.relationship(t, BuiltInAtomicType.BOOLEAN) == TypeHierarchy.DISJOINT &&
                th.relationship(t, BuiltInAtomicType.STRING) == TypeHierarchy.DISJOINT &&
                th.relationship(t, BuiltInAtomicType.ANY_URI) == TypeHierarchy.DISJOINT &&
                th.relationship(t, BuiltInAtomicType.UNTYPED_ATOMIC) == TypeHierarchy.DISJOINT &&
                th.relationship(t, BuiltInAtomicType.NUMERIC) == TypeHierarchy.DISJOINT &&
                !(t instanceof ExternalObjectType)) {
            DynamicError err = new DynamicError("Effective boolean value is defined only for sequences containing " +
                    "booleans, strings, numbers, URIs, or nodes");
            err.setErrorCode("FORG0006");
            err.setIsTypeError(true);
            return err;
        }
        return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
