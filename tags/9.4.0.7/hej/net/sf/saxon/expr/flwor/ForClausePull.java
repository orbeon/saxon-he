package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;

/**
 * This class implements the changes to the tuple stream effected by a "for" clause in a FLWOR expression
 */
public class ForClausePull extends TuplePull {

    protected TuplePull base;
    protected ForClause forClause;
    /*@Nullable*/ protected SequenceIterator currentIteration;

    public ForClausePull(TuplePull base, ForClause forClause) {
        this.base = base;
        this.forClause = forClause;
    }

    /**
     * Move on to the next tuple. Before returning, this method must set all the variables corresponding
     * to the "returned" tuple in the local stack frame associated with the context object
     *
     * @param context the dynamic evaluation context
     * @return true if another tuple has been generated; false if the tuple stream is exhausted. If the
     *         method returns false, the values of the local variables corresponding to this tuple stream
     *         are undefined.
     */
    @Override
    public boolean nextTuple(XPathContext context) throws XPathException {
        while (true) {
            if (currentIteration == null) {
                if (!base.nextTuple(context)) {
                    return false;
                }
                currentIteration = forClause.getSequence().iterate(context);
            }
            Item next = currentIteration.next();
            if (next != null) {
                context.setLocalVariable(forClause.getRangeVariable().getLocalSlotNumber(), next);
                if (forClause.getPositionVariable() != null) {
                    context.setLocalVariable(
                            forClause.getPositionVariable().getLocalSlotNumber(),
                            new Int64Value(currentIteration.position()));
                }
                return true;
            } else {
                currentIteration = null;
            }
        }
    }

    /**
     * Close the tuple stream, indicating that although not all tuples have been read,
     * no further tuples are required and resources can be released
     */
    @Override
    public void close() {
        base.close();
        if (currentIteration != null) {
            currentIteration.close();
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