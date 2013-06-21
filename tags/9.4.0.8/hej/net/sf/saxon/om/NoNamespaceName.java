package net.sf.saxon.om;

/**
 * An implementation of NodeName for the common case of a name in no namespace
 */
public class NoNamespaceName implements NodeName {

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
        return other.getURI().length()==0;
    }

    /**
     * Test whether this name is in a given namespace
     *
     * @param ns the namespace to be tested against
     * @return true if the name is in the specified namespace
     */
    public boolean isInNamespace(String ns) {
        return ns.length()==0;
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
            return (nameCode = namePool.allocate("", "", localName));
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
                ((NodeName) obj).isInNamespace("");
    }

    @Override
    public String toString() {
        return localName;
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