////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;


import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LiteralCompiler;
import com.saxonica.ee.stream.adjunct.LiteralAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import com.saxonica.functions.hof.FunctionLiteral;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeTestPattern;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;


/**
 * A Literal is an expression whose value is constant: it is a class that implements the {@link Expression}
 * interface as a wrapper around a {@link GroundedValue}. This may derive from an actual literal in an XPath expression
 * or query, or it may be the result of evaluating a constant subexpression such as true() or xs:date('2007-01-16')
 */

public class Literal extends Expression {

    private GroundedValue value;

    /**
     * Create a literal as a wrapper around a Value
     *
     * @param value     the value of this literal
     * @param container the container in the expression tree. Should not be null.
     *                  Saxon does not reject a null value for the container, but it may cause
     *                  subsequent failures if a container is not supplied at some stage.
     */

    protected Literal(GroundedValue value, Container container) {
        this.value = value.reduce();
        setContainer(container);
    }

    /**
     * Get the value represented by this Literal
     *
     * @return the constant value
     */

    public GroundedValue getValue() {
        return value;
    }

    /**
     * TypeCheck an expression
     *
     * @return for a Value, this always returns the value unchanged
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        return this;
    }

    /**
     * Optimize an expression
     *
     * @return for a Value, this always returns the value unchanged
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        return this;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @return for the default implementation: AnyItemType (not known)
     */

