package net.sf.saxon.lib;

/**
 * This interface defines a method for checking whether a string is considered to be a valid URI.
 *
 * <p>A user-supplied implementation of this class can be set in a customized instance of
 * {@link ConversionRules}, which can be set in the configuration using
 * {@link net.sf.saxon.Configuration#setConversionRules(ConversionRules)}</p>
 *
 * <p>A user-supplied implementation can be written either from scratch, or by reference to the
 * system-supplied implementation {@link StandardURIChecker}.
 */
public interface URIChecker {

    /**
     * Check whether a given string is considered valid according to the rules of the xs:anyURI type.
     * <p>This method is called during schema validation, and when casting string to xs:anyURI. It is not
     * used when the xs:anyURI type is used as a return value from methods such as namespace-uri() or
     * namespace-uri-from-QName() - in such cases no checking is applied to the name.</p>
     * @param value the string to be checked
     * @return true if the string is considered to represent a valid URI
     */

    boolean isValidURI(CharSequence value);
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