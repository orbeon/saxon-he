package net.sf.saxon.tinytree;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.value.Value;

/**
  * A node in the XML parse tree representing a text node with compressed whitespace content
  * @author Michael H. Kay
  */

public final class WhitespaceTextImpl extends TinyNodeImpl {

    /**
     * Create a compressed whitespace text node
     * @param tree the tree to contain the node
     * @param nodeNr the internal node number
     */

    public WhitespaceTextImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }

    /**
    * Return the character value of the node.
    * @return the string value of the node
    */

    public String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String. For a WhitespaceTextImpl node, it avoids the
     * cost of decompressing the whitespace
     */

    public CharSequence getStringValueCS() {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        return new CompressedWhitespace(value);
    }

    /**
     * Static method to get the string value of a text node without first constructing the node object
     * @param tree the tree
     * @param nodeNr the node number of the text node
     * @return the string value of the text node
     */

    public static CharSequence getStringValueCS(TinyTree tree, int nodeNr) {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        return new CompressedWhitespace(value);
    }

   /**
     * Static method to get the string value of a text node and append it to a supplied buffer
     * without first constructing the node object
     * @param tree the tree
     * @param nodeNr the node number of the text node
     * @param buffer a buffer to which the string value will be appended
     */

    public static void appendStringValue(TinyTree tree, int nodeNr, FastStringBuffer buffer) {
        long value = ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
        CompressedWhitespace.uncompress(value, buffer);
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link net.sf.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     * @return the typed value. This will either be a single AtomicValue or a Value whose items are
     *         atomic values.
     * @since 8.5
     */

    public Value atomize() throws XPathException {
        return new UntypedAtomicValue(getStringValueCS());
    }

    /**
     * Get the typed value of the item.
     * <p/>
     * For a node, this is the typed value as defined in the XPath 2.0 data model. Since a node
     * may have a list-valued data type, the typed value is in general a sequence, and it is returned
     * in the form of a SequenceIterator.
     * <p/>
     * If the node has not been validated against a schema, the typed value
     * will be the same as the string value, either as an instance of xs:string or as an instance
     * of xs:untypedAtomic, depending on the node kind.
     * <p/>
     * For an atomic value, this method returns an iterator over a singleton sequence containing
     * the atomic value itself.
     * @return an iterator over the items in the typed value of the node or atomic value. The
     *         items returned by this iterator will always be atomic values.
     * @throws net.sf.saxon.trans.XPathException
     *          where no typed value is available, for example in the case of
     *          an element with complex content
     * @since 8.4
     */

    public SequenceIterator getTypedValue() throws XPathException {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(getStringValueCS()));
    }

    /**
     * Static method to get the "long" value representing the content of a whitespace text node
     * @param tree the TinyTree
     * @param nodeNr the internal node number
     * @return a value representing the compressed whitespace content
     * @see CompressedWhitespace
     */

    public static long getLongValue(TinyTree tree, int nodeNr) {
        return ((long)tree.alpha[nodeNr]<<32) | ((long)tree.beta[nodeNr] & 0xffffffffL);
    }

    /**
    * Return the type of node.
    * @return Type.TEXT
    */

    public final int getNodeKind() {
        return Type.TEXT;
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.characters(getStringValueCS(), 0, 0);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
