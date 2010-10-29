package net.sf.saxon.functions;

import net.sf.saxon.lib.CollectionURIResolver;
import net.sf.saxon.expr.*;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AnyURIValue;

import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;

/**
 * Implement the fn:collection() function. This is responsible for calling the
 * registered {@link CollectionURIResolver}. For the effect of the default
 * system-supplied CollectionURIResolver, see {@link net.sf.saxon.lib.StandardCollectionURIResolver}
 */

public class Collection extends SystemFunction {

    private String expressionBaseURI = null;

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() & ~StaticProperty.NON_CREATIVE &
                StaticProperty.ORDERED_NODESET & StaticProperty.PEER_NODESET; 
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return addDocToPathMap(pathMap, pathMapNodeSet);
    }


    /**
     * Iterate over the contents of the collection
     * @param context the dynamic context
     * @return an iterator, whose items will always be nodes (typically but not necessarily document nodes)
     * @throws XPathException
     */

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

        return getResolverResults(iter, expressionBaseURI, context, this);
    }

    /**
     * Return the results of iterating over the results returned by the CollectionURIResolver.
     * <p>Note, this method is called by generated code</p>
     * @param iter iterator over the results of the CollectionURIResolver
     * @param baseURI the base URI
     * @param context the dynamic context
     * @param locator location of the instruction
     * @return an iterator over the documents in the collection.
     */

    public static SequenceIterator getResolverResults(
            SequenceIterator iter, final String baseURI, final XPathContext context, final SourceLocator locator) {
        if (iter == null) {
            return EmptyIterator.getInstance();
        } else {
            ItemMappingFunction imf = new ItemMappingFunction() {
                public Item mapItem(Item item) throws XPathException {
                    if (item instanceof NodeInfo) {
                        return item;
                    } else if (item instanceof AnyURIValue) {
                        return DocumentFn.makeDoc(
                                item.getStringValue(),
                                baseURI,
                                context,
                                locator);
                    } else if (item instanceof Source) {
                        return context.getConfiguration().buildDocument(((Source)item));
                    } else {
                        throw new XPathException("Value returned by CollectionURIResolver must be a Source or an anyURI");
                    }
                }
            };
            return new ItemMappingIterator(iter, imf);
        }
    }



    // TODO: provide control over error recovery (etc) through options in the catalog file.

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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
