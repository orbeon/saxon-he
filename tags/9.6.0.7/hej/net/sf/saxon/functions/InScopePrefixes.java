////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;

import java.util.Iterator;

/**
 * This class supports fuctions get-in-scope-prefixes()
 */

public class InScopePrefixes extends SystemFunctionCall implements Callable {

    /**
     * Iterator over the results of the expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        final NodeInfo element = (NodeInfo) argument[0].evaluateItem(context);
        final Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(element);

        return sequenceIterator(iter, context);

    }

    /*@Nullable*/
    public SequenceIterator sequenceIterator(final Iterator<NamespaceBinding> iter, final XPathContext context) {
        return new SequenceIterator() {
            private int position = 0;

            /*@NotNull*/
            public SequenceIterator getAnother() throws XPathException {
                return iterate(context);
            }

            public int getProperties() {
                return 0;
            }

            public StringValue next() throws XPathException {
                if (position == 0) {
                    position++;
                    return new StringValue("xml");
                } else if (iter.hasNext()) {
                    String prefix = iter.next().getPrefix();
                    StringValue current;
                    if (prefix.length() == 0) {
                        current = StringValue.EMPTY_STRING;
                    } else {
                        current = new StringValue(prefix, BuiltInAtomicType.NCNAME);
                    }
                    position++;
                    return current;
                } else {
                    position = -1;
                    return null;
                }
            }

            public void close() {
            }
        };

    }

    public Sequence call(final XPathContext context, Sequence[] arguments) throws XPathException {
        final NodeInfo element = (NodeInfo) arguments[0].head();
        final Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(element);

        return SequenceTool.toLazySequence(sequenceIterator(iter, context));
    }

}

