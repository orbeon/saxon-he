package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;

/**
 *  Implements the changes to a tuple stream effected by the Let clause in a FLWOR expression
 */
public class LetClausePush extends TuplePush {

    TuplePush destination;
    LetClause letClause;

    public LetClausePush(TuplePush destination, LetClause letClause) {
        this.destination = destination;
        this.letClause = letClause;
    }

    /*
     * Notify the next tuple.
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {
        ValueRepresentation val = ExpressionTool.lazyEvaluate(letClause.getSequence(), context, 100);
        // TODO: be smarter, see LetExpression.eval()
        context.setLocalVariable(letClause.getRangeVariable().getLocalSlotNumber(), val);
        destination.processTuple(context);
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