    /*@NotNull*/
    public ItemType getItemType() {
        // Avoid getting the configuration if we can: it's a common source of NPE's
        if (value instanceof AtomicValue) {
            return ((AtomicValue) value).getItemType();
        } else if (value.getLength() == 0) {
            return ErrorType.getInstance();
        } else {
            TypeHierarchy th = getConfiguration().getTypeHierarchy();
            return SequenceTool.getItemType(value, th);
        }
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        if (value.getLength() == 0) {
            return StaticProperty.EMPTY;
        } else if (value instanceof AtomicValue) {
            return StaticProperty.EXACTLY_ONE;
        }
        try {
            SequenceIterator iter = value.iterate();
            Item next = iter.next();
            if (next == null) {
                return StaticProperty.EMPTY;
            } else {
                if (iter.next() != null) {
                    return StaticProperty.ALLOWS_ONE_OR_MORE;
                } else {
                    return StaticProperty.EXACTLY_ONE;
                }
            }
        } catch (XPathException err) {
            // can't actually happen
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Compute the static properties of this expression (other than its type). For a
     * Value, the only special property is {@link StaticProperty#NON_CREATIVE}.
     *
     * @return the value {@link StaticProperty#NON_CREATIVE}
     */


    public int computeSpecialProperties() {
        if (value.getLength() == 0) {
            // An empty sequence has all special properties except "has side effects".
            return StaticProperty.SPECIAL_PROPERTY_MASK & ~StaticProperty.HAS_SIDE_EFFECTS;
        }
        return StaticProperty.NON_CREATIVE;
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    /*@Nullable*/
    @Override
    public IntegerValue[] getIntegerBounds() {
        if (value instanceof IntegerValue) {
            return new IntegerValue[]{(IntegerValue) value, (IntegerValue) value};
        } else if (value instanceof IntegerRange) {
            return new IntegerValue[]{
                    Int64Value.makeIntegerValue(((IntegerRange) value).getStart()),
                    Int64Value.makeIntegerValue(((IntegerRange) value).getEnd())};
        } else {
            return null;
        }
    }

    /**
     * Determine whether this is a vacuous expression as defined in the XQuery update specification
     *
     * @return true if this expression is vacuous
     */

    public boolean isVacuousExpression() {
        return value.getLength() == 0;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return a copy of the original literal. Note that the
     *         underlying value is not copied; the code relies on the caller
     *         treating the underlying value as immutable.
     */

    /*@NotNull*/
    public Expression copy() {
        return new Literal(value, getContainer());
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        if (isEmptySequence(this)) {
            return new NodeTestPattern(ErrorType.getInstance());
        } else {
            return super.toPattern(config, is30);
        }
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the set of nodes within the path map
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return pathMapNodeSet;
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
     * StaticProperty.CURRENT_NODE
     *
     * @return for a Value, this always returns zero.
     */

    public final int getDependencies() {
        return 0;
    }


    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return value.iterate();
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    /*@NotNull*/
    public SequenceIterator iterate() throws XPathException {
        return value.iterate();
    }

    /**
     * Evaluate as a singleton item (or empty sequence). Note: this implementation returns
     * the first item in the sequence. The method should not be used unless appropriate type-checking
     * has been done to ensure that the value will be a singleton.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return value.head();
    }


    /**
     * Process the value as an instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = value.iterate();
        SequenceReceiver out = context.getReceiver();
        Item it;
        while ((it = iter.next()) != null) {
            out.append(it, 0, NodeInfo.ALL_NAMESPACES);
        }
    }

    /*
      * Evaluate an expression as a String. This function must only be called in contexts
      * where it is known that the expression will return a single string (or where an empty sequence
      * is to be treated as a zero-length string). Implementations should not attempt to convert
      * the result to a string, other than converting () to "". This method is used mainly to
      * evaluate expressions produced by compiling an attribute value template.
      *
      * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
      *     expression
      * @exception ClassCastException if the result type of the
      *     expression is not xs:string?
      * @param context The context in which the expression is to be evaluated
      * @return the value of the expression, evaluated in the current context.
      *     The expression must return a string or (); if the value of the
      *     expression is (), this method returns "".
      */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) evaluateItem(context);
        if (value == null) {
            return "";
        }
        return value.getStringValueCS();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return value.effectiveBooleanValue();
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException. The implementation for a literal representing
     * an empty sequence, however, is a no-op.
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        if (value.getLength() == 0) {
            // do nothing
        } else {
            super.evaluatePendingUpdates(context, pul);
        }
    }

    /**
     * Determine whether two literals are equal, when considered as expressions.
     *
     * @param obj the other expression
     * @return true if the two literals are equal. The test here requires (a) identity in the
     *         sense defined by XML Schema (same value in the same value space), and (b) identical type
     *         annotations. For example the literal xs:int(3) is not equal (as an expression) to xs:short(3),
     *         because the two expressions are not interchangeable.
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof Literal)) {
            return false;
        }
        GroundedValue v0 = value;
        GroundedValue v1 = ((Literal) obj).value;
        try {
            SequenceIterator i0 = v0.iterate();
            SequenceIterator i1 = v1.iterate();
            while (true) {
                Item m0 = i0.next();
                Item m1 = i1.next();
                if (m0 == null && m1 == null) {
                    return true;
                }
                if (m0 == null || m1 == null) {
                    return false;
                }
                boolean n0 = m0 instanceof NodeInfo;
                boolean n1 = m1 instanceof NodeInfo;
                if (n0 != n1) {
                    return false;
                }
                if (n0 && n1 && !((NodeInfo) m0).isSameNodeInfo((NodeInfo) m1)) {
                    return false;
                }
                if (m0 instanceof AtomicValue && m1 instanceof AtomicValue) {
                    if (!((AtomicValue) m0).isIdentical((AtomicValue) m1) ||
                            ((AtomicValue) m0).getItemType() != ((AtomicValue) m1).getItemType()) {
                        return false;
                    }
                }
            }
        } catch (XPathException err) {
            return false;
        }
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        if (value instanceof AtomicSequence) {
            return ((AtomicSequence) value).getSchemaComparable().hashCode();
        } else {
            return super.hashCode();
        }
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return value.toString();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("literal");
        if (value.getLength() == 0) {
            out.emitAttribute("value", "()");
        } else if (value instanceof AtomicValue) {
            //noinspection RedundantCast
            out.emitAttribute("value", ((AtomicValue) value).getStringValue());
            // cast is needed to tell the compiler there's no exception possible
            out.emitAttribute("type", ((AtomicValue) value).getItemType().getDisplayName());
        } else {
            try {
                out.emitAttribute("count", value.getLength() + "");
                if (value.getLength() < 20) {
                    SequenceIterator iter = iterate();
                    while (true) {
                        Item it = iter.next();
                        if (it == null) {
                            break;
                        }
                        if (it instanceof NodeInfo) {
                            out.startElement("node");
                            out.emitAttribute("path", Navigator.getPath((NodeInfo) it));
                            out.emitAttribute("uri", ((NodeInfo) it).getSystemId());
                            out.endElement();
                        } else if (it instanceof AtomicValue) {
                            out.startElement("atomicValue");
                            out.emitAttribute("value", it.getStringValue());
                            out.emitAttribute("type", ((AtomicValue) it).getItemType().getDisplayName());
                            out.endElement();
                        } else if (it instanceof FunctionItem) {
                            out.startElement("functionItem");
                            if (((FunctionItem) it).getFunctionName() != null) {
                                out.emitAttribute("name", ((FunctionItem) it).getFunctionName().getDisplayName());
                            }
                            out.emitAttribute("arity", "" + ((FunctionItem) it).getArity());
                            out.endElement();
                        } else if (it instanceof ObjectValue) {
                            out.startElement("externalObject");
                            out.emitAttribute("class", ((ObjectValue) it).getObject().getClass().getName());
                            out.endElement();
                        }
                    }
                }
            } catch (XPathException err) {
                //
            }
        }
        out.endElement();
    }

    /**
     * Produce a short string identifying the expression for use in error messages
     *
     * @return a short string, sufficient to identify the expression
     */
    @Override
    public String toShortString() {
        if (value.getLength() == 0) {
            return "()";
        } else if (value instanceof AtomicValue) {
            return value.toString();
        } else {
            return "(" + value.head().toString() + ",...)";
        }
    }

    /**
     * Test whether the literal wraps an atomic value. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to contain
     * a single atomic value).
     *
     * @param exp an expression
     * @return true if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isAtomic(Expression exp) {
        return exp instanceof Literal && ((Literal) exp).getValue() instanceof AtomicValue;
    }

    /**
     * Test whether the literal explicitly wraps an empty sequence. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to be empty).
     *
     * @param exp an expression
     * @return true if the expression is a literal and the value of the literal is an empty sequence
     */

    public static boolean isEmptySequence(Expression exp) {
        return exp instanceof Literal && ((Literal) exp).getValue().getLength() == 0;
    }

    /**
     * Test if a literal represents the boolean value true
     *
     * @param exp   an expression
     * @param value true or false
     * @return true if the expression is a literal and the literal represents the boolean value given in the
     *         second argument
     */

    public static boolean isConstantBoolean(Expression exp, boolean value) {
        if (exp instanceof Literal) {
            GroundedValue b = ((Literal) exp).getValue();
            return b instanceof BooleanValue && ((BooleanValue) b).getBooleanValue() == value;
        }
        return false;
    }

    /**
     * Test if a literal represents the integer value 1
     *
     * @param exp an expression
     * @return true if the expression is a literal and the literal represents the integer value 1
     */

    public static boolean isConstantOne(Expression exp) {
        if (exp instanceof Literal) {
            GroundedValue v = ((Literal) exp).getValue();
            return v instanceof Int64Value && ((Int64Value) v).longValue() == 1;
        }
        return false;
    }

    /**
     * Determine whether the expression can be evaluated without reference to the part of the context
     * document outside the subtree rooted at the context node.
     *
     * @return true if the expression has no dependencies on the context node, or if the only dependencies
     *         on the context node are downward selections using the self, child, descendant, attribute, and namespace
     *         axes.
     */

    public boolean isSubtreeExpression() {
        return true;
    }

    /**
     * Factory method to make an empty-sequence literal
     *
     * @param container the container in the expression tree. Should not be null.
     *                  Saxon does not reject a null value for the container, but it may cause
     *                  subsequent failures if a container is not supplied at some stage.
     * @return a literal whose value is the empty sequence
     */

    public static Literal makeEmptySequence(Container container) {
        return new Literal(EmptySequence.getInstance(), container);
    }

    /**
     * Factory method to create a literal as a wrapper around a Value (factory method)
     *
     * @param value     the value of this literal
     * @param container the container in the expression tree. Should not be null.
     *                  Saxon does not reject a null value for the container, but it may cause
     *                  subsequent failures if a container is not supplied at some stage.
     * @return the Literal
     */

    public static Literal makeLiteral(GroundedValue value, Container container) {
        value = value.reduce();
        if (value instanceof StringValue) {
            return new StringLiteral((StringValue) value, container);
//#ifdefined HOF
        } else if (value instanceof FunctionItem) {
            return new FunctionLiteral((FunctionItem) value, container);
//#endif
        } else {
            return new Literal(value, container);
        }
    }

//#ifdefined STREAM

    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new LiteralAdjunct();
    }

    //#endif

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Literal expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LiteralCompiler();
    }
//#endif

}