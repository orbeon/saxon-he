package net.sf.saxon.tree.util;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class provides an iterator over the namespace codes representing the in-scope namespaces
 * of any node. It relies on nodes to implement the method
 * {@link net.sf.saxon.om.NodeInfo#getDeclaredNamespaces(net.sf.saxon.om.NamespaceBinding[])}.
 *
 * <p>The result does not include the XML namespace.</p>
 */
public class NamespaceIterator implements Iterator<NamespaceBinding> {

    /*@Nullable*/ private NodeInfo element;
    private int index;
    /*@Nullable*/ private NamespaceBinding next;
    private NamespaceBinding[] localDeclarations;
    HashSet<String> undeclaredPrefixes;

    /**
     * Factory method: create an iterator over the in-scope namespace codes for an element
     * @param element the element (or other node) whose in-scope namespaces are required. If this
     * is not an element, the result will be an empty iterator
     * @return an iterator over the namespace codes. A namespace code is an integer that represents
     * a prefix-uri binding; the prefix and URI can be obtained by reference to the name pool. This
     * iterator will represent all the in-scope namespaces, without duplicates, and respecting namespace
     * undeclarations. It does not include the XML namespace.
     */

    public static Iterator<NamespaceBinding> iterateNamespaces(/*@NotNull*/ NodeInfo element) {
        if (element.getNodeKind() == Type.ELEMENT) {
            return new NamespaceIterator(element);
        } else {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    /**
     * Send all the in-scope namespaces for a node (except the XML namespace) to a specified receiver
     * @param element the element in question (the method does nothing if this is not an element)
     * @param receiver the receiver to which the namespaces are notified
     */

    public static void sendNamespaces(/*@NotNull*/ NodeInfo element, /*@NotNull*/ Receiver receiver) throws XPathException {
        if (element.getNodeKind() == Type.ELEMENT) {
            boolean foundDefault = false;
            for (Iterator<NamespaceBinding> iter = iterateNamespaces(element); iter.hasNext();) {
                NamespaceBinding nb = iter.next();
                if (nb.getPrefix().length()==0) {
                    foundDefault = true;
                }
                receiver.namespace(nb, 0);
            }
//            if (!foundDefault) {
//                // see bug 5857
//                receiver.namespace(NamespaceBinding.DEFAULT_UNDECLARATION, 0);
//            }
        }
    }

    private NamespaceIterator(/*@NotNull*/ NodeInfo element) {
        this.element = element;
        undeclaredPrefixes = new HashSet(8);
        index = 0;
        localDeclarations = element.getDeclaredNamespaces(null);
    }

    public boolean hasNext() {
        if (element == null || (next == null && index != 0)) {
            return false;
        }
        advance();
        return next != null;
    }

    /*@Nullable*/ public NamespaceBinding next() {
        return next;
    }

    private void advance() {
        while (true) {
            boolean ascend = index >= localDeclarations.length;
            NamespaceBinding nsCode = null;
            if (!ascend) {
                nsCode = localDeclarations[index++];
                ascend = nsCode == null;
            }
            if (ascend) {
                element = element.getParent();
                if (element != null && element.getNodeKind() == Type.ELEMENT) {
                    localDeclarations = element.getDeclaredNamespaces(localDeclarations);
                    index = 0;
                    continue;
                } else {
                    next = null;
                    return;
                }
            }
            String uri = nsCode.getURI();
            String prefix = nsCode.getPrefix();
            if (uri.length()==0) {
                // this is an undeclaration
                undeclaredPrefixes.add(prefix);
            } else {
                if (undeclaredPrefixes.add(prefix)) {
                    // it was added, so it's new, so return it
                    next = nsCode;
                    return;
                }
                // else it wasn't added, so we've already seen it
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
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