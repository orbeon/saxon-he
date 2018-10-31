////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;

import java.util.ArrayList;
import java.util.List;

/**
 * A value that is a sequence containing one or more items. The main use is in declarations of reflexive extension
 * functions, where declaring an argument of type &lt;OneOrMore&lt;IntegerValue&gt;&gt; triggers automatic type
 * checking in the same way as for a native XSLT/XQuery function declaring the type as xs:integer+.
 */

public class OneOrMore<T extends Item<?>> extends SequenceExtent<T> {

    /**
     * Create a sequence containing zero or one items
     *
     * @param content The content of the sequence
     */

    public OneOrMore(T[] content) {
        super(content);
        if (content.length == 0) {
            throw new IllegalArgumentException();
        }
    }

    public OneOrMore(List<T> content) {
        super(content);
        if (content.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    public static <T extends Item<?>> OneOrMore<T> makeOneOrMore(Sequence<T> sequence) throws XPathException {
        List<T> content = new ArrayList<>();
        sequence.iterate().forEachOrFail(content::add);
        if (content.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return new OneOrMore<>(content);
    }

}