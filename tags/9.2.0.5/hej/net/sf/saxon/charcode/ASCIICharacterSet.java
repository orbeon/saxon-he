package net.sf.saxon.charcode;

/**
* This class defines properties of the US-ASCII character set
*/

public class ASCIICharacterSet implements CharacterSet {

    public static ASCIICharacterSet theInstance = new ASCIICharacterSet();

    private ASCIICharacterSet() {}

    public static ASCIICharacterSet getInstance() {
        return theInstance;
    }

    public final boolean inCharset(int c) {
        return c <= 0x7f;
    }

    public String getCanonicalName() {
        return "US-ASCII";
    }
}

// Copyright (c) 2009 Saxonica Limited. All rights reserved.

// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Saxonica Limited.
//
// Portions created by __ are Copyright (C) __. All Rights Reserved.
//
// Contributor(s): 	None
//