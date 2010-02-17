package net.sf.saxon.om;

import net.sf.saxon.trans.XPathException;

/**
 * This class wraps any sequence iterator, turning it into a lookahead iterator,
 * by looking ahead one item
 */
public class LookaheadIteratorImpl implements LookaheadIterator {

    private SequenceIterator base;
    private Item current;
    private Item next;

    private LookaheadIteratorImpl(SequenceIterator base) throws XPathException {
        this.base = base;
        next = base.next();
    }

    public static LookaheadIterator makeLookaheadIterator(SequenceIterator base) throws XPathException {
        if (base instanceof LookaheadIterator) {
            return (LookaheadIterator)base;
        } else {
            return new LookaheadIteratorImpl(base);
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public Item next() throws XPathException {
        current = next;
        if (next != null) {
            next = base.next();
        }
        return current;
    }

    public Item current() {
        return current;
    }

    public int position() {
        int p = base.position();
        return (p <= 0 ? p : p-1);
    }

    public void close() {
        base.close();
    }

    public SequenceIterator getAnother() throws XPathException {
        return new LookaheadIteratorImpl(base.getAnother());
    }

    public int getProperties() {
        return LOOKAHEAD;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



