package net.sf.saxon.event;

import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Rule;
import net.sf.saxon.trans.RuleTarget;
import net.sf.saxon.trans.StripSpaceRules;
import net.sf.saxon.trans.XPathException;

import java.io.Serializable;

/**
  * The RuleBasedStripper class performs whitespace stripping according to the rules of
  * the xsl:strip-space and xsl:preserve-space instructions.
  * It maintains details of which elements need to be stripped.
  * The code is written to act as a SAX-like filter to do the stripping.
  * @author Michael H. Kay
  */


public class RuleBasedStripper extends Stripper {

    public static class StripRuleTarget implements RuleTarget, Serializable {
        public void explain(ExpressionPresenter presenter) {
            // no-op
        }
    }
    public final static StripRuleTarget STRIP = new StripRuleTarget(){};
    public final static StripRuleTarget PRESERVE = new StripRuleTarget(){};

    private boolean preserveAll;              // true if all elements have whitespace preserved

    // stripStack is used to hold information used while stripping nodes. We avoid allocating
    // space on the tree itself to keep the size of nodes down. Each entry on the stack is two
    // booleans, one indicates the current value of xml-space is "preserve", the other indicates
    // that we are in a space-preserving element.

 	// We use a collection of rules to determine whether to strip spaces; a collection
	// of rules is known as a Mode. (We are reusing the code for template rule matching)

	private StripSpaceRules stripperMode;

    /**
    * Default constructor for use in subclasses
    */

    public RuleBasedStripper() {}

    /**
    * create a Stripper and initialise variables
    * @param stripperRules defines which elements have whitespace stripped. If
    * null, all whitespace is preserved.
    */

    public RuleBasedStripper(StripSpaceRules stripperRules) {
        stripperMode = stripperRules;
        preserveAll = (stripperRules==null);
    }

    /**
     * Get a clean copy of this stripper. The new copy shares the same PipelineConfiguration
     * as the original, but the underlying receiver (that is, the destination for post-stripping
     * events) is left uninitialized.
     */

    public RuleBasedStripper getAnother() {
        RuleBasedStripper clone = new RuleBasedStripper(stripperMode);
        clone.setPipelineConfiguration(getPipelineConfiguration());
        clone.preserveAll = preserveAll;
        return clone;
    }

    /**
     * Decide whether an element is in the set of white-space preserving element names
     * @param fingerprint Identifies the name of the element whose whitespace is to
     * be preserved
     * @return ALWAYS_PRESERVE if the element is in the set of white-space preserving
     *  element types, ALWAYS_STRIP if the element is to be stripped regardless of the
     * xml:space setting, and STRIP_DEFAULT otherwise
    */

    public byte isSpacePreserving(int fingerprint) throws XPathException {
        if (preserveAll) {
            return ALWAYS_PRESERVE;
        }

        Rule rule = stripperMode.getRule(fingerprint);

        if (rule==null) {
            return ALWAYS_PRESERVE;
        }

        return (rule.getAction() == PRESERVE ? ALWAYS_PRESERVE : STRIP_DEFAULT);

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
