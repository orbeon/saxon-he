package net.sf.saxon.javax.xml.xquery;

import org.w3c.dom.Node;
import org.xml.sax.XMLReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public interface XQDataFactory {

    XQItemType createAtomicType(int baseType) throws XQException;

    XQItemType createAtomicType(int baseType, QName typename, URI schemaURI) throws XQException;

    XQItemType createAttributeType(QName nodename, int basetype) throws XQException;

    XQItemType createAttributeType(QName nodename, int basetype, QName typename, URI schemaURI) throws XQException;

    XQItemType createCommentType() throws XQException;

    XQItemType createDocumentElementType(XQItemType elementType) throws XQException;

    XQItemType createDocumentSchemaElementType(XQItemType type) throws XQException;

    XQItemType createDocumentType() throws XQException;

    XQItemType createElementType(QName nodename, int basetype) throws XQException;

    XQItemType createElementType(QName nodename, int basetype, QName typename, URI schemaURI, boolean allowNill) throws XQException;

    XQItem createItem(XQItem item) throws XQException;

    XQItem createItemFromAtomicValue(String value, XQItemType type) throws XQException;

    XQItem createItemFromBoolean(boolean value, XQItemType type) throws XQException;

    XQItem createItemFromByte(byte value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(InputStream value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(Reader value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(Source value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(String value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(XMLReader value, XQItemType type) throws XQException;

    XQItem createItemFromDocument(XMLStreamReader value, XQItemType type) throws XQException;     

    XQItem createItemFromDouble(double value, XQItemType type) throws XQException;

    XQItem createItemFromFloat(float value, XQItemType type) throws XQException;

    XQItem createItemFromInt(int value, XQItemType type) throws XQException;

    XQItem createItemFromLong(long value, XQItemType type) throws XQException;

    XQItem createItemFromNode(Node value, XQItemType type) throws XQException;

    XQItem createItemFromObject(Object value, XQItemType type)  throws XQException;

    XQItem createItemFromShort(short value, XQItemType type) throws XQException;

    XQItem createItemFromString(String value, XQItemType type) throws XQException;

    XQItemType createItemType() throws XQException;

    XQItemType createNodeType() throws XQException;

    XQItemType createProcessingInstructionType(String piTarget) throws XQException;

    XQItemType createSchemaAttributeType(QName nodename, int basetype, URI schemaURI) throws XQException;

    XQItemType createSchemaElementType(QName nodename, int basetype, URI schemaURI) throws XQException;

    XQSequence createSequence(java.util.Iterator i) throws XQException;

    XQSequence createSequence(XQSequence s) throws XQException;

    XQSequenceType createSequenceType(XQItemType item, int occurrence) throws XQException;

    XQItemType createTextType() throws XQException;




}


//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
