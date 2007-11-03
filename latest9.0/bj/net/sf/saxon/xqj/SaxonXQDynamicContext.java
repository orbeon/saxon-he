package net.sf.saxon.xqj;

import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.FloatValue;
import net.sf.saxon.value.Value;
import org.w3c.dom.Node;
import org.xml.sax.XMLReader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.io.Reader;
import java.util.TimeZone;

/**
 * Saxon implementation of the XQJ DynamicContext interface
 */

public abstract class SaxonXQDynamicContext implements XQDynamicContext {

    protected SaxonXQConnection connection;

    protected abstract DynamicQueryContext getDynamicContext();

    protected abstract void checkNotClosed() throws XQException;

    protected abstract SaxonXQDataFactory getDataFactory() throws XQException ;

    public void bindAtomicValue(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromAtomicValue(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindBoolean(QName varname, boolean value, XQItemType type) throws XQException {
        checkNotClosed();
        bindExternalVariable(varname, BooleanValue.get(value));
    }

    public void bindByte(QName varname, byte value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromByte(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, InputStream value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, Reader value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, Source value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, XMLReader value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDocument(QName varname, XMLStreamReader value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem)connection.createItemFromDocument(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindDouble(QName varname, double value, XQItemType type) throws XQException {
        checkNotClosed();
        getDynamicContext().setParameterValue(getClarkName(varname), new DoubleValue(value));
    }

    public void bindFloat(QName varname, float value, XQItemType type) throws XQException {
        checkNotClosed();
        getDynamicContext().setParameterValue(getClarkName(varname), new FloatValue(value));
    }

    public void bindInt(QName varname, int value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromInt(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindItem(QName varname, XQItem value) throws XQException {
        checkNotClosed();
        bindExternalVariable(varname, ((SaxonXQItem) value).getItem());
    }

    public void bindLong(QName varname, long value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromLong(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindNode(QName varname, Node value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromNode(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindObject(QName varname, Object value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromObject(value, type);
        bindExternalVariable(varname, item.getItem());

    }

    public void bindSequence(QName varname, XQSequence value) throws XQException {
        checkNotClosed();
        try {
            if (value instanceof SaxonXQForwardSequence) {
                getDynamicContext().setParameter(getClarkName(varname),
                        ((SaxonXQForwardSequence) value).getCleanIterator());
            } else if (value instanceof SaxonXQSequence) {
                bindExternalVariable(varname, ((SaxonXQSequence) value).getValue());
            } else {
                throw new XQException("XQSequence value is not a Saxon sequence");
            }
        } catch (XPathException de) {
            XQException err = new XQException(de.getMessage());
            err.initCause(de);
            throw err;
        }
    }

    public void bindShort(QName varname, short value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromShort(value, type);
        bindExternalVariable(varname, item.getItem());
    }

    public void bindString(QName varname, String value, XQItemType type) throws XQException {
        checkNotClosed();
        SaxonXQItem item = (SaxonXQItem) getDataFactory().createItemFromString(value, type);
        bindExternalVariable(varname, item.getItem());
    }    

    public TimeZone getImplicitTimeZone() throws XQException {
        checkNotClosed();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setImplicitTimeZone(TimeZone implicitTimeZone) throws XQException {
        checkNotClosed();
        //To change body of implemented methods use File | Settings | File Templates.
    }


    private void bindExternalVariable(QName varName, ValueRepresentation value) throws XQException {
        try {
            if (varName.equals(XQConstants.CONTEXT_ITEM)) {
                getDynamicContext().setContextItem(Value.asItem(value));
            } else {
                getDynamicContext().setParameterValue(getClarkName(varName), value);
            }
        } catch (XPathException e) {
            XQException err = new XQException(e.getMessage());
            err.initCause(e);
            throw err;
        }
    }


    private String getClarkName(QName qname) {
        String uri = qname.getNamespaceURI();
        return "{" + (uri == null ? "" : uri) + "}" + qname.getLocalPart();
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
//