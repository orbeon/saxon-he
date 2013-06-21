package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;

import java.util.Iterator;

/**
* This class supports fuctions get-in-scope-prefixes()
*/

public class InScopePrefixes extends SystemFunction implements CallableExpression {

    /**
    * Iterator over the results of the expression
    */

    /*@NotNull*/
    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        final NodeInfo element = (NodeInfo)argument[0].evaluateItem(context);
        final Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(element);

        return sequenceIterator(iter, context);

    }
    
    /*@Nullable*/ public SequenceIterator sequenceIterator(final Iterator<NamespaceBinding> iter, final XPathContext context){
    	return new SequenceIterator() {
            private Item current = null;
            private int position = 0;

            public Item current() {
                return current;
            }

            /*@NotNull*/
            public SequenceIterator getAnother() throws XPathException {
                return iterate(context);
            }

            public int getProperties() {
                return 0;
            }

            public Item next() throws XPathException {
                if (position == 0) {
                    current = new StringValue("xml");
                    position++;
                    return current;
                } else if (iter.hasNext()) {
                    String prefix = iter.next().getPrefix();
                    if (prefix.length() == 0) {
                        current = StringValue.EMPTY_STRING;
                    } else {
                        current = new StringValue(prefix, BuiltInAtomicType.NCNAME);
                    }
                    position++;
                    return current;
                } else {
                    current = null;
                    position = -1;
                    return null;
                }
            }

            public int position() {
                return position;
            }

            public void close() {
            }
        };
    	
    }

	public SequenceIterator call(SequenceIterator[] arguments,
			final XPathContext context) throws XPathException {
	    final NodeInfo element = (NodeInfo)arguments[0].next();
        final Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(element);

        return sequenceIterator(iter, context);
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