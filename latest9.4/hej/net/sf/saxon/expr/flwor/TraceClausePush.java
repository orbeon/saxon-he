package net.sf.saxon.expr.flwor;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

/**
 * This class represents the tuple stream delivered as the output of a trace clause in a
 * FLWOR expression. It does not change the values of any variables in the tuple stream,
 * but merely notifies the TraceListener whenever a new tuple is available.
 */
public class TraceClausePush extends TuplePush {

    private TuplePush destination;
    private Clause baseClause;
    private Container container;

    public TraceClausePush(TuplePush destination, Clause baseClause, Container container) {
        this.destination = destination;
        this.baseClause = baseClause;
        this.container = container;
    }

    /*
     * Process the next tuple.
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        if (controller.isTracing()) {
            ClauseInfo baseInfo = new ClauseInfo(baseClause, container);
            controller.getTraceListener().enter(baseInfo, context);
            destination.processTuple(context);
            controller.getTraceListener().leave(baseInfo);
        } else {
            destination.processTuple(context);
        }
    }

    /*
     * Notify the end of the tuple stream
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