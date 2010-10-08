package net.sf.saxon.charcode;

import net.sf.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
* This class delivers a CharacterSet object for a given named encoding.
 *
 * It maintains a mapping from character set names to class names, and a separate mapping
 * from character set names to instances of those classes. This means that a class is not
 * actually instantiated until the encoding is used, but once instantiated, the same instance
 * is used whenever that encoding is used again in the same Configuration.
 *
 * Note that the purpose of the CharacterSet object is only to record which Unicode
 * characters are represented in the encoding, so that non-encodable characters can
 * be represented as XML or HTML character references. The actual translation from Unicode
 * codepoints to bytes in the chosen encoding is left to the Java IO library.
*/


public class CharacterSetFactory {

    //private HashMap<String, String> characterSetClassNames = new HashMap<String, String>(40);
    private HashMap<String, CharacterSet> characterSets = new HashMap<String, CharacterSet>(10);

    /**
     * Class has a single instance per Configuration
     */
    public CharacterSetFactory() {
        HashMap<String, CharacterSet> c = characterSets;
        UTF8CharacterSet utf8 = UTF8CharacterSet.getInstance();
        c.put("utf8", utf8);
        UTF16CharacterSet utf16 = UTF16CharacterSet.getInstance();
        c.put("utf16", utf16);
        ASCIICharacterSet acs = ASCIICharacterSet.getInstance();
        c.put("ascii", acs);
        c.put("iso646", acs);
        c.put("usascii", acs);
        ISO88591CharacterSet lcs = ISO88591CharacterSet.getInstance();
        c.put("iso88591", lcs);
    }

    /**
     * Register an implementation of a character set, using the class name
     * @param encoding the name of the character set
     * @param charSet the name of a class that implements {@link net.sf.saxon.charcode.CharacterSet}
     */

    public void setCharacterSetImplementation(String encoding, CharacterSet charSet) {
        characterSets.put(normalizeCharsetName(encoding), charSet);
    }

    /**
     * Normalize the name of a character set
     * @param name the character set name
     * @return the normalized name (removes hyphens and underscores and converts to lower-case)
     */

    private String normalizeCharsetName(String name) {
        return name.replace("-", "").replace("_", "").toLowerCase();
    }

    /**
     * Make a CharacterSet appropriate to the encoding
     * @param details the serialization properties
     * @return the constructed CharacterSet
    */

    public CharacterSet getCharacterSet(Properties details)
    throws XPathException {

        String encoding = details.getProperty(OutputKeys.ENCODING);
        if (encoding == null) {
            return UTF8CharacterSet.getInstance();
        } else {
            String encodingKey = normalizeCharsetName(encoding);
            CharacterSet cs = characterSets.get(encodingKey);
            if (cs != null) {
                return cs;
            }

            // Otherwise see if the Java VM knows anything about the character set

            Charset charset;
            try {
                charset = Charset.forName(encoding);
                CharacterSet res = JavaCharacterSet.makeCharSet(charset);
                characterSets.put(encodingKey, res);
                return res;
            } catch (IllegalCharsetNameException err) {
                XPathException e = new XPathException("Invalid encoding name: " + encoding);
                e.setErrorCode("SESU0007");
                throw e;
            } catch (UnsupportedCharsetException err) {
                XPathException e = new XPathException("Unknown encoding requested: " + encoding);
                e.setErrorCode("SESU0007");
                throw e;
            }
        }
    }

    /**
     * Main program is a utility to give a list of the character sets supported
     * by the Java VM
     * @param args command line arguments
     */

    public static void main(String[] args) throws Exception {
        System.err.println("Available Character Sets in the java.nio package for this Java VM:");
        Iterator iter = Charset.availableCharsets().keySet().iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            System.err.println("    " + s);
        }
        System.err.println("Registered Character Sets in Saxon:");
        CharacterSetFactory factory = new CharacterSetFactory();
        iter = factory.characterSets.keySet().iterator();
        while (iter.hasNext()) {
            String s = (String) iter.next();
            System.err.println("    " + s + " = " + factory.characterSets.get(s).getClass().getName());
        }
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
