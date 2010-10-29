package net.sf.saxon.expr;

import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.om.ValueRepresentation;

import java.util.Stack;

/**
 * This class represents a stack frame holding details of the variables used in a function or in
 * an XSLT template.
 */

public class StackFrame {
    protected SlotManager map;
    protected ValueRepresentation[] slots;
    protected Stack<ValueRepresentation> dynamicStack;

    public StackFrame (SlotManager map, ValueRepresentation[] slots) {
        this.map = map;
        this.slots = slots;
    }

    public SlotManager getStackFrameMap() {
        return map;
    }

    public ValueRepresentation[] getStackFrameValues() {
        return slots;
    }

    public void setStackFrameValues(ValueRepresentation[] values) {
        slots = values;
    }

    public StackFrame copy() {
        ValueRepresentation[] v2 = new ValueRepresentation[slots.length];
        System.arraycopy(slots, 0, v2, 0, slots.length);
        StackFrame s = new StackFrame(map, v2);
        if (dynamicStack != null) {
            s.dynamicStack = new Stack<ValueRepresentation>();
            s.dynamicStack.addAll(dynamicStack);
        }
        return s;
    }

    public void pushDynamicValue(ValueRepresentation value) {
        if (dynamicStack == null) {
            dynamicStack = new Stack<ValueRepresentation>();
        }
        dynamicStack.push(value);
    }

    public ValueRepresentation popDynamicValue() {
        return dynamicStack.pop();
    }

    public static final StackFrame EMPTY =
            new StackFrame(SlotManager.EMPTY, ValueRepresentation.EMPTY_VALUE_ARRAY);

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