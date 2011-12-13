package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.value.AnyURIValue;

import javax.xml.transform.Source;

/**
 * Implement the fn:uri-collection() function (new in XQuery 3.0/XSLT 3.0). This is responsible for calling the
 * registered {@link net.sf.saxon.lib.CollectionURIResolver}. For the effect of the default
 * system-supplied CollectionURIResolver, see {@link net.sf.saxon.lib.StandardCollectionURIResolver}
 */

public class UriCollection extends SystemFunction implements CallableExpression {

    // TODO: change the standard collection URI resolver so that query parameters such as onerror=warning become
    // part of the returned document URI, to be actioned only when the document URI is dereferenced

    /*@Nullable*/ private String expressionBaseURI = null;

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
     * Iterate over the contents of the collection
     * @param context the dynamic context
     * @return an iterator, whose items will always be nodes (typically but not necessarily document nodes)
     * @throws net.sf.saxon.trans.XPathException
     */

    /*@NotNull*/
    public SequenceIterator iterate(final XPathContext context) throws XPathException {

        String href;

        if (getNumberOfArguments() == 0) {
            // No arguments supplied: this gets the default collection
            href = context.getConfiguration().getDefaultCollection();
        } else {
            href = argument[0].evaluateItem(context).getStringValue();
        }

        CollectionURIResolver resolver = context.getConfiguration().getCollectionURIResolver();
        SequenceIterator iter;
        try {
            iter = resolver.resolve(href, expressionBaseURI, context);
        } catch (XPathException e) {
            e.setLocator(this);
            throw e;
        }

        return getResolverResults(iter, context);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        String href;

        if (getNumberOfArguments() == 0) {
            // No arguments supplied: this gets the default collection
            href = context.getConfiguration().getDefaultCollection();
        } else {
            href = arguments[0].next().getStringValue();
        }

        CollectionURIResolver resolver = context.getConfiguration().getCollectionURIResolver();
        SequenceIterator iter;
        try {
            iter = resolver.resolve(href, expressionBaseURI, context);
        } catch (XPathException e) {
            e.setLocator(this);
            throw e;
        }

        return getResolverResults(iter, context);
    }

    private static SequenceIterator getResolverResults(
            SequenceIterator iter, final XPathContext context) {
        if (iter == null) {
            return EmptyIterator.getInstance();
        } else {
            ItemMappingFunction imf = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (item instanceof NodeInfo) {
                        return DocumentUriFn.getDocumentURI(((NodeInfo)item), context);
                    } else if (item instanceof Source) {
                        return new AnyURIValue(((Source)item).getSystemId());
                    } else if (item instanceof AnyURIValue) {
                        return item;
                    } else {
                        throw new XPathException("Value returned by CollectionURIResolver must be a Source or an anyURI");
                    }
                }
            };
            return new ItemMappingIterator(iter, imf);
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