package net.sf.saxon.lib;

import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NotationSet;
import net.sf.saxon.type.StringToDouble;

import java.io.Serializable;

/**
 * This class defines a set of rules for converting between different atomic types. It handles the variations
 * that arise between different versions of the W3C specifications, for example the changes in Name syntax
 * between XML 1.0 and XML 1.1, the introduction of "+INF" as a permitted xs:double value in XSD 1.1, and so on.
 *
 * <p>It is possible to nominate a customized <code>ConversionRules</code> object at the level of the
 * {@link net.sf.saxon.Configuration}, either by instantiating this class and changing the properties, or
 * by subclassing.</p>
 *
 * @since 9.3
 *
 * @see net.sf.saxon.Configuration#setConversionRules(ConversionRules) 
 */

public class ConversionRules implements Serializable {

    private NameChecker nameChecker;
    private StringToDouble stringToDouble;
    private NotationSet notationSet; // may be null
    private URIChecker uriChecker;
    private boolean allowYearZero;

    /**
     * Set the class that will be used to check whether XML names are valid.
     * @param checker the class to be used for checking names. There are variants of this
     * class depending on which version/edition of the XML specification is in use.
     */

    public void setNameChecker(NameChecker checker) {
        this.nameChecker = checker;
    }

    /**
     * Get the class that will be used to check whether XML names are valid.
     * @return the class to be used for checking names. There are variants of this
     * class depending on which version/edition of the XML specification is in use.
     */

    public NameChecker getNameChecker() {
        return nameChecker;
    }

    /**
     * Set the converter that will be used for converting strings to doubles and floats.
     * @param converter the converter to be used. There are two converters in regular use:
     * they differ only in whether the lexical value "+INF" is recognized as a representation of
     * positive infinity.
     */

    public void setStringToDoubleConverter(StringToDouble converter) {
        this.stringToDouble = converter;
    }

    /**
     * Get the converter that will be used for converting strings to doubles and floats.
     * @return the converter to be used. There are two converters in regular use:
     * they differ only in whether the lexical value "+INF" is recognized as a representation of
     * positive infinity.
     */

    public StringToDouble getStringToDoubleConverter() {
        return stringToDouble;
    }

    /**
     * Specify the set of notations that are accepted by xs:NOTATION and its subclasses. This is to
     * support the rule that for a notation to be valid, it must be declared in an xs:notation declaration
     * in the schema
     * @param notations the set of notations that are recognized; or null, to indicate that all notation
     * names are accepted
     */

    public void setNotationSet(NotationSet notations) {
        this.notationSet = notations;
    }

    /**
     * Ask whether a given notation is accepted by xs:NOTATION and its subclasses. This is to
     * support the rule that for a notation to be valid, it must be declared in an xs:notation declaration
     * in the schema
     * @param uri the namespace URI of the notation
     * @param local the local part of the name of the notation
     * @return true if the notation is in the set of recognized notation names
     */


    public boolean isDeclaredNotation(String uri, String local) {
        if (notationSet == null) {
            return true;    // in the absence of a known configuration, treat all notations as valid
        } else {
            return notationSet.isDeclaredNotation(uri, local);
        }
    }

    /**
     * Set the class to be used for checking URI values. By default, no checking takes place.
     * @param checker an object to be used for checking URIs; or null if any string is accepted as an anyURI value
     */

    public void setURIChecker(URIChecker checker) {
        this.uriChecker = checker;
    }

    /**
     * Ask whether a string is a valid instance of xs:anyURI according to the rules
     * defined by the current URIChecker
     * @param string the string to be checked against the rules for URIs
     */

    public boolean isValidURI(CharSequence string) {
        if (uriChecker == null) {
            return true;
        } else {
            return uriChecker.isValidURI(string);
        }
    }

    /**
     * Say whether year zero is permitted in dates. By default it is not permitted when XSD 1.0 is in use,
     * but it is permitted when XSD 1.1 is used.
     * @param allowed true if year zero is permitted
     */

    public void setAllowYearZero(boolean allowed) {
        allowYearZero = allowed;
    }

    /**
     * Ask whether  year zero is permitted in dates. By default it is not permitted when XSD 1.0 is in use,
     * but it is permitted when XSD 1.1 is used.
     * @return true if year zero is permitted
     */

    public boolean isAllowYearZero() {
        return allowYearZero;
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



