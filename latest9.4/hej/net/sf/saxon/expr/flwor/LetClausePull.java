package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;

/**
 *  Implements the changes to a tuple stream effected by the Let clause in a FLWOR expression
 */
public class LetClausePull extends TuplePull {

    TuplePull base;
    LetClause letClause;

    public LetClausePull(TuplePull base, LetClause letClause) {
        this.base = base;
        this.letClause = letClause;
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
        if (!base.nextTuple(context)) {
            return false;
        }
        ValueRepresentation val = ExpressionTool.lazyEvaluate(letClause.getSequence(), context, 100);
        // TODO: be smarter, see LetExpression.eval()
        context.setLocalVariable(letClause.getRangeVariable().getLocalSlotNumber(), val);
        return true;
    }

    /**
     * Close the tuple stream, indicating that although not all tuples have been read,
     * no further tuples are required and resources can be released
     */
    @Override
    public void close() {
        base.close();
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