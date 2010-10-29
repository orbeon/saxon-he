package net.sf.saxon.s9api;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.RuleBasedStripper;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.trans.StripSpaceRules;
import net.sf.saxon.value.Whitespace;

/**
 * WhitespaceStrippingPolicy is class defining the possible policies for handling
 * whitespace text nodes in a source document.
 */

public class WhitespaceStrippingPolicy {

    private int policy;
    private StripSpaceRules stripperRules;

    /**
     * The value NONE indicates that all whitespace text nodes are retained
     */
    public static final WhitespaceStrippingPolicy NONE = new WhitespaceStrippingPolicy(Whitespace.NONE);

    /**
     * The value IGNORABLE indicates that whitespace text nodes in element-only content are
     * discarded. Content is element-only if it is defined by a schema or DTD definition that
     * does not allow mixed or PCDATA content.
     */
    public static final WhitespaceStrippingPolicy IGNORABLE = new WhitespaceStrippingPolicy(Whitespace.IGNORABLE);

    /**
     * The value ALL indicates that all whitespace-only text nodes are discarded.
     */
    public static final WhitespaceStrippingPolicy ALL = new WhitespaceStrippingPolicy(Whitespace.ALL);

    /**
     * UNSPECIFIED means that no other value has been specifically requested.
     */
    public static final WhitespaceStrippingPolicy UNSPECIFIED = new WhitespaceStrippingPolicy(Whitespace.UNSPECIFIED);

    private WhitespaceStrippingPolicy(int policy) {
        this.policy = policy;
    }

    /**
     * Create a WhitespaceStrippingPolicy based on the xsl:strip-space and xsl:preserve-space declarations
     * in a given XSLT stylesheet
     * @param executable the stylesheet containing the xsl:strip-space and xsl:preserve-space declarations
     */
    
    protected WhitespaceStrippingPolicy (Executable executable) {
        policy = Whitespace.XSLT;
        stripperRules = executable.getStripperRules();
    }

    protected int ordinal() {
        return policy;
    }

    protected ProxyReceiver makeStripper() {
        return new RuleBasedStripper(stripperRules);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

