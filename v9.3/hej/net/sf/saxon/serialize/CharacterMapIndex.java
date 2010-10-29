package net.sf.saxon.serialize;

import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class represents a set of named character maps. Each character map in the set is identified by a unique
 * QName.
 */

public class CharacterMapIndex implements Serializable {

    private HashMap<StructuredQName, CharacterMap> index = new HashMap<StructuredQName, CharacterMap>(10);

    public CharacterMapIndex() {}

    public CharacterMap getCharacterMap(StructuredQName name) {
        return index.get(name);
    }

    public void putCharacterMap(StructuredQName name, CharacterMap charMap) {
        index.put(name, charMap);
    }

    /**
     * Make a CharacterMapExpander to handle the character map definitions in the serialization
     * properties.
     * <p>
     * This method is intended for internal use only.
     *
     * @param useMaps the expanded use-character-maps property: a space-separated list of names
     * of character maps to be used, each one expressed as an expanded-QName in Clark notation
     * (that is, {uri}local-name).
     * @param sf the SerializerFactory - used to create a CharacterMapExpander. This callback
     * is provided so that a user-defined SerializerFactory can customize the result of this function,
     * for example by returning a subclass of the standard CharacterMapExpander.
     * @return a CharacterMapExpander if one is required, or null if not (for example, if the
     * useMaps argument is an empty string).
     * @throws net.sf.saxon.trans.XPathException if a name in the useMaps property cannot be resolved to a declared
     * character map.
     */

    public CharacterMapExpander makeCharacterMapExpander(String useMaps, SerializerFactory sf) throws XPathException {
        CharacterMapExpander characterMapExpander = null;
        if (useMaps != null) {
            List<CharacterMap> characterMaps = new ArrayList<CharacterMap>(5);
            StringTokenizer st = new StringTokenizer(useMaps, " \t\n\r", false);
            while (st.hasMoreTokens()) {
                String expandedName = st.nextToken();
                StructuredQName qName = StructuredQName.fromClarkName(expandedName);
                CharacterMap map = getCharacterMap(qName);
                if (map==null) {
                    throw new XPathException("Character map '" + expandedName + "' has not been defined");
                }
                characterMaps.add(map);
            }
            if (!characterMaps.isEmpty()) {
                characterMapExpander = sf.newCharacterMapExpander();
                if (characterMaps.size() == 1) {
                    characterMapExpander.setCharacterMap(characterMaps.get(0));
                } else {
                    characterMapExpander.setCharacterMap(new CharacterMap(characterMaps));
                }
            }
        }
        return characterMapExpander;
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



