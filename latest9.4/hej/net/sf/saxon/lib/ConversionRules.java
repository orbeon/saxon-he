package net.sf.saxon.lib;

import net.sf.saxon.expr.sort.LRUCache;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NotationSet;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.Converter;
import net.sf.saxon.type.StringConverter;
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

    // These two tables need to be thread-local or synchronised to make the caching thread-safe
    private ThreadLocal<LRUCache<Integer, Converter>> converterCache =
            new ThreadLocal<LRUCache<Integer, Converter>>();
    private ThreadLocal<LRUCache<Integer, StringConverter>> stringConverterCache =
            new ThreadLocal<LRUCache<Integer, StringConverter>>();


    public ConversionRules() {

    }

    public ConversionRules copy() {
        ConversionRules cr = new ConversionRules();
        cr.nameChecker = nameChecker;
        cr.stringToDouble = stringToDouble;
        cr.notationSet = notationSet;
        cr.uriChecker = uriChecker;
        cr.allowYearZero = allowYearZero;
        return cr;
    }


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

    public void setNotationSet(/*@Nullable*/ NotationSet notations) {
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
        //noinspection SimplifiableIfStatement
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
     * @return true if the string represents a valid xs:anyURI value
     */

    public boolean isValidURI(CharSequence string) {
        return uriChecker == null || uriChecker.isValidURI(string);
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

    /**
     * Get a Converter for a given pair of atomic types. These can be primitive types,
     * derived types, or user-defined types. The converter implements the casting rules.
     * @param source the source type
     * @param target the target type
     * @return a Converter if conversion between the two types is possible; or null otherwise
     */

    /*@Nullable*/ public Converter getConverter(AtomicType source, AtomicType target) {
        // For a lookup key, use the primitive type of the source type (always 10 bits) and the
        // fingerprint of the target type (20 bits)
        int key = (source.getPrimitiveType() << 20) | target.getFingerprint();
        LRUCache<Integer, Converter> converters = converterCache.get();
        if (converters == null) {
            converters = new LRUCache<Integer, Converter>(50);
            converterCache.set(converters);
        }
        Converter converter = converters.get(key);
        if (converter == null) {
            converter = Converter.getConverter(source, target, this);
            if (converter != null) {
                converters.put(key, converter);
            } else {
                return null;
            }
        }
        return converter;
    }

    /**
     * Get a Converter that converts from strings to a given atomic type. These can be primitive types,
     * derived types, or user-defined types. The converter implements the casting rules.
     * @param target the target type
     * @return a Converter if conversion between the two types is possible; or null otherwise
     */

    public StringConverter getStringConverter(AtomicType target) {
        int key = target.getFingerprint();
        LRUCache<Integer, StringConverter> stringConverters = stringConverterCache.get();
        if (stringConverters == null) {
            stringConverters = new LRUCache<Integer, StringConverter>(50);
            stringConverterCache.set(stringConverters);
        }
        StringConverter converter = stringConverters.get(key);
        if (converter == null) {
            converter = StringConverter.getStringConverter(target, this);
            stringConverters.put(key, converter);
        }
        return converter;
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