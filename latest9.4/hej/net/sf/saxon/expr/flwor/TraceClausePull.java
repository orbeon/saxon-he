package net.sf.saxon.expr.flwor;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

/**
 * This class represents the tuple stream delivered as the output of a trace clause in a
 * FLWOR expression. It does not change the values of any variables in the tuple stream,
 * but merely informs the TraceListener whenever a new tuple is requested.
 */
public class TraceClausePull extends TuplePull {

    private TuplePull base;
    private Clause baseClause;
    private Container container;

    public TraceClausePull(TuplePull base, Clause baseClause, Container container) {
        this.base = base;
        this.baseClause = baseClause;
        this.container = container;
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
        Controller controller = context.getController();
        if (controller.isTracing()) {
            ClauseInfo baseInfo = new ClauseInfo(baseClause, container);
            controller.getTraceListener().enter(baseInfo, context);
            boolean b = base.nextTuple(context);
            controller.getTraceListener().leave(baseInfo);
            return b;
        } else {
            return base.nextTuple(context);
        }

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