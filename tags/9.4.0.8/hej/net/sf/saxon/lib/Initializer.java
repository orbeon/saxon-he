package net.sf.saxon.lib;

import net.sf.saxon.Configuration;

import javax.xml.transform.TransformerException;

/**
 * This interface can be implemented by users (there are no implementations in Saxon itself). It is
 * used only when Saxon is invoked from the command line, and the -init:class option is used on the command
 * line to nominate an implementation of this class. The initialize() method of the supplied class will
 * then be called to perform any user-defined initialization of the Configuration.
 *
 * The initializer is invoked after all other options on the command line have been processed; the initializer
 * can therefore examine the Configuration to see what options have been set, and it can modify them accordingly.
 *
 * @since 9.3
 */
public interface Initializer {

    /**
     * Initialize the Configuration
     * @param config the Configuration to be initialized
     * @throws TransformerException if the initializer chooses to abort processing for any reason
     */

    public void initialize(Configuration config) throws TransformerException;
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
