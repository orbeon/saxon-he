package net.sf.saxon.s9api;

import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.sxpath.XPathVariable;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.StaticContext;

import java.util.ArrayList;

/**
 * An XPathExecutable represents the compiled form of an XPath expression.
 * To evaluate the expression, it must first be loaded to form an {@link XPathSelector}.
 *
 * <p>An XPathExecutable is immutable, and therefore thread-safe. It is simplest to load
 * a new XPathSelector each time the expression is to be evaluated. However, the XPathSelector
 * is serially reusable within a single thread.</p>
 *
 * <p>An XPathExecutable is created by using the {@link XPathCompiler#compile} method
 * on the {@link XPathCompiler} class.</p>
 */

public class XPathExecutable {

    private XPathExpression exp;
    private Configuration config;
    private IndependentContext env;
    private ArrayList<XPathVariable> declaredVariables;

    // protected constructor

    protected XPathExecutable(XPathExpression exp, Configuration config,
            IndependentContext env, ArrayList<XPathVariable> declaredVariables) {
        this.exp = exp;
        this.config = config;
        this.env = env;
        this.declaredVariables = declaredVariables;
    }

    /**
     * Load the compiled XPath expression to prepare it for execution.
     * @return An XPathSelector. The returned XPathSelector can be used to set up the
     * dynamic context, and then to evaluate the expression.
     */

    public XPathSelector load() {
        return new XPathSelector(exp, declaredVariables);
    }

    /**
     * Get the underlying implementation object representing the compiled XPath expression.
     * This method provides access to lower-level Saxon classes and methods which may be subject to change
     * from one release to the next.
     * @return the underlying compiled XPath expression.
     */

    public XPathExpression getUnderlyingExpression() {
        return exp;
    }

    /**
     * Get the underlying implementation object representing the static context of the compiled
     * XPath expression. This method provides access to lower-level Saxon classes and methods which may be
     * subject to change from one release to the next.
     * @return the underlying static context.
     */

    public StaticContext getUnderlyingStaticContext() {
        return env;
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

