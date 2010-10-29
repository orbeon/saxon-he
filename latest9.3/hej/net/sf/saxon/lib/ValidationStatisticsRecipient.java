package net.sf.saxon.lib;

import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaComponent;

import java.util.Map;

/**
 * Defines a class that is notified of validation statistics at the end of a validation episode
 */
public interface ValidationStatisticsRecipient {

    /**
     * Notify the validation statistics
     * @param statistics the statistics, in the form of a map from schema components (currently,
     * element declarations and schema types) to a count of how often the component
     * was used during the validation episode
     */

    public void notifyValidationStatistics(Map<SchemaComponent, Integer> statistics) throws XPathException;
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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

