package net.sf.saxon.expr;

import net.sf.saxon.functions.regex.RegexIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;

    /**
     * Mapping function that maps the sequence of matching/non-matching strings to the
     * sequence delivered by applying the matching-substring and non-matching-substring
     * expressions respectively to each such string
     */

    public class AnalyzeMappingFunction implements ContextMappingFunction {

        private RegexIterator base;
        private XPathContext c2;
        private Expression nonMatchExpr;
        private Expression matchingExpr;

        public AnalyzeMappingFunction(RegexIterator base, XPathContext c2, Expression nonMatchExpr, Expression matchingExpr) {
            this.base = base;
            this.c2 = c2;
            this.nonMatchExpr = nonMatchExpr;
            this.matchingExpr = matchingExpr;
        }

        /**
         * Map one item to a sequence.
         *
         * @param context The processing context. Some mapping functions use this because they require
         *                context information. Some mapping functions modify the context by maintaining the context item
         *                and position. In other cases, the context may be null.
         * @return either (a) a SequenceIterator over the sequence of items that the supplied input
         *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
         *         sequence.
         */

        public SequenceIterator map(XPathContext context) throws XPathException {
            if (base.isMatching()) {
                if (matchingExpr != null) {
                    return matchingExpr.iterate(c2);
                }
            } else {
                if (nonMatchExpr != null) {
                    return nonMatchExpr.iterate(c2);
                }
            }
            return EmptyIterator.getInstance();
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