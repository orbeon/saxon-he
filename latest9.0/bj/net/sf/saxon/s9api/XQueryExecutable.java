package net.sf.saxon.s9api;

import net.sf.saxon.query.XQueryExpression;

/**
 * An XQueryExecutable represents the compiled form of a query.
 * To execute the query, it must first be loaded to form an {@link net.sf.saxon.s9api.XQueryEvaluator}.
 *
 * <p>An XQueryExecutable is immutable, and therefore thread-safe.
 *  It is simplest to load a new XsltTransformer each time the stylesheet is to be run.
 *  However, the XsltTransformer is serially reusable within a single thread. </p>
 *
 * <p>An XQueryExecutable is created by using one of the <code>compile</code> methods on the
 * {@link net.sf.saxon.s9api.XQueryCompiler} class.</p>
 */
public class XQueryExecutable {

    Processor processor;
    XQueryExpression exp;

    protected XQueryExecutable(Processor processor, XQueryExpression exp) {
        this.processor = processor;
        this.exp = exp;
    }

    /**
     * Load the stylesheet to prepare it for execution.
     * @return  An XsltTransformer. The returned XsltTransformer can be used to set up the
     * dynamic context for stylesheet evaluation, and to run the stylesheet.
     */

    public XQueryEvaluator load() {
        return new XQueryEvaluator(processor, exp);
    }

    /**
     * Get the underlying implementation object representing the compiled stylesheet. This provides
     * an escape hatch into lower-level APIs. The object returned by this method may change from release
     * to release.
     * @return the underlying implementation of the compiled stylesheet
     */

    public XQueryExpression getUnderlyingCompiledQuery() {
        return exp;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

