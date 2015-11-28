////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;

/**
 * The class XdmFunctionItem represents a function item
 */

public class XdmFunctionItem extends XdmItem {

    protected XdmFunctionItem(Function fi) {
        super(fi);
    }

    /**
     * Get the name of the function
     *
     * @return the function name, as a QName, or null for an anonymous inline function item
     */

    public QName getName() {
        Function fi = (Function) getUnderlyingValue();
        StructuredQName sq = fi.getFunctionName();
        return sq == null ? null : new QName(sq);
    }

    /**
     * Get the arity of the function
     *
     * @return the arity of the function, that is, the number of arguments in the function's signature
     */

    public int getArity() {
        Function fi = (Function) getUnderlyingValue();
        return fi.getArity();
    }

    /**
     * Determine whether the item is an atomic value
     *
     * @return false, the item is not an atomic value, it is a function item
     */

    @Override
    public boolean isAtomicValue() {
        return false;
    }

    /**
     * Call the function
     *
     * @param arguments the values to be supplied as arguments to the function
     * @param processor the s9api Processor
     * @return the result of calling the function
     */

    public XdmValue call(Processor processor, XdmValue... arguments) throws SaxonApiException {
        if (arguments.length != getArity()) {
            throw new SaxonApiException("Supplied " + arguments.length + " arguments, required " + getArity());
        }
        try {
            Function fi = (Function) getUnderlyingValue();
            Sequence[] argVals = new Sequence[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                argVals[i] = arguments[i].getUnderlyingValue();
            }
            XPathContext context = processor.getUnderlyingConfiguration().getConversionContext();
            Sequence result = fi.call(context, argVals);
            Sequence se = SequenceExtent.makeSequenceExtent(result.iterate());
            return XdmValue.wrap(se);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

}