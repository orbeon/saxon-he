////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GeneralComparison10: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This version of the class implements general comparisons
 * in XPath 1.0 backwards compatibility mode, as defined in the Oct 2004 revision
 * of the specifications.
*/

public class GeneralComparison10 extends BinaryExpression implements Callable {

    protected int singletonOperator;
    protected AtomicComparer comparer;
    private boolean atomize0 = true;
    private boolean atomize1 = true;
    private boolean maybeBoolean0 = true;
    private boolean maybeBoolean1 = true;

    /**
    * Create a general comparison identifying the two operands and the operator
    * @param p0 the left-hand operand
    * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
    * @param p1 the right-hand operand
    */

    public GeneralComparison10(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
        singletonOperator = GeneralComparison.getCorrespondingSingletonOperator(op);
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {

        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        StaticContext env = visitor.getStaticContext();
        StringCollator comp = env.getCollation(env.getDefaultCollationName());
        if (comp==null) {
            comp = CodepointCollator.getInstance();
        }

        XPathContext context = env.makeEarlyEvaluationContext();
        comparer = new GenericAtomicComparer(comp, context);

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral((AtomicValue)evaluateItem(context));
        }

        return this;
    }

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /**
    * Optimize the expression
    * @return the checked expression
    */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        StaticContext env = visitor.getStaticContext();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        // Neither operand needs to be sorted

