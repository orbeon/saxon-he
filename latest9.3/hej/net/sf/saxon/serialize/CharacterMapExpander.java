package net.sf.saxon.serialize;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.trans.XPathException;

/**
* CharacterMapExpander: This ProxyReceiver expands characters occurring in a character map,
 * as specified by the XSLT 2.0 xsl:character-map declaration
*
* @author Michael Kay
*/


public class CharacterMapExpander extends ProxyReceiver {

    private CharacterMap charMap;
    private boolean useNullMarkers = true;

    /**
     * Set the character maps to be used by this CharacterMapExpander.
     * They are merged into a single character map if there is more than one.
     */

    public void setCharacterMap(CharacterMap map) {
        charMap = map;
    }

    /**
     * Indicate whether the result of character mapping should be marked using NUL
     * characters to prevent subsequent XML or HTML character escaping. The default value
     * is true (used for the XML and HTML output methods); the value false is used by the text
     * output method.
     */

    public void setUseNullMarkers(boolean use) {
        useNullMarkers = use;
    }

    /**
     * Output an attribute
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        if ((properties & ReceiverOptions.DISABLE_CHARACTER_MAPS) == 0) {
            CharSequence mapped = charMap.map(value, useNullMarkers);
            if (mapped == value) {
                // no mapping was done
                nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
            } else {
                nextReceiver.attribute(nameCode, typeCode, mapped,
                        locationId,
                        (properties | ReceiverOptions.USE_NULL_MARKERS) & ~ReceiverOptions.NO_SPECIAL_CHARS);
            }
        } else {
            nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
    * Output character data
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            CharSequence mapped = charMap.map(chars, useNullMarkers);
            if (mapped != chars) {
                properties = (properties | ReceiverOptions.USE_NULL_MARKERS) & ~ReceiverOptions.NO_SPECIAL_CHARS;
            }
            nextReceiver.characters(mapped, locationId, properties);
        } else {
            // if the user requests disable-output-escaping, this overrides the character
            // mapping
            nextReceiver.characters(chars, locationId, properties);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

