package net.sf.saxon.expr;

import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;

public interface ExportAgent {

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     * @throws XPathException if the export fails, for example if an expression is found that won't work
     *                        in the target environment.
     */

    void export(ExpressionPresenter out) throws XPathException;
}

