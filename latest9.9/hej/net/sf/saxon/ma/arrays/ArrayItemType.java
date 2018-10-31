////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.om.Genre;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceType;

import java.util.Optional;
import java.util.function.Function;

/**
 * An instance of this class represents a specific array item type, for example
 * function(xs:int) as xs:boolean
 */
public class ArrayItemType extends AnyFunctionType {

    public final static ArrayItemType ANY_ARRAY_TYPE = new ArrayItemType(SequenceType.ANY_SEQUENCE);

    public final static SequenceType SINGLE_ARRAY =
            SequenceType.makeSequenceType(ArrayItemType.ANY_ARRAY_TYPE, StaticProperty.EXACTLY_ONE);

    private SequenceType memberType;

    public ArrayItemType(SequenceType memberType) {
        this.memberType = memberType;
    }

    /**
     * Determine the Genre (top-level classification) of this type
     *
     * @return the Genre to which this type belongs, specifically {@link Genre#ARRAY}
     */
    @Override
    public Genre getGenre() {
        return Genre.ARRAY;
    }

    /**
     * Get the type of the members of the array
     * @return the type to which all members of the array must conform
     */

    public SequenceType getMemberType() {
        return memberType;
    }

    /**
     * Ask whether this function item type is a map type. In this case function coercion (to the map type)
     * will never succeed.
     *
     * @return true if this FunctionItemType is a map type
     */
    @Override
    public boolean isMapType() {
        return false;
    }

    /**
     * Ask whether this function item type is an array type. In this case function coercion (to the array type)
     * will never succeed.
     *
     * @return true if this FunctionItemType is an array type
     */
    @Override
    public boolean isArrayType() {
        return true;
    }

