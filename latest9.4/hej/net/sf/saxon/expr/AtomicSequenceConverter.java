package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
 * An AtomicSequenceConverter is an expression that performs a cast on each member of
 * a supplied sequence
 */

public final class AtomicSequenceConverter extends UnaryExpression {

    public static ToStringMappingFunction TO_STRING_MAPPER = new ToStringMappingFunction();

    private PlainType requiredItemType;
    /*@Nullable*/ private Converter converter;
    private boolean allConverted;

    /**
     * Constructor
     *
     * @param sequence         this must be a sequence of atomic values. This is not checked; a ClassCastException
     *                         will occur if the precondition is not satisfied.
     * @param requiredItemType the item type to which all items in the sequence should be converted,
     *                         using the rules for "cast as".
     */

    public AtomicSequenceConverter(Expression sequence, PlainType requiredItemType, boolean allConverted) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        this.allConverted = allConverted;
    }

    /**
     * Create an AtomicSequenceConverter that converts all untypedAtomic values in the input sequence to
     * a specified target type, while leaving items other than untypedAtomic unchanged
     * @param config the Saxon configuration
     * @param operand the expression that delivers the input sequence
     * @param requiredItemType the type to which untypedAtomic values should be cast, which must either be an
     * atomic type or a "plain" union type
     * @return an AtomicSequenceConverter that performs the required conversion
     */


    public static AtomicSequenceConverter makeUntypedSequenceConverter(Configuration config, Expression operand, PlainType requiredItemType) {
        TypeHierarchy th = config.getTypeHierarchy();
        AtomicSequenceConverter atomicSeqConverter =
                new AtomicSequenceConverter(operand, requiredItemType, operand.getItemType(th) == BuiltInAtomicType.UNTYPED_ATOMIC);
        final ConversionRules rules = config.getConversionRules();
        final Converter untypedConverter;
        if (requiredItemType.isAtomicType()) {
            untypedConverter = rules.getConverter(BuiltInAtomicType.UNTYPED_ATOMIC, (AtomicType)requiredItemType);
        } else {
            untypedConverter = new StringConverter.StringToUnionConverter(requiredItemType, rules);
        }
        // source type not known statically; create a converter that decides at run-time
        Converter converter = new UntypedConverter(rules, untypedConverter);
        atomicSeqConverter.setConverter(converter);
        return atomicSeqConverter;
    }


    public static class UntypedConverter extends Converter {
        Converter untypedConverter = null;

        public UntypedConverter(ConversionRules rules, Converter converter) {
            super(rules);
            untypedConverter = converter;
            //untypedConverter.setConversionRules(rules);

        }

        /*@NotNull*/
        @Override
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof UntypedAtomicValue) {
                return untypedConverter.convert(input);
            } else {
                return input;
            }
        }
    }

    public static AtomicSequenceConverter makeUntypedSequenceRejector(Configuration config, final Expression operand, final PlainType requiredItemType) {
        TypeHierarchy th = config.getTypeHierarchy();
        AtomicSequenceConverter atomicSeqConverter = new AtomicSequenceConverter(operand, requiredItemType, operand.getItemType(th) == BuiltInAtomicType.UNTYPED_ATOMIC);
        final ConversionRules rules = config.getConversionRules();
        final Converter untypedConverter = new Converter() {
            // called when an untyped atomic value is encountered
            public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
                ValidationFailure vf = new ValidationFailure(
                        "Implicit conversion of untypedAtomic value to " + requiredItemType.toString() + " is not allowed");
                vf.setErrorCode("XPTY0117");
                vf.setLocator(operand);
                return vf;
            }
        };

        // source type not known statically; create a converter that decides at run-time
        Converter converter = new UntypedConverter(rules, untypedConverter);
        atomicSeqConverter.setConverter(converter);
        return atomicSeqConverter;
    }

    public void allocateConverter(Configuration config) {

        ItemType sourceType = operand.getItemType(config.getTypeHierarchy());
        final ConversionRules rules = config.getConversionRules();
        if (sourceType instanceof EmptySequenceTest) {
            converter = Converter.IDENTITY_CONVERTER;
        } else if (!(sourceType instanceof AtomicType)) {
            converter = null;
        } else if (requiredItemType instanceof AtomicType) {
            converter = rules.getConverter((AtomicType) sourceType, (AtomicType)requiredItemType);
        } else if (((SimpleType)requiredItemType).isUnionType()) {
            converter = new StringConverter.StringToUnionConverter(requiredItemType, rules);
        }

        if (converter == null) {
            // source type not known statically; create a converter that decides at run-time
            converter = new Converter(rules) {
                /*@NotNull*/
                public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
                    Converter converter = rules.getConverter(input.getPrimitiveType(), (AtomicType)requiredItemType);
                    if (converter == null) {
                        return new ValidationFailure("Cannot convert value from " + input.getPrimitiveType() + " to " + requiredItemType);
                    } else {
                        return converter.convert(input);
                    }
                }
            };
        }

    }

    /**
     * Get the converter used to convert the items in the sequence
     *
     * @return the converter. Note that a converter is always allocated during the typeCheck() phase,
     *         even if the source type is not known.
     */

    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    /**
     * Ask if all items in the input are converted to the target type
     */

    public boolean isAllItemsConverted() {
        return allConverted;
    }

    /**
     * Simplify an expression
     *
     * @param visitor an expression visitor
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal && requiredItemType instanceof AtomicType) {
            if (Literal.isEmptySequence(operand)) {
                return new Literal(EmptySequence.getInstance());
            }
            Configuration config = visitor.getConfiguration();
            AtomicType sourceType = (AtomicType) operand.getItemType(config.getTypeHierarchy());
            converter = config.getConversionRules().getConverter(
                    sourceType, (AtomicType)requiredItemType);
            ValueRepresentation val = SequenceExtent.makeSequenceExtent(
                    iterate(visitor.getStaticContext().makeEarlyEvaluationContext()));
            return Literal.makeLiteral(Value.asValue(val));
        }
        return this;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        Configuration config = visitor.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        if (th.isSubType(operand.getItemType(th), requiredItemType)) {
            return operand;
        } else {
            if (converter == null) {
                allocateConverter(config);
            }
            return this;
        }
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     */
    /*@NotNull*/
    @Override
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (operand instanceof AtomicSequenceConverter) {
            AtomicSequenceConverter asc = (AtomicSequenceConverter)operand;
            ItemType ascType = asc.getItemType(th);
            if (ascType == requiredItemType) {
                return operand;
            } else if ((requiredItemType == BuiltInAtomicType.STRING || requiredItemType == BuiltInAtomicType.UNTYPED_ATOMIC) &&
                        (ascType == BuiltInAtomicType.STRING || ascType == BuiltInAtomicType.UNTYPED_ATOMIC)) {
                AtomicSequenceConverter old = (AtomicSequenceConverter)operand;
                AtomicSequenceConverter asc2 = new AtomicSequenceConverter(
                        old.getBaseExpression(),
                        requiredItemType,
                        old.isAllItemsConverted());
                return asc2.typeCheck(visitor, contextItemType)
                        .optimize(visitor, contextItemType);
            }
        }
        return this;

    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties() | StaticProperty.NON_CREATIVE;
        if (converter instanceof UntypedConverter) {
            return p | StaticProperty.NOT_UNTYPED_ATOMIC;
        } else {
            return p;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        AtomicSequenceConverter atomicConverter = new AtomicSequenceConverter(getBaseExpression().copy(), requiredItemType, allConverted);
        atomicConverter.setConverter(converter);
        return atomicConverter;
    }

    /**
     * Iterate over the sequence of values
     */

    /*@NotNull*/
    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        if (converter == Converter.TO_STRING) {
            return new ItemMappingIterator(base, TO_STRING_MAPPER, true);
        } else {
            AtomicSequenceMappingFunction mapper = new AtomicSequenceMappingFunction();
            mapper.setConverter(converter);
            return new ItemMappingIterator(base, mapper, true);
        }
    }

    /**
     * Mapping function wrapped around a converter
     */

    public static class AtomicSequenceMappingFunction
            implements ItemMappingFunction<AtomicValue, AtomicValue> {
        private Converter converter;

        public void setConverter(Converter converter) {
            this.converter = converter;
        }

        public AtomicValue mapItem(AtomicValue item) throws XPathException {
            return converter.convert(item).asAtomic();
        }
    }

    /**
     * Mapping function that converts every item in a sequence to a string
     */

    public static class ToStringMappingFunction
            implements ItemMappingFunction<Item, StringValue> {
        public StringValue mapItem(Item item) throws XPathException {
            return StringValue.makeStringValue(item.getStringValueCS());
        }
    }


    /**
     * Evaluate as an Item. This should only be called if the AtomicSequenceConverter has cardinality zero-or-one
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item == null) {
            return null;
        }
        return converter.convert((AtomicValue) item).asAtomic();
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @param th the type hierarchy cache
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        if (allConverted) {
            return requiredItemType;
        } else {
            return Type.getCommonSuperType(requiredItemType, operand.getItemType(th), th);
        }
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        return operand.getCardinality();
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType.equals(((AtomicSequenceConverter) other).requiredItemType);
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ requiredItemType.hashCode();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("convertItems");
        destination.emitAttribute("to", requiredItemType.toString(destination.getNamePool()));
        operand.explain(destination);
        destination.endElement();
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
