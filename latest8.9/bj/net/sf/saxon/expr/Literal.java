package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import java.io.PrintStream;

/**
 * A Literal is an expression whose value is constant: it is a class that implements the Expression
 * interface as a wrapper around a Value. This may derive from an actual literal in an XPath expression
 * or query, or it may be the result of evaluating a constant subexpression such as true() or xs:date('2007-01-16')
*/

public class Literal extends Expression {

    private Value value;

    /**
     * Create a literal as a wrapper around a Value
     * @param value
     */

    public Literal(Value value) {
        this.value = value;
    }

    /**
     * Create a literal as a wrapper around a Value
     * @param value
     */

    public static Literal makeLiteral(Value value) {
        if (value instanceof StringValue) {
            return new StringLiteral((StringValue)value);
        } else {
            return new Literal(value);
        }
    }

    /**
     * Get the value represented by this Literal
     */

    public Value getValue() {
        return value;
    }


    /**
    * Simplify an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression simplify(StaticContext env) {
        try {
            value = value.reduce();
        } catch (XPathException err) {}
        return this;
    }

    /**
    * TypeCheck an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression typeCheck(StaticContext env, ItemType contextItemType) {
        return this;
    }

    /**
    * Optimize an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) {
        return this;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     * @return for the default implementation: AnyItemType (not known)
     * @param th The TypeHierarchy. Can be null if the target is an AtomicValue.
     */

    public ItemType getItemType(TypeHierarchy th) {
        return value.getItemType(th);
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        if (value instanceof EmptySequence) {
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
     * @return {@link StaticProperty#NON_CREATIVE}
     */


    public int computeSpecialProperties() {
        return StaticProperty.NON_CREATIVE;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
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

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return value.iterate();
    }

    /**
     * Evaluate as a singleton item (or empty sequence). Note: this implementation returns
     * the first item in the sequence. The method should not be used unless appropriate type-checking
     * has been done to ensure that the value will be a singleton.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (value instanceof AtomicValue) {
            return (AtomicValue)value;
        }
        return value.iterate().next();
    }


    /**
      * Process the value as an instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = value.iterate();
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iter.next();
            if (it==null) break;
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

    public String evaluateAsString(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) evaluateItem(context);
        if (value == null) return "";
        return value.getStringValue();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return value.effectiveBooleanValue();
    }

    /**
     * Determine whether two literals are equal, when considered as expressions.
     * @param obj the other expression
     * @return true if the two literals are equal
     */

    public boolean equals(Object obj) {
        return (obj instanceof Literal && value.equals(((Literal)obj).value));
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Diagnostic display of the expression
     */

    public void display(int level, PrintStream out, Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (value instanceof EmptySequence) {
            out.println(ExpressionTool.indent(level) + "()");
        } else if (value instanceof AtomicValue) {
            out.println(ExpressionTool.indent(level) + value.toString());
        } else {
            try {
                out.println(ExpressionTool.indent(level) + "sequence of " +
                        getItemType(th).toString() + " (");
                SequenceIterator iter = iterate(null);
                while (true) {
                    Item it = iter.next();
                    if (it == null) {
                        break;
                    }
                    if (it instanceof NodeInfo) {
                        out.println(ExpressionTool.indent(level + 1) + "node " + Navigator.getPath(((NodeInfo)it)));
                    } else {
                        out.println(ExpressionTool.indent(level + 1) + it.toString());
                    }
                }
                out.println(ExpressionTool.indent(level) + ')');
            } catch (XPathException err) {
                out.println(ExpressionTool.indent(level) + "(*error*)");
            }
        }
    }

    /**
     * Test whether the literal wraps an atomic value. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to contain
     * a single atomic value).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isAtomic(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof AtomicValue;
    }

    /**
     * Test whether the literal explicitly wraps an empty sequence. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to be empty).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isEmptySequence(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof EmptySequence;
    }

    /**
     * Test if a literal represents the boolean value true
     * @param exp an expression
     * @param value true or false
     * @return if the expression is a literal and the literal represents the boolean value given in the
     * second argument
     */

    public static boolean isConstantBoolean(Expression exp, boolean value) {
        if (exp instanceof Literal) {
            Value b = ((Literal)exp).getValue();
            return (b instanceof BooleanValue && ((BooleanValue)b).getBooleanValue() == value);
        }
        return false;
    }

    /**
     * Test if a literal represents the integer value 1
     * @param exp an expression
     * @return if the expression is a literal and the literal represents the integer value 1
     */

    public static boolean isConstantOne(Expression exp) {
        if (exp instanceof Literal) {
            Value v = ((Literal)exp).getValue();
            return (v instanceof Int64Value && ((Int64Value)v).longValue() == 1);
        }
        return false;
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
