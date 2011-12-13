package net.sf.saxon.trace;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.instruct.TraceExpression;
import net.sf.saxon.expr.parser.CodeInjector;
import net.sf.saxon.om.StructuredQName;

/**
 * A code injector that wraps every expression (other than a literal) in a TraceExpression, which causes
 * a TraceListener to be notified when the expression is evaluated
 */
public class TraceCodeInjector implements CodeInjector {

    /**
     * If tracing, wrap an expression in a trace instruction
     *
     * @param exp         the expression to be wrapped
     * @param env         the static context
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return the expression that does the tracing
     */

    public Expression inject(Expression exp, /*@NotNull*/ StaticContext env, int construct, StructuredQName qName) {
        if (exp instanceof Literal) {
            return exp;
        }
        TraceExpression trace = new TraceExpression(exp);
        //ExpressionTool.copyLocationInfo(exp, trace);
        trace.setNamespaceResolver(env.getNamespaceResolver());
        trace.setConstructType(construct);
        trace.setObjectName(qName);
        //trace.setObjectNameCode(objectNameCode);
        return trace;
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
