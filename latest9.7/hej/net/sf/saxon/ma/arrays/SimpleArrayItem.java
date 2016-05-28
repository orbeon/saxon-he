////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.OperandRole;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.functions.DeepEqual;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.z.IntSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A simple implementation of XDM array items, in which the array is backed by a Java List.
 */
public class SimpleArrayItem extends AbstractItem implements ArrayItem {

    public static final SimpleArrayItem EMPTY_ARRAY = new SimpleArrayItem(new ArrayList<Sequence>());

    private List<Sequence> members;

    public SimpleArrayItem(List<Sequence> members) {
        this.members = members;
    }

    public static SimpleArrayItem makeSimpleArrayItem(SequenceIterator input) throws XPathException {
        List<Sequence> members = new ArrayList<Sequence>();
        Item item;
        while ((item = input.next()) != null){
            members.add(item);
        }
        return new SimpleArrayItem(members);
    }

    /**
     * Get the roles of the arguments, for the purposes of streaming
     *
     * @return an array of OperandRole objects, one for each argument
     */
    public OperandRole[] getOperandRoles() {
        return new OperandRole[]{OperandRole.SINGLE_ATOMIC};
    }

    /**
     * Atomize the item.
     *
     * @return the result of atomization
     * @throws XPathException if atomization is not allowed for this kind of item
     */
    public AtomicSequence atomize() throws XPathException {
        List<AtomicValue> list = new ArrayList<AtomicValue>(members.size());
        for (Sequence seq : members) {
            Item item;
            SequenceIterator iter = seq.iterate();
            while ((item = iter.next()) != null) {
                AtomicSequence atoms = item.atomize();
                for (AtomicValue atom : atoms) {
                    list.add(atom);
                }
            }
        }
        return new AtomicArray(list);
    }

    /**
     * Ask whether this function item is an array
     *
     * @return true (it is an array)
     */
    public boolean isArray() {
        return true;
    }

    /**
     * Ask whether this function item is a map
     *
     * @return false (it is not a map)
     */
    public boolean isMap() {
        return false;
    }

    /**
     * Get a member of the array
     *
     * @param index the position of the member to retrieve (zero-based)
     * @return the value at the given position.
     * @throws XPathException if the index is out of range
     */


    public Sequence get(int index) throws XPathException {
        if (index < 0 || index >= members.size()) {
            throw new XPathException("Array index (" + (index+1) + ") out of range (1 to " + members.size() + ")", "FOAY0001");
        }
        return members.get(index);
    }

    /**
     * Get the size of the array
     *
     * @return the number of members in this array
     */

    public int size() {
        return members.size();
    }

    /**
     * Ask whether the array is empty
     *
     * @return true if and only if the size of the array is zero
     */

    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * Get the list of all members of the array
     *
     * @return an iterator over the members of the array
     */

    public Iterator<Sequence> iterator() {
        return members.iterator();
    }

    /**
     * Remove zero or more members from the array
     *
     * @param positions the positions of the members to be removed (zero-based).
     *                  A value that is out of range is ignored.
     * @return a new array in which the requested member has been removed
     */

    public ArrayItem removeSeveral(IntSet positions)  {
        List<Sequence> newList = new ArrayList<Sequence>(members.size() - 1);
        for (int i=0; i<members.size(); i++) {
            if (!positions.contains(i)) {
                newList.add(members.get(i));
            }
        }
        return new SimpleArrayItem(newList);
    };

    /**
     * Remove a member from the array
     *
     * @param pos the position of the member to be removed (zero-based). A value
     *            that is out of range results in an IndexOutOfBoundsException
     * @return a new array in which the requested member has been removed
     */

    public ArrayItem remove(int pos) {
        List<Sequence> newList = new ArrayList<Sequence>(members.size() - 1);
        newList.addAll(members.subList(0, pos));
        newList.addAll(members.subList(pos + 1, members.size()));
        return new SimpleArrayItem(newList);
    }



    /**
     * Concatenate this array with another
     *
     * @param other the second array
     * @return the concatenation of the two arrays; that is, an array
     *         containing first the members of this array, and then the members of the other array
     */

    public ArrayItem concat(ArrayItem other) {
        List<Sequence> newList = new ArrayList<Sequence>(members.size() + other.size());
        newList.addAll(members);
        for (Sequence s : other) {
            newList.add(s);
        }
        return new SimpleArrayItem(newList);
    }

    /**
     * Get the lowest common item type of the members of the array
     *
     * @return the most specific type to which all the members belong.
     */

