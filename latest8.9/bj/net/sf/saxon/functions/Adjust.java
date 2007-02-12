package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.CalendarValue;
import net.sf.saxon.value.DayTimeDurationValue;

/**
* This class implements the XPath 2.0 functions
 * adjust-date-to-timezone(), adjust-time-timezone(), and adjust-dateTime-timezone().
*/


public class Adjust extends SystemFunction {

    int implicitTimezone; // implicit timezone as offset from UTC in minutes

    /**
    * Simplify and validate.
    */

    public Expression simplify(StaticContext env) throws XPathException {
        implicitTimezone = env.getConfiguration().getImplicitTimezone();
        return super.simplify(env);
    }

    /**
     * Get the implicit timezone used (as an offset from UTC in minutes)
     */

    public int getImplicitTimezone() {
        return implicitTimezone;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        if (av1==null) {
            return null;
        }
        CalendarValue in = (CalendarValue)av1;

        int nargs = argument.length;
        DayTimeDurationValue tz;
        if (nargs==1) {
            return in.adjustTimezone(implicitTimezone);
        } else {
            AtomicValue av2 = (AtomicValue)argument[1].evaluateItem(context);
            if (av2==null) {
                return in.removeTimezone();
            }
            tz = (DayTimeDurationValue)av2;
            long microseconds = tz.getLengthInMicroseconds();
            if (microseconds%60000000 != 0) {
                DynamicError err = new DynamicError("Timezone is not an integral number of minutes");
                err.setErrorCode("FODT0003");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
            int tzminutes = (int)(microseconds / 60000000);
            if (Math.abs(tzminutes) > 14*60) {
                DynamicError err = new DynamicError("Timezone out of range (-14:00 to +14:00)");
                err.setErrorCode("FODT0003");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
            return in.adjustTimezone(tzminutes);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
