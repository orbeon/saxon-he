////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;

/**
 * A function obtained by optimizing a system function call in which some of the parameter values are
 * supplied statically. Used where a useful level of compile-time analysis of the statically-known
 * argument values is possible, for example, compiling a regular expression
 */

public abstract class CurriedSystemFunction extends AbstractFunction {

    public abstract String getName();

    public StructuredQName getFunctionName() {
        return new StructuredQName("saxon", NamespaceConstant.SAXON, getName());
    }

    /**
     * Get a description of this function for use in error messages. For named functions, the description
     * is the function name (as a lexical QName). For others, it might be, for example, "inline function",
     * or "partially-applied ends-with function".
     *
     * @return a description of the function for use in error messages
     */
    public String getDescription() {
        return "internal function " + getName();
    }


    public int getArity() {
        return getFunctionItemType().getArgumentTypes().length;
    }

    /**
     * Output information about this function item
     *
     * @param out
     */
    @Override
    public void export(ExpressionPresenter out) {
        out.startElement("curriedSysFunc");
        out.emitAttribute("name", getName());
        exportLocalData(out);
        out.endElement();
    }

    public abstract void exportLocalData(ExpressionPresenter out);

    public boolean isTrustedResultType() {
        return false;
    }
}
