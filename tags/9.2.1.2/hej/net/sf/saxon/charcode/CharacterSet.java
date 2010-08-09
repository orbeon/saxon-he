package net.sf.saxon.charcode;


/**
* This interface defines properties of a character set, built in to the Saxon product.
* This is selected in xsl:output using encoding="encoding-name", where
* the mapping from an encoding-name to a class is defined in CharacterSetFactory.
*/

public interface CharacterSet {

    /**
    * Determine if a character is present in the character set
    */

    public boolean inCharset(int ch);

    /**
     * Get the preferred Java name of the character set. Note that Java in many
     * cases also supports a "historic name".
     */

    public String getCanonicalName();

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