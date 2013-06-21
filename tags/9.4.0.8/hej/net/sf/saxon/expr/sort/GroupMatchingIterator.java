package net.sf.saxon.expr.sort;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.value.AtomicValue;

import java.util.List;

/**
 * A GroupMatchingIterator contains code shared between GroupStartingIterator and GroupEndingIterator
 */

public abstract class GroupMatchingIterator implements LookaheadIterator, GroupIterator {

    protected SequenceIterator population;
    protected Pattern pattern;
    protected XPathContext baseContext;
    protected XPathContext runningContext;
    protected List currentMembers;
    /*@Nullable*/ protected Item next;
    protected Item current = null;
    protected int position = 0;


    protected abstract void advance() throws XPathException;

    public AtomicValue getCurrentGroupingKey() {
        return null;
    }

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(currentMembers);
    }

    public boolean hasNext() {
        return next != null;
    }

    public Item next() throws XPathException {
        if (next != null) {
            current = next;
            position++;
            advance();
            return current;
        } else {
            current = null;
            position = -1;
            return null;
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        population.close();
    }

    public int getProperties() {
        return LOOKAHEAD;
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