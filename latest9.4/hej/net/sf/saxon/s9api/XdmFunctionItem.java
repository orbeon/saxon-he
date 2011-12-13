package net.sf.saxon.s9api;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

/**
 * The class XdmFunctionItem represents a function item
 */

public class XdmFunctionItem extends XdmItem {

    protected XdmFunctionItem(FunctionItem fi) {
        super(fi);
    }

    /**
     * Get the name of the function
     * @return the function name, as a QName, or null for an anonymous inline function item
     */

    public QName getName() {
        FunctionItem fi = (FunctionItem)getUnderlyingValue();
        StructuredQName sq = fi.getFunctionName();
        return (sq == null ? null : new QName(sq));
    }

    /**
     * Get the arity of the function
     * @return the arity of the function, that is, the number of arguments in the function's signature
     */

    public int getArity() {
        FunctionItem fi = (FunctionItem)getUnderlyingValue();
        return fi.getArity();
    }

    /**
     * Determine whether the item is an atomic value
     * @return false, the item is not an atomic value, it is a function item
     */

    @Override
    public boolean isAtomicValue() {
        return false;
    }

    /**
     * Call the function
     * @param arguments the values to be supplied as arguments to the function
     * @param processor the s9api Processor
     * @return the result of calling the function
     */

    public XdmValue call(XdmValue[] arguments, Processor processor) throws SaxonApiException {
        if (arguments.length != getArity()) {
            throw new SaxonApiException("Supplied " + arguments.length + " arguments, required " + getArity());
        }
        try {
            FunctionItem fi = (FunctionItem)getUnderlyingValue();
            SequenceIterator[] argIters = new SequenceIterator[arguments.length];
            for (int i=0; i<arguments.length; i++) {
                argIters[i] = Value.asIterator(arguments[i].getUnderlyingValue());
            }
            XPathContext context = processor.getUnderlyingConfiguration().getConversionContext();
            SequenceIterator resultIter = fi.invoke(argIters, context);
            ValueRepresentation se = SequenceExtent.makeSequenceExtent(resultIter);
            return XdmValue.wrap(se);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
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