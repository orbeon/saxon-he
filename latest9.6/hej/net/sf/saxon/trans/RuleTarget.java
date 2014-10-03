////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.expr.Component;
import net.sf.saxon.trace.ExpressionPresenter;

/**
 * The target of a rule, typically a Template.
 */
public interface RuleTarget {

    public Component getDeclaringComponent();

    /**
     * Output diagnostic explanation to an ExpressionPresenter
     * @param presenter the destination for the explanation
     */

    public void explain(ExpressionPresenter presenter);

    /**
     * Register a rule for which this is the target
     * @param rule a rule in which this is the target
     */

    public void regiaterRule(Rule rule);
}

