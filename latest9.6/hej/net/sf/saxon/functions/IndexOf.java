////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.IndexOfAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.ItemMappingFunction;
import net.sf.saxon.expr.ItemMappingIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;


/**
 * The XPath 2.0 index-of() function
 */


public class IndexOf extends CollatingFunction implements Callable {

    /**
     * Get the argument position (0-based) containing the collation name
     *
     * @return the position of the argument containing the collation URI
     */
    @Override
    protected int getCollationArgument() {
        return 2;
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
    @Override
    public IntegerValue[] getIntegerBounds() {
        return new IntegerValue[]{Int64Value.PLUS_ONE, MAX_SEQUENCE_LENGTH};
    }

    @Override
    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        ItemType type0 = argument[0].getItemType();
        ItemType type1 = argument[1].getItemType();
        if (type0 instanceof AtomicType && type1 instanceof AtomicType) {
            preAllocateComparer((AtomicType) type0, (AtomicType) type1, visitor.getStaticContext(), false);
        }
    }

    /**
     * Evaluate the function to return an iteration of selected nodes.
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicComparer preAllocated = getPreAllocatedAtomicComparer();
        final AtomicComparer comparer =
                preAllocated==null ? getAtomicComparer(getCollator(context), context) : preAllocated;

        SequenceIterator seq = argument[0].iterate(context);
        final AtomicValue val = (AtomicValue) argument[1].evaluateItem(context);
        return indexOf(seq, val, comparer);
    }

    private static SequenceIterator indexOf(SequenceIterator seq, final AtomicValue val, final AtomicComparer comparer) {
        final BuiltInAtomicType searchType = val.getPrimitiveType();
        ItemMappingFunction f = new ItemMappingFunction<AtomicValue, IntegerValue>() {
            int index = 0;
            public IntegerValue mapItem(AtomicValue item) throws XPathException {
                index++;
                if (Type.isGuaranteedComparable(searchType, item.getPrimitiveType(), false) &&
                        comparer.comparesEqual(item, val)) {
                    return new Int64Value(index);
                } else {
                    return null;
                }
            }
        };
        return new ItemMappingIterator<AtomicValue, IntegerValue>(seq, f);
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringCollator collator = getCollatorFromLastArgument(arguments, 2, context);
        GenericAtomicComparer comparer = new GenericAtomicComparer(collator, context);
        SequenceIterator seq = arguments[0].iterate();
        AtomicValue val = (AtomicValue) arguments[1].head();
        return SequenceTool.toLazySequence(indexOf(seq, val, comparer));
    }

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public IndexOfAdjunct getStreamingAdjunct() {
        return new IndexOfAdjunct();
    }
//#endif

}

