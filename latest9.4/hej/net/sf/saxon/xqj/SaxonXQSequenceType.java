package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.value.SequenceType;

import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQSequenceType;

/**
 * Saxon implementation of the XQJ SequenceType interface
 */


public class SaxonXQSequenceType implements XQSequenceType {

    SequenceType sequenceType;
    Configuration config;

    SaxonXQSequenceType(SequenceType sequenceType, Configuration config) {
        this.sequenceType = sequenceType;
        this.config = config;
    }

    public int getItemOccurrence() {
        int cardinality = sequenceType.getCardinality();
        switch (cardinality) {
            case StaticProperty.EXACTLY_ONE:
                return XQSequenceType.OCC_EXACTLY_ONE;
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return XQSequenceType.OCC_ZERO_OR_ONE;
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return XQSequenceType.OCC_ONE_OR_MORE;
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return XQSequenceType.OCC_ZERO_OR_MORE;
            default:
                return XQSequenceType.OCC_ZERO_OR_MORE;
        }
    }

    /*@NotNull*/ public XQItemType getItemType() {
        return new SaxonXQItemType(sequenceType.getPrimaryType(), config);
    }

    /*@Nullable*/ public String getString() {
        String s = sequenceType.getPrimaryType().toString(config.getNamePool());
        switch (sequenceType.getCardinality()) {
            case StaticProperty.EXACTLY_ONE:
                return s;
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return s + "?";
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return s + "+";
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return s + "*";
            default:
                return s;
        }
    }

    /*@Nullable*/ public String toString() {
        return getString();
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