package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.CalendarValue;
import net.sf.saxon.value.DayTimeDurationValue;

/**
* This class implements the XPath 2.0 functions
 * adjust-date-to-timezone(), adjust-time-timezone(), and adjust-dateTime-timezone().
*/


public class Adjust extends SystemFunction implements CallableExpression {

    /**
    * Evaluate in a general context
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        if (av1==null) {
            return null;
        }
        CalendarValue in = (CalendarValue)av1;

        int nargs = argument.length;
        if (nargs==1) {
            return in.adjustTimezone(context.getImplicitTimezone());
        } else {
            AtomicValue av2 = (AtomicValue)argument[1].evaluateItem(context);
            if (av2==null) {
                return in.removeTimezone();
            }
            return adjustToExplicitTimezone(in, (DayTimeDurationValue)av2, context);
        }
    }

    private Item adjustToExplicitTimezone(CalendarValue in, DayTimeDurationValue tz, XPathContext context) throws XPathException {
        long microseconds = tz.getLengthInMicroseconds();
        if (microseconds%60000000 != 0) {
            XPathException err = new XPathException("Timezone is not an integral number of minutes", "FODT0003");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        int tzminutes = (int)(microseconds / 60000000);
        if (Math.abs(tzminutes) > 14*60) {
            XPathException err = new XPathException("Timezone out of range (-14:00 to +14:00)", "FODT0003");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        return in.adjustTimezone(tzminutes);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator<? extends Item> call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        CalendarValue in = (CalendarValue)arguments[0].next();
        if (in == null) {
            return EmptyIterator.emptyIterator();
        }
        if (arguments.length == 1) {
            return SingletonIterator.makeIterator(
                    in.adjustTimezone(context.getImplicitTimezone()));
        } else {
            DayTimeDurationValue tz = (DayTimeDurationValue)arguments[1].next();
            if (tz == null) {
                return SingletonIterator.makeIterator(
                        in.removeTimezone());
            } else {
                return SingletonIterator.makeIterator(
                        adjustToExplicitTimezone(in, tz, context));
            }
        }
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