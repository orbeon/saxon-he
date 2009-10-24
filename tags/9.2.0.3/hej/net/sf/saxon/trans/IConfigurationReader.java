package net.sf.saxon.trans;

import net.sf.saxon.Configuration;

import javax.xml.transform.Source;

/**
 * The implementation of this interface is in Saxon-PE/EE. It reads a configuration file to
 * construct a Configuration.
 */
public interface IConfigurationReader {

     /**
     * Create a Configuration based on the contents of this configuration file
     * @param source the Source of the configuration file
     * @return the constructed Configuration
     * @throws XPathException
     */

    public Configuration makeConfiguration(Source source) throws XPathException;
    
}

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

