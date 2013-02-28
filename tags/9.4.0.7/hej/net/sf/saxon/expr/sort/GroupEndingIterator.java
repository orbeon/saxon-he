package net.sf.saxon.expr.sort;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;

import java.util.ArrayList;

/**
 * A GroupEndingIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-ending-with="x". The groups are returned in
 * order of first appearance.
 */

public class GroupEndingIterator extends GroupMatchingIterator implements GroupIterator, LookaheadIterator {

    public GroupEndingIterator(SequenceIterator population, Pattern endPattern,
                               XPathContext context)
            throws XPathException {
        this.population = population;
        this.pattern = endPattern;
        baseContext = context;
        runningContext = context.newMinorContext();
        runningContext.setCurrentIterator(population);
        // the first item in the population always starts a new group
        next = population.next();
    }

    protected void advance() throws XPathException {
        currentMembers = new ArrayList(20);
        currentMembers.add(current);

        next = current;
        while (next != null) {
            if (pattern.matches(next, runningContext)) {
                next = population.next();
                if (next != null) {
                    break;
                }
            } else {
                next = population.next();
                if (next != null) {
                    currentMembers.add(next);
                }
            }
        }
    }

    /*@NotNull*/ public SequenceIterator getAnother() throws XPathException {
        return new GroupEndingIterator(population.getAnother(), pattern, baseContext);
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