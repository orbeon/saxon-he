package net.sf.saxon.expr.parser;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.trans.XPathException;

/**
 * General action class which can be used to process all nodes on an expression tree
 */
public interface ExpressionAction {

    /**
     * Process an expression
     *
     * @param expression the expression to be processed
     * @param result     supplied value (of an appropriate type!) which can be updated to return results
     * @return true if processing is now complete and further expressions do not need to be processed
     * @throws XPathException if a failure occurs
     */
    boolean process(Expression expression, Object result) throws XPathException;

}

