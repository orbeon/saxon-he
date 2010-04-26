package net.sf.saxon.functions;

import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.event.ComplexContentOutputter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;


public class Concat extends SystemFunction {

    /**
    * Get the required type of the nth argument
    */

    protected SequenceType getRequiredType(int arg) {
        return getDetails().argumentTypes[0];
        // concat() is a special case
    }

    /**
    * Evaluate the function in a string context
    */

    public CharSequence evaluateAsString(XPathContext c) throws XPathException {
        return evaluateItem(c).getStringValueCS();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        int numArgs = argument.length;
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        for (int i=0; i<numArgs; i++) {
            AtomicValue val = (AtomicValue)argument[i].evaluateItem(c);
            if (val!=null) {
                sb.append(val.getStringValueCS());
            }
        }
        return StringValue.makeStringValue(sb.condense());
    }

    /**
     * Process the instruction in push mode. This avoids constructing the concatenated string
     * in memory, instead each argument can be sent straight to the serializer.
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        if (out instanceof ComplexContentOutputter) {
            // This optimization is only safe if the output forms part of document or element content
            int numArgs = argument.length;
            // Start and end with an empty string to force space separation from any previous or following outputs
            out.append(StringValue.EMPTY_STRING, 0, 0);
            boolean empty = true;
            for (int i=0; i<numArgs; i++) {
                AtomicValue val = (AtomicValue)argument[i].evaluateItem(context);
                if (val!=null) {
                    out.characters(val.getStringValueCS(), 0, 0);
                    empty = false;
                }
            }
            if (!empty) {
                out.append(StringValue.EMPTY_STRING, 0, 0);
            }
        } else {
            out.append(evaluateItem(context), 0, 0);
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
