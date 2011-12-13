package net.sf.saxon.om;

/**
 * An implementation of NodeName that gets the name of an existing NodeInfo object. Useful when nodes are copied.
 */
public class NameOfNode implements NodeName {

    private NodeInfo node;

    public NameOfNode(NodeInfo node) {
        this.node = node;
    }

    /**
     * Get the prefix of the QName.
     *
     * @return the prefix. Returns the empty string if the name is unprefixed.
     */
    public String getPrefix() {
        return node.getPrefix();
    }

    /**
     * Get the namespace URI of the QName.
     *
     * @return the URI. Returns the empty string to represent the no-namespace
     */
    public String getURI() {
        return node.getURI();
    }

    /**
     * Get the local part of the QName
     *
     * @return the local part of the QName
     */
    public String getLocalPart() {
        return node.getLocalPart();
    }

    /**
     * Get the display name, that is the lexical QName in the form [prefix:]local-part
     *
     * @return the lexical QName
     */
    public String getDisplayName() {
        return node.getDisplayName();
    }

    /**
     * Get the name in the form of a StructuredQName
     *
     * @return the name in the form of a StructuredQName
     */
    public StructuredQName getStructuredQName() {
        return new StructuredQName(getPrefix(), getURI(), getLocalPart());
    }

    /**
     * Test whether this name is in the same namespace as another name
     *
     * @return true if the two names are in the same namespace
     */
    public boolean isInSameNamespace(/*@NotNull*/ NodeName other) {
        return node.getURI().equals(other.getURI());
    }

    /**
     * Test whether this name is in a given namespace
     *
     * @param ns the namespace to be tested against
     * @return true if the name is in the specified namespace
     */
    public boolean isInNamespace(String ns) {
        return node.getURI().equals(ns);
    }

    /**
     * Get a {@link net.sf.saxon.om.NamespaceBinding} whose (prefix, uri) pair are the prefix and URI of this
     * node name
     *
     * @return the corresponding NamespaceBinding
     */

    public NamespaceBinding getNamespaceBinding() {
        return NamespaceBinding.makeNamespaceBinding(getPrefix(), getURI());
    }

    /**
     * Ask whether this node name representation has a known namecode and fingerprint
     *
     * @return true if the methods getFingerprint() and getNameCode() will
     *         return a result other than -1
     */
    public boolean hasFingerprint() {
        return node instanceof FingerprintedNode;
    }

    /**
     * Get the fingerprint of this name if known. This method should not to any work to allocate
     * a fingerprint if none is already available
     *
     * @return the fingerprint if known; otherwise -1
     */
    public int getFingerprint() {
        if (hasFingerprint()) {
            return node.getFingerprint();
        } else {
            return -1;
        }
    }

    /**
     * Get the nameCode of this name if known. This method should not to any work to allocate
     * a nameCode if none is already available
     *
     * @return the nameCode if known; otherwise -1
     */
    public int getNameCode() {
        if (hasFingerprint()) {
            return node.getNameCode();
        } else {
            return -1;
        }
    }

    /**
     * Get the nameCode of this name, allocating a new code from the namepool if necessary
     *
     * @param namePool the NamePool used to allocate the name
     * @return a nameCode for this name, newly allocated if necessary
     */
    public int allocateNameCode(NamePool namePool) {
        return node.getNameCode();
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return StructuredQName.computeHashCode(getURI(), getLocalPart());
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeName) {
            NodeName n = (NodeName)obj;
            if (node instanceof FingerprintedNode && n.hasFingerprint()) {
                return node.getFingerprint() == n.getFingerprint();
            } else {
                return n.getLocalPart().equals(node.getLocalPart()) && n.isInNamespace(node.getURI());
            }
        } else {
            return false;
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