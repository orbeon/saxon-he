////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.value.StringValue;

/**
 * Subclass of Literal used specifically for string literals, as this is a common case
 */
public class StringLiteral extends Literal {

    /**
     * Create a StringLiteral that wraps a StringValue
     *
     * @param value     the StringValue
     * @param container the container in the expression tree. Should not be null.
     *                  Saxon does not reject a null value for the container, but it may cause
     *                  subsequent failures if a container is not supplied at some stage.
     */

    public StringLiteral(StringValue value, Container container) {
        super(value, container);
    }

    /**
     * Create a StringLiteral that wraps any CharSequence (including, of course, a String)
     *
     * @param value     the CharSequence to be wrapped
     * @param container the container in the expression tree. Should not be null.
     *                  Saxon does not reject a null value for the container, but it may cause
     *                  subsequent failures if a container is not supplied at some stage.
     */

    public StringLiteral(CharSequence value, Container container) {
        super(StringValue.makeStringValue(value), container);
    }

    /**
     * Get the string represented by this StringLiteral
     *
     * @return the underlying string
     */

    public String getStringValue() {
        //noinspection RedundantCast
        return ((StringValue) getValue()).getStringValue();
    }

    /*@NotNull*/
    public Expression copy() {
        StringLiteral stringLiteral = new StringLiteral((StringValue) getValue(), getContainer());
        ExpressionTool.copyLocationInfo(this, stringLiteral);
        return stringLiteral;
    }
}

