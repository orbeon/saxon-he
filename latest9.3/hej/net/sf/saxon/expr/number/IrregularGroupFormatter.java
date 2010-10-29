package net.sf.saxon.expr.number;

import net.sf.saxon.tree.util.FastStringBuffer;

public class IrregularGroupFormatter extends NumericGroupFormatter {

    private int[] position = null;
    private int[] separator = null;

    public IrregularGroupFormatter(int[] pos, int[] sep) {
        position = pos;
        separator = sep;
    }

    @Override
    public String format(FastStringBuffer value) {
        FastStringBuffer temp = new FastStringBuffer(FastStringBuffer.TINY);
        temp.append(value);
        if (position.length != 0) {
            for (int j = 0; j < position.length; j++) {
                temp.insertWideCharAt(value.length() - position[j], separator[j]);
            }
            return temp.toString();
        } else {
            return value.toString();
        }
    }

    /**
     * Get the grouping separator to be used. If more than one is used, return the last.
     * If no grouping separators are used, return null
     *
     * @return the grouping separator
     */
    @Override
    public String getSeparator() {
        if (separator.length == 0) {
            return null;
        } else {
            int sep = separator[separator.length - 1];
            FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.TINY);
            fsb.appendWideChar(sep);
            return fsb.toString();
        }
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



