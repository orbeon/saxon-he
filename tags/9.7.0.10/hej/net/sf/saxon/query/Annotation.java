////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.query;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an annotation that appears in a function or variable declarations
 */
public class Annotation {

    public static final StructuredQName UPDATING = new StructuredQName("", NamespaceConstant.XQUERY, "updating");
    public static final StructuredQName SIMPLE = new StructuredQName("", NamespaceConstant.XQUERY, "simple");
    public static final StructuredQName PRIVATE = new StructuredQName("", NamespaceConstant.XQUERY, "private");
    public static final StructuredQName PUBLIC = new StructuredQName("", NamespaceConstant.XQUERY, "public");

    public static final int FUNCTION_DECLARATION = 1;
    public static final int VARIABLE_DECLARATION = 2;
    public static final int INLINE_FUNCTION = 4;
    public static final int ANNOTATION_ASSERTION = 8;

    // The name of the annotation
    private StructuredQName qName = null;

    // The list of paramters (all strings or numbers) associated with the annotation
    private List<AtomicValue> annList = null;

    /**
     * Create an annotation
     *
     * @param name the annotation name (a QName)
     */

    public Annotation(StructuredQName name) {
        this.qName = name;
    }

    /**
     * Get the name of the annotation (a QName)
     *
     * @return the annotation name
     */

    public StructuredQName getAnnotationQName() {
        return qName;
    }

    /**
     * Add a value to the list of annotation parameters
     *
     * @param value the value to be added. This will always be a string or number,
     *              but Saxon enforces this only at the level of the query parser
     */

    public void addAnnotationParameter(AtomicValue value) {
        if (annList == null) {
            annList = new ArrayList<AtomicValue>();
        }
        annList.add(value);
    }

    /**
     * Get the list of annotation parameters
     *
     * @return the list of parameters
     */

    public List<AtomicValue> getAnnotationParameters() {
        return annList;
    }

    public static boolean existsAnnotation(List<Annotation> annotationList, StructuredQName name) {
        for (Annotation a : annotationList) {
            if (a.getAnnotationQName().equals(name)) {
                return true;
            }
        }
        return false;
    }


    public static void checkAnnotationList(List<Annotation> list, int where) throws XPathException {
        for (int i=0; i<list.size(); i++) {
            Annotation ann = list.get(i);
            for (DisallowedCombination dc : blackList) {
                if (dc.one.equals(ann.getAnnotationQName()) && (dc.where & where) != 0) {
                    if (dc.two == null) {
                        throw new XPathException("Annotation %" + ann.getAnnotationQName().getLocalPart() + " is not allowed here",
                                                 dc.errorCode);
                    } else {
                        for (int j=0; j<i; j++) {
                            Annotation other = list.get(j);
                            if (dc.two.equals(other.getAnnotationQName())) {
                                if (dc.two.equals(ann.getAnnotationQName())) {
                                    throw new XPathException("Annotation %" + ann.getAnnotationQName().getLocalPart() +
                                                                     " cannot appear more than once", dc.errorCode);
                                } else {
                                    throw new XPathException("Annotations %" + ann.getAnnotationQName().getLocalPart() +
                                                                     " and " + other.getAnnotationQName().getLocalPart() + " cannot appear together",
                                                             dc.errorCode);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static class DisallowedCombination {
        public DisallowedCombination(StructuredQName one, StructuredQName two, String errorCode, int where) {
            this.one = one;
            this.two = two;
            this.errorCode = errorCode;
            this.where = where;
        }
        public StructuredQName one;
        public StructuredQName two;
        public String errorCode;
        public int where;
    }

    private static DisallowedCombination[] blackList = {
            new DisallowedCombination(SIMPLE, null, "XUST0032", VARIABLE_DECLARATION),
            new DisallowedCombination(UPDATING, null, "XUST0032", VARIABLE_DECLARATION),
            new DisallowedCombination(PUBLIC, null, "XQST0125", INLINE_FUNCTION),
            new DisallowedCombination(PRIVATE, null, "XQST0125", INLINE_FUNCTION),
            new DisallowedCombination(PRIVATE, PRIVATE, "XQST0106", FUNCTION_DECLARATION),
            new DisallowedCombination(PRIVATE, PUBLIC, "XQST0106", FUNCTION_DECLARATION),
            new DisallowedCombination(PUBLIC, PUBLIC, "XQST0106", FUNCTION_DECLARATION),
            new DisallowedCombination(PUBLIC, PRIVATE, "XQST0106", FUNCTION_DECLARATION),
            new DisallowedCombination(PRIVATE, PRIVATE, "XQST0116", VARIABLE_DECLARATION),
            new DisallowedCombination(PRIVATE, PUBLIC, "XQST0116", VARIABLE_DECLARATION),
            new DisallowedCombination(PUBLIC, PUBLIC, "XQST0116", VARIABLE_DECLARATION),
            new DisallowedCombination(PUBLIC, PRIVATE, "XQST0116", VARIABLE_DECLARATION),
            new DisallowedCombination(UPDATING, UPDATING, "XUST0033", FUNCTION_DECLARATION | INLINE_FUNCTION),
            new DisallowedCombination(UPDATING, SIMPLE, "XUST0033", FUNCTION_DECLARATION | INLINE_FUNCTION),
            new DisallowedCombination(SIMPLE, SIMPLE, "XUST0033", FUNCTION_DECLARATION | INLINE_FUNCTION),
            new DisallowedCombination(SIMPLE, UPDATING, "XUST0033", FUNCTION_DECLARATION | INLINE_FUNCTION),
    };


}

