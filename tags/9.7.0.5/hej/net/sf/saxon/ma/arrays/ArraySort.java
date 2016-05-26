////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.type.SpecificFunctionType;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.*;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.NoDynamicContextException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;

import javax.xml.xpath.XPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the extension function array:sort(array, function) => array
 */
public class ArraySort extends ExtensionFunctionDefinition {

    private final static StructuredQName name = new StructuredQName("array", NamespaceConstant.ARRAY_FUNCTIONS, "sort");
    private final static SequenceType[] ARG_TYPES = new SequenceType[]{
            ArrayItem.SINGLE_ARRAY_TYPE, SequenceType.makeSequenceType(
            new SpecificFunctionType(
                    new SequenceType[]{SequenceType.ANY_SEQUENCE},
                    SequenceType.ATOMIC_SEQUENCE),
            StaticProperty.EXACTLY_ONE)};

    /**
     * Get the name of the function, as a QName.
     * <p>This method must be implemented in all subclasses</p>
     *
     * @return the function name
     */
    @Override
    public StructuredQName getFunctionQName() {
        return name;
    }

    /**
     * Get the minimum number of arguments required by the function
     *
     * @return the minimum number of arguments that must be supplied in a call to this function
     */

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    /**
     * Get the maximum number of arguments allowed by the function.
     *
     * @return the maximum number of arguments that may be supplied in a call to this function
     */

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    /**
     * Get the required types for the arguments of this function.
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @return the required types of the arguments, as defined by the function signature. Normally
     * this should be an array of size {@link #getMaximumNumberOfArguments()}; however for functions
     * that allow a variable number of arguments, the array can be smaller than this, with the last
     * entry in the array providing the required type for all the remaining arguments.
     */
    @Override
    public SequenceType[] getArgumentTypes() {
        return ARG_TYPES;
    }

    /**
     * Get the type of the result of the function
     * <p>This method must be implemented in all subtypes.</p>
     *
     * @param suppliedArgumentTypes the static types of the supplied arguments to the function.
     *                              This is provided so that a more precise result type can be returned in the common
     *                              case where the type of the result depends on the types of the arguments.
     * @return the return type of the function, as defined by its function signature
     */
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return suppliedArgumentTypes[0];
    }

    /**
     * Ask whether the result actually returned by the function can be trusted,
     * or whether it should be checked against the declared type.
     *
     * @return true if the function implementation warrants that the value it returns will
     *         be an instance of the declared result type. The default value is false, in which case
     *         the result will be checked at run-time to ensure that it conforms to the declared type.
     *         If the value true is returned, but the function returns a value of the wrong type, the
     *         consequences are unpredictable.
     */

    public boolean trustResultType() {
        return true;
    }

    private static class MemberToBeSorted{
        public Sequence value;
        public GroundedValue sortKey;
        int originalPosition;
    }

    /**
     * Create a call on this function. This method is called by the compiler when it identifies
     * a function call that calls this function.
     *
     * @return an expression representing a call of this extension function
     */
    @Override
    public ExtensionFunctionCall makeCallExpression() {
        return new ExtensionFunctionCall() {

            public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
                ArrayItem array = (ArrayItem) arguments[0].head();
                final List<MemberToBeSorted> inputList = new ArrayList<MemberToBeSorted>(array.size());
                int i = 0;
                Function key = null;
                if (arguments.length == 2){
                    key = (Function) arguments[1].head();
                }
                for (Sequence seq: array){
                    MemberToBeSorted member = new MemberToBeSorted();
                    member.value = seq;
                    member.originalPosition = i++;
                    if (arguments.length == 2) {
                        member.sortKey = SequenceTool.toGroundedValue(key.call(context, new Sequence[]{seq}));
                    } else {
                        member.sortKey = atomize(seq);
                    }
                    inputList.add(member);
                }
                // TODO: default collation
                final AtomicComparer atomicComparer =  AtomicSortComparer.makeSortComparer(
                        CodepointCollator.getInstance(), StandardNames.XS_ANY_ATOMIC_TYPE, context);
                Sortable sortable = new Sortable() {
                    public int compare(int a, int b) {
                        int result = compareSortKeys(inputList.get(a).sortKey, inputList.get(b).sortKey, atomicComparer);
                        if (result == 0){
                            return inputList.get(a).originalPosition - inputList.get(b).originalPosition;
                        } else {
                            return result;
                        }
                    }

                    public void swap(int a, int b) {
                        MemberToBeSorted temp = inputList.get(a);
                        inputList.set(a, inputList.get(b));
                        inputList.set(b, temp);
                    }
                };
                try {
                    GenericSorter.quickSort(0, array.size(), sortable);
                } catch (ClassCastException e) {
                    XPathException err = new XPathException("Non-comparable types found while sorting: " + e.getMessage());
                    err.setErrorCode("XPTY0004");
                    throw err;
                }
                List<Sequence> outputList = new ArrayList<Sequence>(array.size());
                for (MemberToBeSorted member: inputList){
                    outputList.add(member.value);
                }
                return new SimpleArrayItem(outputList);
            }
        };
    }
    public static int compareSortKeys(GroundedValue a, GroundedValue b, AtomicComparer comparer) {
        UnfailingIterator iteratora = a.iterate();
        UnfailingIterator iteratorb = b.iterate();
        while (true){
            AtomicValue firsta = (AtomicValue) iteratora.next();
            AtomicValue firstb = (AtomicValue) iteratorb.next();
            if (firsta == null){
                if (firstb == null){
                    return 0;
                }
                else {
                    return -1;
                }
            }
            else if (firstb == null){
                return +1;
            }
            else {
                try {
                    int first = comparer.compareAtomicValues(firsta, firstb);
                    if (first == 0){
                        continue;
                    } else {
                        return first;
                    }
                } catch (NoDynamicContextException e) {
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static GroundedValue atomize(Sequence input) throws XPathException {
        try {
            SequenceIterator iterator = input.iterate();
            SequenceIterator mapper = Atomizer.getAtomizingIterator(iterator, false);
            return SequenceExtent.makeSequenceExtent(mapper);
        } catch (XPathException e) {
            throw new XPathException(e);
        }
    }
}
