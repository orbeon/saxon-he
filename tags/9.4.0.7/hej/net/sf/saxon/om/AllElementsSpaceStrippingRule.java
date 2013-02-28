package net.sf.saxon.om;


import net.sf.saxon.event.Stripper;

/**
 * A whitespace stripping rule that strips all elements unless xml:space indicates that whitespace
 * should be preserved.
 */

public class AllElementsSpaceStrippingRule implements SpaceStrippingRule {

    private final static AllElementsSpaceStrippingRule THE_INSTANCE = new AllElementsSpaceStrippingRule();

    public static AllElementsSpaceStrippingRule getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Decide whether an element is in the set of white-space preserving element types
     *
     * @param fingerprint identifies the element being tested
     * @return STRIP_DEFAULT: strip spaces unless xml:space tells you not to.
     */

    public byte isSpacePreserving(NodeName fingerprint) {
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