package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;

/**
 * A "trace" clause in a FLWOR expression, added by a TraceCodeInjector for diagnostic
 * tracing, debugging, profiling or similar purposes.
 */
public class TraceClause extends Clause {

    private Clause target;
    private Container container;
    private NamespaceResolver nsResolver;

    /**
     * Create a traceClause
     * @param target the clause whose evaluation is being traced
     * @param container the container of the containing FLWORExpression
     */

    public TraceClause(Clause target, NamespaceResolver nsResolver, Container container) {
        this.target = target;
        this.nsResolver = nsResolver;
        this.container = container;
    }

    /**
     * Get the namespace bindings from the static context of the clause
     * @return a namespace resolver that reflects the in scope namespaces of the clause
     */

    public NamespaceResolver getNamespaceResolver() {
        return nsResolver;
    }

    /**
     * Set the namespace bindings from the static context of the clause
     * @param nsResolver a namespace resolver that reflects the in scope namespaces of the clause
     */

    public void setNamespaceResolver(NamespaceResolver nsResolver) {
        this.nsResolver = nsResolver;
    }


    @Override
    public int getClauseKey() {
        return TRACE;
    }

    public TraceClause copy() {
        return new TraceClause(target, nsResolver,  container);
    }

    /**
     * Get a tuple stream that implements the functionality of this clause, taking its
     * input from another tuple stream which this clause modifies
     * @param base the input tuple stream
     * @param context the dynamic evaluation context
     * @return the output tuple stream
     */
    @Override
    public TuplePull getPullStream(TuplePull base, XPathContext context) {
        return new TraceClausePull(base, this, target, container);
    }

    /**
     * Get a push-mode tuple stream that implements the functionality of this clause, supplying its
     * output to another tuple stream
     *
     * @param destination the output tuple stream
     * @param context  the dynamic evaluation context
     * @return the push tuple stream that implements the functionality of this clause of the FLWOR
     *         expression
     */
    @Override
    public TuplePush getPushStream(TuplePush destination, XPathContext context) {
        return new TraceClausePush(destination, this, target, container);
    }

    /**
     * Process the subexpressions of this clause
    * @param processor the expression processor used to process the subexpressions
    *
    */
    @Override
    public void processSubExpressions(ExpressionProcessor processor) throws XPathException {
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */
    @Override
    public void explain(ExpressionPresenter out) {
        out.startElement("trace");
        out.endElement();
    }

    public String toString() {
        return "trace";
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