package net.sf.saxon.expr.number;

import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;

/**
 * A RegularGroupFormatter is a NumericGroupFormatter that inserts a separator
 * at constant intervals through a number: for example, a comma after every three
 * digits counting from the right.
 */

public class RegularGroupFormatter extends NumericGroupFormatter {
    
    private int groupSize;
    private String groupSeparator;

    /**
     * Create a RegularGroupFormatter
     * @param grpSize the grouping size. If zero, no grouping separators are inserted
     * @param grpSep the grouping separator (normally a single character, but may be a surrogate pair)
     */
    
    public RegularGroupFormatter(int grpSize, String grpSep){
        groupSize = grpSize;
        groupSeparator = grpSep;
    }

    @Override
    public String format(FastStringBuffer value) {
        int [] valueEx = StringValue.expand(value); 
        int [] groupSeparatorVal = StringValue.expand(groupSeparator);
        FastStringBuffer temp = new FastStringBuffer(FastStringBuffer.TINY);
        if (groupSize>0) {
            for (int i=valueEx.length-1,j=0; i>=0; i--, j++) {
                if (j!=0 && (j % groupSize) == 0) {
                    temp.prependWideChar(groupSeparatorVal[0]);
                }
                temp.prependWideChar(valueEx[i]);
            }
            return temp.toString();
        } 
        return value.toString();
    }

    /**
     * Get the grouping separator to be used. If more than one is used, return the last.
     * If no grouping separators are used, return null
     *
     * @return the grouping separator
     */
    @Override
    public String getSeparator() {
        return groupSeparator;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is O'Neil Delpratt, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//


