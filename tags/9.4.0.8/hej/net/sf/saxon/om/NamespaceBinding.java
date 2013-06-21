package net.sf.saxon.om;

import net.sf.saxon.lib.NamespaceConstant;

/**
 * Represents the binding of a prefix to a URI. Also, in some contexts, represents an unbinding, by
 * virtue of the URI being set to a zero length string.
 *
 * @since 9.4
 */
public final class NamespaceBinding {

    private String prefix;
    private String uri;

    public final static NamespaceBinding XML = new NamespaceBinding("xml", NamespaceConstant.XML);
    public final static NamespaceBinding DEFAULT_UNDECLARATION = new NamespaceBinding("", "");

    public final static NamespaceBinding[] EMPTY_ARRAY = new NamespaceBinding[0];

    /**
     * Create a binding of a prefix to a URI
     *
     * @param prefix the prefix: either an NCName, or a zero-length string to bind the default namespace.
     *               Must not be null.
     * @param uri    the namespace URI: either a URI, or a zero-length string to unbind the prefix. Must
     *               not be null.
     */

    public NamespaceBinding(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
        if (prefix == null || uri == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Create a binding of a prefix to a URI. Static factory method for the convenience of compiled bytecode;
     * reuses standard NamespaceBinding objects where possible
     *
     * @param prefix the prefix: either an NCName, or a zero-length string to bind the default namespace.
     *               Must not be null.
     * @param uri    the namespace URI: either a URI, or a zero-length string to unbind the prefix. Must
     *               not be null.
     * @return the namespace binding object
     */

    public static NamespaceBinding makeNamespaceBinding(CharSequence prefix, CharSequence uri) {
        if (prefix.length() == 0 && uri.length() == 0) {
            return DEFAULT_UNDECLARATION;
        } else if (prefix.equals("xml") && uri.equals(NamespaceConstant.XML)) {
            return XML;
        } else {
            return new NamespaceBinding(prefix.toString(), uri.toString());
        }
    }

    /**
     * Get the prefix part of the binding
     *
     * @return the prefix. Never null. The zero-length string indicates a binding for the default namespace.
     */

    public String getPrefix() {
        return prefix;
    }

    /**
     * Get the URI part of the binding
     *
     * @return the URI. Never null. The zero-length string indicates an unbinding of the prefix. For the
     *         default namespace (prefix="") this indicates that the prefix refers to names in no namespace; for other
     *         prefixes, it indicates that the prefix is not bound to any namespace and therefore cannot be used.
     */

    public String getURI() {
        return uri;
    }

    /**
     * Ask whether this is a binding for the XML namespace
     *
     * @return true if this is the binding of the prefix "xml" to the standard XML namespace.
     */

    public boolean isXmlNamespace() {
        return prefix.equals("xml");
    }

    /**
     * Ask whether this is an undeclaration of the default prefix, that is, a namespace binding
     * corresponding to <code>xmlns=""</code>
     *
     * @return true if this corresponding to <code>xmlns=""</code>
     */

    public boolean isDefaultUndeclaration() {
        return prefix.length()==0 && uri.length()==0;
    }

    /**
     * Test if this namespace binding is the same as another
     *
     * @param obj the comparand
     * @return true if the comparand is a Namespace binding of the same prefix to the same URI
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof NamespaceBinding &&
                prefix.equals(((NamespaceBinding) obj).getPrefix()) &&
                uri.equals(((NamespaceBinding) obj).getURI());
    }

    @Override
    public int hashCode() {
        return prefix.hashCode() ^ uri.hashCode();
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