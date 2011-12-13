package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Int64Value;

/**
 * This class implements the changes to the tuple stream effected by a "for" clause in a FLWOR expression
 * where "allowing empty" is specified
 */
public class ForClauseOuterPush extends TuplePush {

    protected TuplePush destination;
    protected ForClause forClause;

    public ForClauseOuterPush(TuplePush destination, ForClause forClause) {
        this.destination = destination;
        this.forClause = forClause;
    }

    /*
     * Process the next tuple.
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {
        SequenceIterator iter = forClause.getSequence().iterate(context);
        Item next = iter.next();
        if (next == null) {
            context.setLocalVariable(forClause.getRangeVariable().getLocalSlotNumber(), EmptySequence.getInstance());
            if (forClause.getPositionVariable() != null) {
                context.setLocalVariable(forClause.getPositionVariable().getLocalSlotNumber(), Int64Value.ZERO);
            }
            destination.processTuple(context);
        } else {
            while (true) {
                context.setLocalVariable(forClause.getRangeVariable().getLocalSlotNumber(), next);
                if (forClause.getPositionVariable() != null) {
                    context.setLocalVariable(
                            forClause.getPositionVariable().getLocalSlotNumber(),
                            new Int64Value(iter.position()));
                }
                destination.processTuple(context);
                next = iter.next();
                if (next == null) {
                    break;
                }
            }
        }
    }

    /*
     * Close the tuple stream
     */
    @Override
    public void close() throws XPathException {
        destination.close();
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