        operand0 = ExpressionTool.unsorted(opt, operand0, false);
        operand1 = ExpressionTool.unsorted(opt, operand1, false);

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral(
                    (AtomicValue)evaluateItem(env.makeEarlyEvaluationContext()));
        }

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        ItemType type0 = operand0.getItemType(th);
        ItemType type1 = operand1.getItemType(th);

        if (type0.isPlainType()) {
            atomize0 = false;
        }
        if (type1.isPlainType()) {
            atomize1 = false;
        }

        if (th.relationship(type0, BuiltInAtomicType.BOOLEAN) == TypeHierarchy.DISJOINT) {
            maybeBoolean0 = false;
        }
        if (th.relationship(type1, BuiltInAtomicType.BOOLEAN) == TypeHierarchy.DISJOINT) {
            maybeBoolean1 = false;
        }

        if (!maybeBoolean0 && !maybeBoolean1) {
            int n0 = th.relationship(type0, BuiltInAtomicType.NUMERIC);
            int n1 = th.relationship(type1, BuiltInAtomicType.NUMERIC);
            boolean maybeNumeric0 = (n0 != TypeHierarchy.DISJOINT);
            boolean maybeNumeric1 = (n1 != TypeHierarchy.DISJOINT);
            boolean numeric0 = (n0 == TypeHierarchy.SUBSUMED_BY || n0 == TypeHierarchy.SAME_TYPE);
            boolean numeric1 = (n1 == TypeHierarchy.SUBSUMED_BY || n1 == TypeHierarchy.SAME_TYPE);
            // Use the 2.0 path if we don't have to deal with the possibility of boolean values,
            // or the complications of converting values to numbers
            if (operator == Token.EQUALS || operator == Token.NE) {
                if ((!maybeNumeric0 && !maybeNumeric1) || (numeric0 && numeric1)) {
                    GeneralComparison gc = new GeneralComparison20(operand0, operator, operand1);
                    BinaryExpression binExp = visitor.getConfiguration().obtainOptimizer().optimizeGeneralComparison(gc, false);
                    ExpressionTool.copyLocationInfo(this, binExp);
                    return visitor.optimize(visitor.typeCheck(binExp, contextItemType), contextItemType);
                }
            } else if (numeric0 && numeric1) {
                GeneralComparison gc = new GeneralComparison20(operand0, operator, operand1);
                BinaryExpression binExp = visitor.getConfiguration().obtainOptimizer().optimizeGeneralComparison(gc, false);
                ExpressionTool.copyLocationInfo(this, binExp);
                return visitor.optimize(visitor.typeCheck(binExp, contextItemType), contextItemType);
            }
        }

        return this;
    }



    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

   /**
     * Evaluate the expression: interface for use by compiled bytecode
     *
    *
    * @param context the dynamic evaluation context
    * @param arguments the values of the arguments, supplied as SequenceIterators
    * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws XPathException if a dynamic error occurs during the evaluation of the expression
     */

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(arguments[0].iterate(), arguments[1].iterate(), context));
    }

    /**
    * Evaluate the expression giving a boolean result
    * @param context the given context for evaluation
    * @return a boolean representing the result of the comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return effectiveBooleanValue(operand0.iterate(context), operand1.iterate(context), context);
    }

    /**
     * Evaluate the expression giving a boolean result
     * @param iter0 iterator over the value of the first argument
     * @param iter1 iterator over the value of the second argument
     * @param context the given context for evaluation
     * @return a boolean representing the result of the comparison of the two operands
    */

    private boolean effectiveBooleanValue(SequenceIterator iter0, SequenceIterator iter1, XPathContext context)
    throws XPathException {

        // If the first operand is a singleton boolean,
        // compare it with the effective boolean value of the other operand

        boolean iter0used = false;
        boolean iter1used = false;

        if (maybeBoolean0) {
            iter0used = true;
            Item i01 = iter0.next();
            Item i02 = (i01 == null ? null : iter0.next());
            if (i01 instanceof BooleanValue && i02 == null) {
                iter0.close();
                boolean b = ExpressionTool.effectiveBooleanValue(iter1);
                return compare((BooleanValue)i01, singletonOperator, BooleanValue.get(b), comparer, context);
            }
            if (i01 == null && !maybeBoolean1) {
                iter0.close();
                return false;
            }
        }

        // If the second operand is a singleton boolean,
        // compare it with the effective boolean value of the other operand

        if (maybeBoolean1) {
            iter1used = true;
            Item i11 = iter1.next();
            Item i12 = (i11 == null ? null : iter1.next());
            if (i11 instanceof BooleanValue && i12 == null) {
                iter1.close();
                if (iter0used) {
                    iter0 = iter0.getAnother();
                }
                boolean b = ExpressionTool.effectiveBooleanValue(iter0);
                return compare(BooleanValue.get(b), singletonOperator, (BooleanValue)i11, comparer, context);
            }
            if (i11 == null && !maybeBoolean0) {
                iter1.close();
                return false;
            }
        }

        // Atomize both operands where necessary

        if (iter0used) {
            iter0 = iter0.getAnother();
        }

        if (iter1used) {
            iter1 = iter1.getAnother();
        }

        if (atomize0) {
            iter0 = Atomizer.getAtomizingIterator(iter0, false);
        }

        if (atomize1) {
            iter1 = Atomizer.getAtomizingIterator(iter1, false);
        }

        // If the operator is one of <, >, <=, >=, then convert both operands to sequences of xs:double
        // using the number() function

        if (operator == Token.LT || operator == Token.LE || operator == Token.GT || operator == Token.GE) {
            final Configuration config = context.getConfiguration();
            ItemMappingFunction map = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    return NumberFn.convert((AtomicValue)item, config);
                }
            };
            iter0 = new ItemMappingIterator(iter0, map, true);
            iter1 = new ItemMappingIterator(iter1, map, true);
        }

        // Compare all pairs of atomic values in the two atomized sequences

        List seq1 = null;
        while (true) {
            AtomicValue item0 = (AtomicValue)iter0.next();
            if (item0 == null) {
                return false;
            }
            if (iter1 != null) {
                while (true) {
                    AtomicValue item1 = (AtomicValue)iter1.next();
                    if (item1 == null) {
                        iter1 = null;
                        if (seq1 == null) {
                            // second operand is empty
                            return false;
                        }
                        break;
                    }
                    try {
                        if (compare(item0, singletonOperator, item1, comparer, context)) {
                            return true;
                        }
                        if (seq1 == null) {
                            seq1 = new ArrayList(40);
                        }
                        seq1.add(item1);
                    } catch (XPathException e) {
                        // re-throw the exception with location information added
                        e.maybeSetLocation(this);
                        e.maybeSetContext(context);
                        throw e;
                    }
                }
            } else {
                //assert seq1 != null;
                Iterator listIter1 = seq1.iterator();
                while (listIter1.hasNext()) {
                    AtomicValue item1 = (AtomicValue)listIter1.next();
                    if (compare(item0, singletonOperator, item1, comparer, context)) {
                        return true;
                    }
                }
            }
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        GeneralComparison10 gc = new GeneralComparison10(operand0.copy(), operator, operand1.copy());
        gc.comparer = comparer;
        gc.atomize0 = atomize0;
        gc.atomize1 = atomize1;
        gc.maybeBoolean0 = maybeBoolean0;
        gc.maybeBoolean1 = maybeBoolean1;
        return gc;
    }

    /**
    * Compare two atomic values
     * @param a0 the first value to be compared
     * @param operator the comparison operator
     * @param a1 the second value to be compared
     * @param comparer the comparer to be used (perhaps including a collation)
     * @param context the XPath dynamic context
     * @return the result of the comparison
    */

    private static boolean compare(AtomicValue a0,
                                     int operator,
                                     AtomicValue a1,
                                     AtomicComparer comparer,
                                     XPathContext context) throws XPathException {

        comparer = comparer.provideContext(context);
        ConversionRules rules = context.getConfiguration().getConversionRules();

        BuiltInAtomicType t0 = a0.getPrimitiveType();
        BuiltInAtomicType t1 = a1.getPrimitiveType();

        // If either operand is a number, convert both operands to xs:double using
        // the rules of the number() function, and compare them

        if (t0.isPrimitiveNumeric() || t1.isPrimitiveNumeric()) {
            DoubleValue v0 = NumberFn.convert(a0, context.getConfiguration());
            DoubleValue v1 = NumberFn.convert(a1, context.getConfiguration());
            return ValueComparison.compare(v0, operator, v1, comparer, false);
        }

        // If either operand is a string, or if both are untyped atomic, convert
        // both operands to strings and compare them

        if (t0.equals(BuiltInAtomicType.STRING) || t1.equals(BuiltInAtomicType.STRING) ||
                (t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC) && t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC))) {
            StringValue s0 = StringValue.makeStringValue(a0.getStringValueCS());
            StringValue s1 = StringValue.makeStringValue(a1.getStringValueCS());
            return ValueComparison.compare(s0, operator, s1, comparer, false);
        }

        // If either operand is untyped atomic,
        // convert it to the type of the other operand, and compare

        if (t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            a0 = rules.getStringConverter(t1).convert(a0).asAtomic();
        }

        if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            a1 = rules.getStringConverter(t0).convert(a1).asAtomic();
        }

        return ValueComparison.compare(a0, operator, a1, comparer, false);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }



//    protected String displayOperator() {
//        return "many-to-many (1.0) " + super.displayOperator();
//    }

    protected void explainExtraAttributes(ExpressionPresenter out) {
        out.emitAttribute("cardinality", "many-to-many (1.0)");
    }
    
}

