package net.sf.saxon.serialize.charcode;

/**
* This class defines properties of the US-ASCII character set
*/

public class ASCIICharacterSet implements CharacterSet {

    public static final ASCIICharacterSet theInstance = new ASCIICharacterSet();

    private ASCIICharacterSet() {}

    public static ASCIICharacterSet getInstance() {
        return theInstance;
    }

    public final boolean inCharset(int c) {
        return c <= 0x7f;
    }

    /*@NotNull*/ public String getCanonicalName() {
        return "US-ASCII";
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