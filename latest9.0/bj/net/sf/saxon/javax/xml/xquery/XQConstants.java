package net.sf.saxon.javax.xml.xquery;

import net.sf.saxon.om.NamespaceConstant;

import javax.xml.namespace.QName;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQConstants {
    public static final int BINDING_MODE_DEFERRED = 1;
    public static final int BINDING_MODE_IMMEDIATE = 0;
    public static final int BOUNDARY_SPACE_PRESERVE = 1;
    public static final int BOUNDARY_SPACE_STRIP = 2;
    public static final int CONSTRUCTION_MODE_PRESERVE = 1;
    public static final int CONSTRUCTION_MODE_STRIP = 2;
    public static final QName CONTEXT_ITEM = new QName(NamespaceConstant.SAXON, "contextItemName", "saxon");
    public static final int COPY_NAMESPACES_MODE_INHERIT = 1;
    public static final int COPY_NAMESPACES_MODE_NO_INHERIT = 2;
    public static final int COPY_NAMESPACES_MODE_NO_PRESERVE = 2;
    public static final int COPY_NAMESPACES_MODE_PRESERVE = 1;
    public static final int DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_GREATEST = 1;
    public static final int DEFAULT_ORDER_FOR_EMPTY_SEQUENCES_LEAST = 2;
    public static final int HOLDTYPE_CLOSE_CURSORS_AT_COMMIT = 2;
    public static final int HOLDTYPE_HOLD_CURSORS_OVER_COMMIT = 1;
    public static final int LANGTYPE_XQUERY = 1;
    public static final int LANGTYPE_XQUERYX = 2;
    public static final int ORDERING_MODE_ORDERED = 1;
    public static final int ORDERING_MODE_UNORDERED = 2;
    public static final int RESULTTYPE_READ_ONLY = 1;
    public static final int SCROLLTYPE_FORWARD_ONLY = 1;
    public static final int SCROLLTYPE_SCROLLABLE = 2;
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
