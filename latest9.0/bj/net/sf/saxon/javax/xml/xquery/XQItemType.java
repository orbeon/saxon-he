package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;
import java.net.URI;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQItemType extends XQSequenceType {

    static int XQBASETYPE_ANYATOMICTYPE = 4;
    static int XQBASETYPE_ANYSIMPLETYPE = 3;
    static int XQBASETYPE_ANYTYPE = 2;
    static int XQBASETYPE_ANYURI = 8;
    static int XQBASETYPE_BASE64BINARY = 9;
    static int XQBASETYPE_BOOLEAN = 10;
    static int XQBASETYPE_BYTE = 31;
    static int XQBASETYPE_DATE = 11;
    static int XQBASETYPE_DATETIME = 16;
    static int XQBASETYPE_DAYTIMEDURATION = 7;
    static int XQBASETYPE_DECIMAL = 17;
    static int XQBASETYPE_DOUBLE = 18;
    static int XQBASETYPE_DURATION = 19;
    static int XQBASETYPE_ENTITIES = 50;
    static int XQBASETYPE_ENTITY = 48;
    static int XQBASETYPE_FLOAT = 20;
    static int XQBASETYPE_GDAY = 21;
    static int XQBASETYPE_GMONTH = 22;
    static int XQBASETYPE_GMONTHDAY = 23;
    static int XQBASETYPE_GYEAR = 24;
    static int XQBASETYPE_GYEARMONTH = 25;
    static int XQBASETYPE_HEXBINARY = 26;
    static int XQBASETYPE_ID = 46;
    static int XQBASETYPE_IDREF = 47;
    static int XQBASETYPE_IDREFS = 49;
    static int XQBASETYPE_INT = 12;
    static int XQBASETYPE_INTEGER = 13;
    static int XQBASETYPE_LANGUAGE = 42;
    static int XQBASETYPE_LONG = 15;
    static int XQBASETYPE_NAME = 43;
    static int XQBASETYPE_NCNAME = 44;
    static int XQBASETYPE_NEGATIVE_INTEGER = 34;
    static int XQBASETYPE_NMTOKEN = 45;
    static int XQBASETYPE_NMTOKENS = 51;
    static int XQBASETYPE_NONNEGATIVE_INTEGER = 33;
    static int XQBASETYPE_NONPOSITIVE_INTEGER = 32;
    static int XQBASETYPE_NORMALIZED_STRING = 40;
    static int XQBASETYPE_NOTATION = 27;
    static int XQBASETYPE_POSITIVE_INTEGER = 35;
    static int XQBASETYPE_QNAME = 28;
    static int XQBASETYPE_SHORT = 14;
    static int XQBASETYPE_STRING = 29;
    static int XQBASETYPE_TIME = 30;
    static int XQBASETYPE_TOKEN = 41;
    static int XQBASETYPE_UNSIGNED_BYTE = 39;
    static int XQBASETYPE_UNSIGNED_INT = 37;
    static int XQBASETYPE_UNSIGNED_LONG = 36;
    static int XQBASETYPE_UNSIGNED_SHORT = 38;
    static int XQBASETYPE_UNTYPED = 1;
    static int XQBASETYPE_UNTYPEDATOMIC = 5;
    static int XQBASETYPE_YEARMONTHDURATION = 7;

    static int XQITEMKIND_ATOMIC = 1;
    static int XQITEMKIND_ATTRIBUTE = 2;
    static int XQITEMKIND_COMMENT = 3;
    static int XQITEMKIND_DOCUMENT = 4;
    static int XQITEMKIND_DOCUMENT_ELEMENT = 5;
    static int XQITEMKIND_DOCUMENT_SCHEMA_ELEMENT = 6;
    static int XQITEMKIND_ELEMENT = 7;
    static int XQITEMKIND_ITEM = 8;
    static int XQITEMKIND_NODE = 9;
    static int XQITEMKIND_PI = 10;
    static int XQITEMKIND_SCHEMA_ATTRIBUTE = 13;
    static int XQITEMKIND_SCHEMA_ELEMENT = 12;
    static int XQITEMKIND_TEXT = 11;

    int getBaseType() throws XQException;

    int getItemKind();

    int getItemOccurrence();

    QName getNodeName() throws XQException;

    String getPIName() throws XQException;

    URI getSchemaURI();

    QName getTypeName() throws XQException;

    boolean isAnonymousType();

    boolean isElementNillable();

}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
