package net.sf.saxon.om;


import net.sf.saxon.event.Stripper;

import java.util.Arrays;

/**
 * A whitespace stripping rule that strips whitespace according to the rules defined for XSLT stylesheets
 */

public class StylesheetSpaceStrippingRule implements SpaceStrippingRule {

    //    Any child of one of the following elements is removed from the tree,
    //    regardless of any xml:space attributes. Note that this array must be in numeric
    //    order for binary chop to work correctly.

    private static final int[] specials = {
            StandardNames.XSL_ANALYZE_STRING,
            StandardNames.XSL_APPLY_IMPORTS,
            StandardNames.XSL_APPLY_TEMPLATES,
            StandardNames.XSL_ATTRIBUTE_SET,
            StandardNames.XSL_CALL_TEMPLATE,
            StandardNames.XSL_CHARACTER_MAP,
            StandardNames.XSL_CHOOSE,
            StandardNames.XSL_EVALUATE,
            StandardNames.XSL_MERGE,
            StandardNames.XSL_MERGE_SOURCE,
            StandardNames.XSL_NEXT_ITERATION,
            StandardNames.XSL_NEXT_MATCH,
            StandardNames.XSL_STYLESHEET,
            StandardNames.XSL_TRANSFORM
    };

    private NamePool namePool;

    public StylesheetSpaceStrippingRule(NamePool pool) {
        this.namePool = pool;
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
     * @param elementName identifies the element being tested
     */

    public byte isSpacePreserving(/*@NotNull*/ NodeName elementName) {
        if (!elementName.hasFingerprint()) {
            elementName.allocateNameCode(namePool);
        }
        int fingerprint = elementName.getFingerprint();
        if (fingerprint == (StandardNames.XSL_TEXT & NamePool.FP_MASK)) {
            return Stripper.ALWAYS_PRESERVE;
        }

        if (Arrays.binarySearch(specials, fingerprint) >= 0) {
            return Stripper.ALWAYS_STRIP;
        }

        return Stripper.STRIP_DEFAULT;
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