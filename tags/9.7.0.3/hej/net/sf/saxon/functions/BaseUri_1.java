////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.BaseURICompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AnyURIValue;

/**
 * This class implements the fn:base-uri() function in XPath 2.0
 */

public class BaseUri_1 extends SystemFunction implements Callable {

    public ZeroOrOne<AnyURIValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo node = (NodeInfo)arguments[0].head();
        if (node == null) {
            return ZeroOrOne.empty();
        }
        String s = node.getBaseURI();
        if (s == null) {
            return ZeroOrOne.empty();
        }
        return new ZeroOrOne<AnyURIValue>(new AnyURIValue(s));

    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the BaseURI expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new BaseURICompiler();
    }
    //#endif

}

