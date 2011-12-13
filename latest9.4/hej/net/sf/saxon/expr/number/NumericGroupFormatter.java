package net.sf.saxon.expr.number;

import net.sf.saxon.tree.util.FastStringBuffer;

import java.io.Serializable;

/**
 * A NumericGroupFormatter is responsible for insertion of grouping separators
 * into a formatted number (for example, reformatting "1234" as "1,234").
 */

public abstract class NumericGroupFormatter implements Serializable {

    protected String adjustedPicture;

    /**
     * Get the adjusted (preprocessed) picture
     */

    public String getAdjustedPicture() {
        return adjustedPicture;
    }

    /**
     * Reformat a number to add grouping separators
     * @param value a buffer holding the number to be reformatted
     * @return the reformatted number
     */

    public abstract String format(FastStringBuffer value);

    /**
     * Get the grouping separator to be used. If more than one is used, return the last.
     * If no grouping separators are used, return null
     * @return the grouping separator
     */

    /*@Nullable*/ public abstract String getSeparator();
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