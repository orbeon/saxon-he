////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.SimpleArrayItem;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Event handler for the JSON parser which constructs a structure of maps and arrays
 * representing the content of the JSON text.
 */
public class JsonHandlerMap extends JsonHandler {
    Stack<Sequence> stack;

    protected Stack<String> keys;

    public JsonHandlerMap(XPathContext context, int flags) {
        setContext(context);
        stack = new Stack<Sequence>();
        keys = new Stack<String>();
        escape = (flags & JsonParser.ESCAPE) != 0;
        charChecker = context.getConfiguration().getValidCharacterChecker();
    }

    public Sequence getResult() throws XPathException {
        return stack.peek();
    }

    /**
     * Set the key to be written for the next entry in an object/map
     *
     * @param key       the key for the entry (null implies no key)
     * @param isEscaped true if backslashes within the key are to be treated as signalling
     *                  a JSON escape sequence
     * @return true if the key is already present in the map, false if it is not
     */
    public boolean setKey(String key, boolean isEscaped) throws XPathException {
        key = reEscape(key, false, isEscaped, escape);
        this.keys.push(key);
        HashTrieMap map = (HashTrieMap) stack.peek();
        return map.get(new StringValue(key)) != null;
    }

    /**
     * Open a new array
     *
     * @throws XPathException
     */
    public void startArray() throws XPathException {
        ArrayItem map = new SimpleArrayItem(new ArrayList<Sequence>());
        stack.push(map);
    }

    /**
     * Close the current array
     *
     * @throws XPathException
     */
    public void endArray() throws XPathException {
        ArrayItem map = (ArrayItem) stack.pop();
        if (stack.empty()) {
            stack.push(map); // the end
        } else {
            writeItem(map);
        }
    }

    /**
     * Start a new object/map
     *
     * @throws XPathException
     */
    public void startMap() throws XPathException {
        HashTrieMap map = new HashTrieMap(getContext());
        stack.push(map);
    }

    /**
     * Close the current object/map
     *
     * @throws XPathException
     */
    public void endMap() throws XPathException {
        HashTrieMap map = (HashTrieMap) stack.pop();
        if (stack.empty()) {
            stack.push(map); // the end
        } else {
            writeItem(map);
        }
    }

    /**
     * Write an item into the current map, with the preselected key
     * @param val   the value/map to be written
     * @throws XPathException if an error occurs writing to the map
     */
    private void writeItem(Sequence val) throws XPathException {
        if (stack.empty()) {
            stack.push(val);
        } else if (stack.peek() instanceof ArrayItem) {
            SimpleArrayItem array = (SimpleArrayItem) stack.peek();
            array.getMembers().add(val);
        } else {
            HashTrieMap map = (HashTrieMap) stack.peek();
            //StringValue key = new StringValue(reEscape(keys.pop(), true, false, false));
            StringValue key = new StringValue(keys.pop());
            map.initialPut(key, val);
        }
    }

    /**
     * Write a numeric value
     *
     * @param asString the string representation of the value
     * @param asDouble the double representation of the value
     * @throws XPathException if a dynamic error occurs
     */
    public void writeNumeric(String asString, double asDouble) throws XPathException {
        writeItem(new DoubleValue(asDouble));
    }

    /**
     * Write a string value
     *
     * @param val The string to be written (which may or may not contain JSON escape sequences, according to the
     * options that were set)
     * @param isEscaped true if backslash is to be treated as an escape character
     * @throws XPathException if a dynamic error occurs
     */
    public void writeString(String val, boolean isEscaped) throws XPathException {
        writeItem(new StringValue(reEscape(val, false, isEscaped, escape)));
    }

    /**
     * Write a boolean value
     *
     * @param value the boolean value to be written
     * @throws XPathException if a dynamic error occurs
     */
    public void writeBoolean(boolean value) throws XPathException {
        writeItem(BooleanValue.get(value));
    }

    /**
     * Write a null value
     *
     * @throws XPathException if a dynamic error occurs
     */
    public void writeNull() throws XPathException {
        writeItem(EmptySequence.getInstance());
    }


}
