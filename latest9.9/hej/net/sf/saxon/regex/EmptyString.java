////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.regex;

/**
 * An implementation of UnicodeString representing a zero-length string. This
 * is a singleton class with only one instance.
 */
public final class EmptyString extends UnicodeString {

    public final static EmptyString THE_INSTANCE = new EmptyString();

    private EmptyString() {}; 


    public EmptyString uSubstring(int beginIndex, int endIndex) {
        if (beginIndex == 0 && endIndex == 0) {
            return this;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public int uCharAt(int pos) {
        throw new IndexOutOfBoundsException();
    }

    public int uIndexOf(int search, int pos) {
        return -1;
    }

    public int uLength() {
        return 0;
    }

    public boolean isEnd(int pos) {
        return pos >= 0;
    }

    public String toString() {
        return "";
    }

    public int length() {
        return 0;
    }

    public char charAt(int index) {
        throw new IndexOutOfBoundsException();
    }

    public CharSequence subSequence(int start, int end) {
        if (start == 0 && end == 0) {
            return "";
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

}
