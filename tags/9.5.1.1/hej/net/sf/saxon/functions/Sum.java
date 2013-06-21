////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.bytecode.ExpressionCompiler;
import com.saxonica.bytecode.SumCompiler;
import com.saxonica.stream.adjunct.StreamingAdjunct;
import com.saxonica.stream.adjunct.SumAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import javax.xml.transform.SourceLocator;


/**
 * Implementation of the fn:sum function
 */
public class Sum extends Aggregate {

    /**
     * Get implementation method
     * @return a value that indicates this function is capable of being streamed
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | ITEM_FEED_METHOD;
    }    

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
        if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            base = BuiltInAtomicType.DOUBLE;
        }
        if (Cardinality.allowsZero(argument[0].getCardinality())) {
            if (argument.length == 1) {
                return Type.getCommonSuperType(base, BuiltInAtomicType.INTEGER, th);
            } else {
                return Type.getCommonSuperType(base, argument[1].getItemType(th), th);
            }
        } else {
            return base.getPrimitiveItemType();
        }
    }
    
    public int computeCardinality(){
    	if(argument.length == 1 || argument[1].getCardinality() == 1){	
    		return StaticProperty.EXACTLY_ONE;
    	}else{
    		return super.computeCardinality();
    	}
    	
    }

    /**
    * Evaluate the function
    */

    /*@Nullable*/ public AtomicValue evaluateItem(XPathContext context) throws XPathException {
        AtomicValue sum = total(argument[0].iterate(context), context, this);
        if (sum != null) {
            return sum;
        } else {
            // the sequence was empty
            if (argument.length == 2) {
                return (AtomicValue)argument[1].evaluateItem(context);
            } else {
                return Int64Value.ZERO;
            }
        }
    }

    /**
     * Calculate the total of a sequence.
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the total, according to the rules of the XPath sum() function, but returning null
     * if the sequence is empty. (It's then up to the caller to decide what the correct result is
     * for an empty sequence.
    */

    public static AtomicValue total(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        ConversionRules rules = context.getConfiguration().getConversionRules();
        StringConverter toDouble = rules.getStringConverter(BuiltInAtomicType.DOUBLE);
        AtomicValue sum = (AtomicValue)iter.next();
        if (sum == null) {
            // the sequence is empty
            return null;
        }
        if (sum instanceof UntypedAtomicValue) {
            try {
                sum = toDouble.convert(sum).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (sum instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (next instanceof UntypedAtomicValue) {
                    next = toDouble.convert(next).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                //sum = ((NumericValue)sum).arithmetic(Token.PLUS, (NumericValue)next, context);
                sum = ArithmeticExpression.compute(sum, Calculator.PLUS, next, context);
                if (sum.isNaN() && sum instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return sum;
                }
            }
        } else if (sum instanceof DurationValue) {
            if (!((sum instanceof DayTimeDurationValue) || (sum instanceof YearMonthDurationValue))) {
                XPathException err = new XPathException("Input to sum() contains a duration that is neither a dayTimeDuration nor a yearMonthDuration");
                err.setXPathContext(context);
                err.setErrorCode("FORG0006");
                err.setLocator(location);
                throw err;
            }
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                sum = ((DurationValue)sum).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException(
                    "Input to sum() contains a value of type " +
                            sum.getPrimitiveType().getDisplayName() +
                            " which is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(location);
            throw err;
        }
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        AtomicValue total = total(arguments[0].iterate(), context, this);
        if (total == null) {
            return (arguments.length == 2 ? arguments[1].head() : IntegerValue.ZERO);
        } else {
            return total;
        }
    }

//#ifdefined BYTECODE
    /**
     * Return the compiler of the Sum expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SumCompiler();
    }
//#endif
//#ifdefined STREAM
    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new SumAdjunct();
    }

    //#endif
}

