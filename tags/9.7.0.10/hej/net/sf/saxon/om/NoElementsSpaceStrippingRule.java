////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;


import net.sf.saxon.event.Stripper;
import net.sf.saxon.trace.ExpressionPresenter;

/**
 * A whitespace stripping rule that strips all elements unless xml:space indicates that whitespace
 * should be preserved.
 */

public class NoElementsSpaceStrippingRule implements SpaceStrippingRule {

    private final static NoElementsSpaceStrippingRule THE_INSTANCE = new NoElementsSpaceStrippingRule();

    public static NoElementsSpaceStrippingRule getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Decide whether an element is in the set of white-space preserving element types
     *
     * @param fingerprint identifies the element being tested
     * @return STRIP_DEFAULT: strip spaces unless xml:space tells you not to.
     */

    public byte isSpacePreserving(NodeName fingerprint) {
        return Stripper.ALWAYS_PRESERVE;
    }

    /**
     * Export this rule as part of an exported stylesheet
     *
     * @param presenter the output handler
     */
    public void export(ExpressionPresenter presenter) {
        presenter.startElement("strip.none");
        presenter.endElement();
    }
}

