////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

/**
 * An implementation of NodeName for the common case of a name in no namespace
 */
public final class NoNamespaceName implements NodeName {

    private String localName;
    private int nameCode = -1;

    public NoNamespaceName(String localName) {
        this.localName = localName;
    }

    public NoNamespaceName(String localName, int nameCode) {
        this.localName = localName;
        this.nameCode = nameCode;
    }

    /**
     * Get the prefix of the QName.
     *
     * @return the prefix. Returns the empty string if the name is unprefixed.
     */
    public String getPrefix() {
        return "";
    }

    /**
     * Get the namespace URI of the QName.
     *
     * @return the URI. Returns the empty string to represent the no-namespace
     */
    public String getURI() {
        return "";
    }

    /**
     * Get the local part of the QName
     *
     * @return the local part of the QName
     */
    public String getLocalPart() {
        return localName;
    }

    /**
     * Get the display name, that is the lexical QName in the form [prefix:]local-part
     *
     * @return the lexical QName
     */
    public String getDisplayName() {
        return localName;
    }

    /**
     * Get the name in the form of a StructuredQName
     *
     * @return the name in the form of a StructuredQName
     */
    public StructuredQName getStructuredQName() {
        return new StructuredQName("", "", getLocalPart());
    }

    /**
     * Test whether this name is in the same namespace as another name
     *
     * @return true if the two names are in the same namespace
     */
    public boolean isInSameNamespace(NodeName other) {
        return other.getURI().length() == 0;
    }

    /**
     * Test whether this name is in a given namespace
     *
     * @param ns the namespace to be tested against
     * @return true if the name is in the specified namespace
     */
    public boolean hasURI(String ns) {
        return ns.length() == 0;
    }

    /**
     * Get a {@link net.sf.saxon.om.NamespaceBinding} whose (prefix, uri) pair are the prefix and URI of this
     * node name
     *
     * @return the corresponding NamespaceBinding
     */
    public NamespaceBinding getNamespaceBinding() {
        return NamespaceBinding.DEFAULT_UNDECLARATION;
    }

    /**
     * Ask whether this node name representation has a known namecode and fingerprint
     *
     * @return true if the methods getFingerprint() and getNameCode() will
     *         return a result other than -1
     */
    public boolean hasFingerprint() {
        return nameCode != -1;
    }

    /**
     * Get the fingerprint of this name if known. This method should not to any work to allocate
     * a fingerprint if none is already available
     *
     * @return the fingerprint if known; otherwise -1
     */
    public int getFingerprint() {
        return nameCode;
    }

    /**
     * Get the nameCode of this name if known. This method should not to any work to allocate
     * a nameCode if none is already available
     *
     * @return the nameCode if known; otherwise -1
     */
    public int getNameCode() {
        return nameCode;
    }

    /**
     * Get the nameCode of this name, allocating a new code from the namepool if necessary
     *
     * @param namePool the NamePool used to allocate the name
     * @return a nameCode for this name, newly allocated if necessary
     */
    public int allocateNameCode(NamePool namePool) {
        if (nameCode == -1) {
            return nameCode = namePool.allocate("", "", localName);
        } else {
            return nameCode;
        }
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return StructuredQName.computeHashCode("", localName);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(/*@NotNull*/ Object obj) {
        return obj instanceof NodeName &&
                ((NodeName) obj).getLocalPart().equals(localName) &&
                ((NodeName) obj).hasURI("");
    }

    @Override
    public String toString() {
        return localName;
    }

    /**
     * Determine whether two IdentityComparable objects are identical. This is a stronger
     * test than equality (even schema-equality); for example two dateTime values are not identical unless
     * they are in the same timezone.
     *
     * @param other the value to compare with
     * @return true if the two values are indentical, false otherwise
     */
    public boolean isIdentical(IdentityComparable other) {
        return other instanceof NodeName &&
                this.equals(other) &&
                this.getPrefix().equals(((NodeName) other).getPrefix());
    }

    /**
     * Get a hashCode that offers the guarantee that if A.isIdentical(B), then A.identityHashCode() == B.identityHashCode()
     *
     * @return a hashCode suitable for use when testing for identity.
     */
    public int identityHashCode() {
        return hashCode() ^ getPrefix().hashCode();
    }
}