    /**
     * Ask whether values of this type are atomizable
     *
     * @return true unless it is known that these items will be elements with element-only
     *         content, in which case return false
     * @param th  The type hierarchy cache
     */
    @Override
    public boolean isAtomizable(TypeHierarchy th) {
        return true;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     *
     * @return the item type of the atomic values that will be produced when an item
     *         of this type is atomized
     */
    @Override
    public PlainType getAtomizedItemType() {
        return memberType.getPrimaryType().getAtomizedItemType();
    }

    /**
     * Get the arity (number of arguments) of this function type
     *
     * @return the number of argument types in the function signature
     */

    public int getArity() {
        return 1;
    }

    /**
     * Get the argument types of this array, viewed as a function
     *
     * @return the list of argument types of this array, viewed as a function
     */

    public SequenceType[] getArgumentTypes() {
        // regardless of the key type, a function call on this map can supply any atomic value
        return new SequenceType[]{BuiltInAtomicType.INTEGER.one()};
    }

    /**
     * Test whether a given item conforms to this type
     *
     * @param item The item to be tested
     * @param th  The type hierarchy cache
     * @return true if the item is an instance of this type; false otherwise
     */
    @Override
    public boolean matches(Item item, TypeHierarchy th) throws XPathException{
        if (!(item instanceof ArrayItem)) {
            return false;
        }
        if (this == ANY_ARRAY_TYPE) {
            return true;
        } else {
            for (Sequence s : ((ArrayItem) item).members()){
                if (!memberType.matches(s, th)){
                    return false;
                }
            }
            return  true;
        }
    }

    /**
     * Get the result type of this array, viewed as a function
     *
     * @return the result type of this array, viewed as a function
     */

    public SequenceType getResultType() {
        return memberType;
    }

    /**
     * Produce a representation of this type name for use in error messages.
     *
     * @return a string representation of the type, in notation resembling but not necessarily
     *         identical to XPath syntax
     */
    public String toString() {
        return makeString(SequenceType::toString);
    }

    private String makeString(Function<SequenceType, String> show) {
        if (this.equals(ANY_ARRAY_TYPE)) {
            return "array(*)";
        } else {
            FastStringBuffer sb = new FastStringBuffer(100);
            sb.append("array(");
            sb.append(show.apply(memberType));
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * Return a string representation of this ItemType suitable for use in stylesheet
     * export files. This differs from the result of toString() in that it will not contain
     * any references to anonymous types. Note that it may also use the Saxon extended syntax
     * for union types and tuple types. The default implementation returns the result of
     * calling {@code toString()}.
     *
     * @return the string representation as an instance of the XPath SequenceType construct
     */
    @Override
    public String toExportString() {
        return makeString(SequenceType::toExportString);
    }

    /**
     * Test whether this array type equals another array type
     */

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof ArrayItemType) {
            ArrayItemType f2 = (ArrayItemType) other;
            return memberType.equals(f2.memberType);
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return memberType.hashCode();
    }

    /**
     * Determine the relationship of one function item type to another
     *
     * @return for example {@link net.sf.saxon.type.TypeHierarchy#SUBSUMES}, {@link net.sf.saxon.type.TypeHierarchy#SAME_TYPE}
     */

    public int relationship(FunctionItemType other, TypeHierarchy th) {
        if (other == AnyFunctionType.getInstance()) {
            return TypeHierarchy.SUBSUMED_BY;
        } else if (equals(other)) {
            return TypeHierarchy.SAME_TYPE;
        } else if (other == ArrayItemType.ANY_ARRAY_TYPE) {
            return TypeHierarchy.SUBSUMED_BY;
        } else if (other.isMapType()){
            return TypeHierarchy.DISJOINT;
        } else if (other instanceof ArrayItemType) {
            // See bug 3720. Array types are never disjoint, because the empty array
            // is an instance of every array type
            ArrayItemType f2 = (ArrayItemType) other;
            int rel = th.sequenceTypeRelationship(memberType, f2.memberType);
            return rel==TypeHierarchy.DISJOINT ? TypeHierarchy.OVERLAPS : rel;
        } else {
            int rel = TypeHierarchy.DISJOINT;
            //#ifdefined HOF
            rel = new SpecificFunctionType(getArgumentTypes(), getResultType()).relationship(other, th);
            if (rel == TypeHierarchy.SUBSUMES || rel == TypeHierarchy.SAME_TYPE) {
                rel = TypeHierarchy.OVERLAPS;
            }
            //#endif
            return rel;
        }
    }

    @Override
    public Expression makeFunctionSequenceCoercer(Expression exp, RoleDiagnostic role)
            throws XPathException {
        Expression result = exp;
        //#ifdefined HOF                                                          ~
        result = new SpecificFunctionType(getArgumentTypes(), getResultType()).makeFunctionSequenceCoercer(exp, role);
        //#endif
        return result;
    }

    /**
     * Get extra diagnostic information about why a supplied item does not conform to this
     * item type, if available. If extra information is returned, it should be in the form of a complete
     * sentence, minus the closing full stop. No information should be returned for obvious cases.
     *
     * @param item the item that doesn't match this type
     * @param th the type hierarchy cache
     * @return optionally, a message explaining why the item does not match the type
     */
    @Override
    public Optional<String> explainMismatch(Item item, TypeHierarchy th) {
        if (item instanceof ArrayItem) {
            for (int i=0; i<((ArrayItem)item).arrayLength(); i++) {
                try {
                    GroundedValue<?> member = ((ArrayItem) item).get(i);
                    if (!memberType.matches(member, th)) {
                        String s = "The " + RoleDiagnostic.ordinal(i+1) +
                                " member of the supplied array {" +
                                Err.depictSequence(member) +
                                "} does not match the required member type " +
                                memberType;
                        Optional<String> more = memberType.explainMismatch(member, th);
                        if (more.isPresent()) {
                            s = s + ". " + more.get();
                        }
                        return Optional.of(s);
                    }
                } catch (XPathException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Generate Javascript code to test whether an item conforms to this item type
     *
     * @param knownToBe a type that this item is known to conform to
     * @param targetVersion the version of Saxon-JS SEF format
     * @return a Javascript instruction or sequence of instructions, which can be used as the body
     * of a Javascript function, and which returns a boolean indication whether the value of the
     * variable "item" is an instance of this item type.
     * @throws XPathException if JS code cannot be generated for this item type, for example because
     *                        the test is schema-aware.
     */
    @Override
    public String generateJavaScriptItemTypeTest(ItemType knownToBe, int targetVersion) throws XPathException {
        if (this == ANY_ARRAY_TYPE) {
            return "return SaxonJS.U.isArray(item)";
        } else if (targetVersion == 1) {
            FastStringBuffer fsb = new FastStringBuffer(256);
            fsb.append("function v(item) {" + memberType.getPrimaryType().generateJavaScriptItemTypeTest(AnyItemType.getInstance(), targetVersion) + "};");
            fsb.append(Cardinality.generateJavaScriptChecker(memberType.getCardinality()));
            fsb.append("return SaxonJS.U.isArray(item) && " +
                               "SaxonJS.U.ForArray(item.value).every(function(seq){return c(seq.length) && SaxonJS.U.ForArray(seq).every(v)});");
            return fsb.toString();
        } else {
            // targetVersion == 2
            FastStringBuffer fsb = new FastStringBuffer(256);
            fsb.append("function v(item) {" + memberType.getPrimaryType().generateJavaScriptItemTypeTest(AnyItemType.getInstance(), targetVersion) + "};");
            fsb.append(Cardinality.generateJavaScriptChecker(memberType.getCardinality()));
            fsb.append("return SaxonJS.U.isConstrainedArray(item, v, c);");
            return fsb.toString();

        }
    }
}

// Copyright (c) 2015-2016 Saxonica Limited.