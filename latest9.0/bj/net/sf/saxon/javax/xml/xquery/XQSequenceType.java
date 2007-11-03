package net.sf.saxon.javax.xml.xquery;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQSequenceType {

    static int OCC_EMPTY = 5;

    static int OCC_EXACTLY_ONE = 2;

    static int OCC_ONE_OR_MORE = 4;

    static int OCC_ZERO_OR_MORE = 3;

    static int OCC_ZERO_OR_ONE = 1;

    int getItemOccurrence();

    XQItemType getItemType();

    java.lang.String toString();
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