    public SequenceType getMemberType() {
        return SequenceType.ANY_SEQUENCE; // TODO: return a more precise result
    }

    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */

    public FunctionItemType getFunctionItemType() {
        return ArrayItemType.ANY_ARRAY_TYPE;
    }

    /**
     * Get the name of the function, or null if it is anonymous
     *
     * @return the function name, or null for an anonymous function (which this one is)
     */


    public StructuredQName getFunctionName() {
        return null;
    }

    /**
     * Get a description of this function for use in error messages. For named functions, the description
     * is the function name (as a lexical QName). For others, it might be, for example, "inline function",
     * or "partially-applied ends-with function".
     *
     * @return a description of the function for use in error messages
     */
    public String getDescription() {
        return "array";
    }

    /**
     * Get the arity of the function
     *
     * @return the number of arguments in the function signature (in this case, 1 (one))
     */

    public int getArity() {
        return 1;
    }

    /**
     * Invoke the array in its role as a function
     *
     * @param context the XPath dynamic evaluation context
     * @param args    the actual arguments to be supplied (a single integer)
     * @return the result of invoking the function
     * @throws XPathException if a dynamic error occurs within the function
     */

    public Sequence call(XPathContext context, Sequence[] args) throws XPathException {
        return get((int) ((IntegerValue) args[0].head()).longValue() - 1);
    }

    /**
     * Test whether this array is deep-equal to another array,
     * under the rules of the deep-equal function
     *
     * @param other    the other function item
     * @param context  the dynamic evaluation context
     * @param comparer the object to perform the comparison
     * @param flags    options for how the comparison is performed
     * @return true if the two array items are deep-equal; false if they are not deep equal
     *         or if the other item is not an array
     * @throws XPathException if the comparison cannot be performed
     */


    public boolean deepEquals(Function other, XPathContext context, AtomicComparer comparer, int flags) throws XPathException {
        if (other instanceof ArrayItem) {
            ArrayItem that = (ArrayItem) other;
            if (this.size() != that.size()) {
                return false;
            }
            for (int i = 0; i < this.size(); i++) {
                if (!DeepEqual.deepEqual(this.get(i).iterate(), that.get(i).iterate(), comparer, context, flags)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the effective boolean value of this sequence
     *
     * @return the effective boolean value
     * @throws XPathException if the sequence has no effective boolean value (for example a sequence of two integers)
     */


    public boolean effectiveBooleanValue() throws XPathException {
        throw new XPathException("Effective boolean value is not defined for arrays", "FORG0006");
    }

    /**
     * Get the value of the item as a string. For arrays, there is no string value,
     * so an exception is thrown.
     *
     * @return the string value of the item
     * @throws UnsupportedOperationException if the item is an array (an unchecked exception
     *                                       is used here to avoid introducing exception handling to a large number of paths where it is not
     *                                       needed)
     * @see #getStringValueCS
     * @since 8.4
     */

    public String getStringValue() {
        throw new UnsupportedOperationException("An array does not have a string value");
    }

    /**
     * Get the value of the item as a CharSequence. For arrays, there is no string value,
     * so an exception is thrown.
     *
     * @return the string value of the item
     * @throws UnsupportedOperationException if the item is an array (an unchecked exception
     *                                       is used here to avoid introducing exception handling to a large number of paths where it is not
     *                                       needed)
     * @see #getStringValueCS
     * @since 8.4
     */

    public CharSequence getStringValueCS() {
        throw new UnsupportedOperationException("An array does not have a string value");
    }

    /**
     * Output information about this function item to the diagnostic explain() output
     */
    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("array");
        out.emitAttribute("size", size() + "");
        for (Sequence mem : members) {
            Literal.exportValue(mem, out);
        }
        out.endElement();
    }

    public boolean isTrustedResultType() {
        return false;
    }

    /**
     * Get a list of the members of the array
     *
     * @return the list of members. Note that this returns the actual contained member array, and this is
     * mutable. Changes to this array are permitted only if the caller knows what they are doing, for example
     * during initial construction of an array that will not be presented to the user until construction
     * has finished.
     */

    public List<Sequence> getMembers() {
        return members;
    }

    /**
     * Output a string representation of the array, suitable for diagnostics
     */

    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer(256);
        buffer.append("[");
        for (Sequence seq : members) {
            if (buffer.length() > 1) {
                buffer.append(", ");
            }
            buffer.append(seq.toString());
        }
        buffer.append("]");
        return buffer.toString();
    }
}

