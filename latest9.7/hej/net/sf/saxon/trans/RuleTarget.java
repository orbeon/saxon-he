////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.trace.ExpressionPresenter;

/**
 * The target of a rule, typically a TemplateRule.
 */
public interface RuleTarget {

    /**
     * Output diagnostic explanation to an ExpressionPresenter
     * @param presenter the destination for the explanation
     */

    void export(ExpressionPresenter presenter) throws XPathException;

    /**
     * Register a rule for which this is the target
     * @param rule a rule in which this is the target
     */

    void registerRule(Rule rule);
}

