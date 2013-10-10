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
        String s = sequenceType.getPrimaryType().toString();
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

// Copyright (c) 2013 Saxonica Limited. All rights reserved.