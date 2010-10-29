package net.sf.saxon.om;

import java.io.File;

/**
 * This class encapsulates a string used as the value of the document-uri() property of a document,
 * together with a normalized representation of the string used for equality comparisons. The idea
 * is that on Windows systems, document URIs are compared using case-blind comparison, but the original
 * case is retained for display purposes.
 */
public class DocumentURI {

    public final static boolean CASE_BLIND_FILES = new File("a").equals(new File("A"));

    private String displayValue;
    private String normalizedValue;

    /**
     * Create a DocumentURI object that wraps a given URI
     * @param uri the URI to be wrapped. Must not be null
     * @throws NullPointerException if uri is null
     */

    public DocumentURI(String uri) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.displayValue = uri;
        this.normalizedValue = normalizeURI(uri);
    }

    @Override
    public String toString() {
        return displayValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DocumentURI && normalizedValue.equals(((DocumentURI)obj).normalizedValue);
    }

    @Override
    public int hashCode() {
        return normalizedValue.hashCode();
    }

    /**
     * Normalize the representation of file: URIs to give better equality matching than straight
     * string comparison. The main purpose is (a) to eliminate the distinction between "file:/" and
     * "file:///", and (b) to normalize case in the case of Windows filenames: especially the distinction
     * between "file:/C:" and "file:/c:".
     * @param uri the URI to be normalized
     * @return the normalized URI.
     */

    public static String normalizeURI(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("FILE:")) {
            uri = "file:" + uri.substring(5);
        }
        if (uri.startsWith("file:")) {
            if (uri.startsWith("file:///")) {
                uri = "file:/" + uri.substring(8);
            }
            if (CASE_BLIND_FILES) {
                uri = uri.toLowerCase();
            }
        }
        return uri;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



