package net.sf.saxon.trans;

import net.sf.saxon.number.Numberer;

import java.io.Serializable;
import java.util.Properties;

/**
 * Interface allowing localization modules for different languages to be dynamically loaded
 */
public abstract class LocalizerFactory implements Serializable {


    /**
     * Set properties for a particular language. The properties available are specific to the
     * LocalizerFactory in use. Default implementation does nothing.
     * @param lang the language
     * @param properties properties of this language
     * @since 9.2
     */

    public void setLanguageProperties(String lang, Properties properties) {
        // no action
    }

 
    /**
     * Get the numberer for a given language
     * @param language the language code (for example "de" or "en-GB"). The value may be null,
     * in which case a default language should be assumed.
     * @param country the country, as used in format-date(). This is not the country associated
     * with the language, but the one associated with the date to be formatted. It is primarily
     * used to determine a civil time zone name. The value may be null, in which case a default
     * country should be assumed.
     * @return the appropriate numberer, or null if none is available (in which case the English
     * numberer will be used)
     */

    public abstract Numberer getNumberer(String language, String country);

    /**
     * Copy the state of this factory to create a new LocalizerFactory. The default implementation
     * returns this LocalizerFactory unchanged (which should only be done if it is immutable).
     */

    public LocalizerFactory copy() {
        return this;
    }
}

// Copyright (c) Saxonica Limited 2009.

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

