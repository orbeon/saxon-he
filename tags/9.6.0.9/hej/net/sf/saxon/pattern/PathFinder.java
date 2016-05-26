////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;

/**
 * This class enables a client to find all nodes in a document that match a particular pattern.
 * In fact, it allows any subset of nodes in a document to be located. It is used specifically by the
 * internal implementation of keys. In XSLT, the criterion for including nodes in a key is that they
 * match an XSLT pattern. Internally, however, keys are used for a wider range of purposes, and the
 * nodes indexed by the key are defined by a PatternFinder
 */

public class PathFinder implements PatternFinder {

    private Expression path;

    /**
     * Create a PathFinder that finds all the nodes returned by a particular expression when invoked
     * with the document node as context node
     *
     * @param path an expression designed to be evaluated with the document node of a tree as the
     *             context node, and which selects a set of nodes within the tree
     */

    public PathFinder(Expression path) {
        this.path = path;
    }

    /**
     * Select nodes in a document using this PatternFinder.
     *
     * @param doc     the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(DocumentInfo doc, XPathContext context) throws XPathException {
        XPathContext c2 = context.newMinorContext();
        ManualIterator mi = new ManualIterator(doc);
        c2.setCurrentIterator(mi);
        return path.iterate(c2);
    }

    /**
     * Get the underlying expression (usually a path expression or filter expression)
     *
     * @return the underlying expression
     */

    public Expression getSelectionExpression() {
        return path;
    }

    /**
     * Get a string representation of the expression. Used in "explain" output
     */

    public String toString() {
        return path.toString();
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof PathFinder && path.equals(((PathFinder)obj).path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
