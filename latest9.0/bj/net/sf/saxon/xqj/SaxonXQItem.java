package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SerializerFactory;
import net.sf.saxon.event.TreeReceiver;
import net.sf.saxon.javax.xml.xquery.XQConnection;
import net.sf.saxon.javax.xml.xquery.XQException;
import net.sf.saxon.javax.xml.xquery.XQItemType;
import net.sf.saxon.javax.xml.xquery.XQResultItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.pull.PullFromIterator;
import net.sf.saxon.pull.PullToStax;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * This Saxon class is used to implement both the XQItem and XQResultItem interfaces in XQJ.
 * Where the item is not a real XQResultItem, getConnection() will return null.
 */
public class SaxonXQItem implements XQResultItem {

    private Item item;
    private Configuration config;
    SaxonXQDataFactory dataFactory;

    public SaxonXQItem(Item item, SaxonXQDataFactory factory) {
        this.item = item;
        dataFactory = factory;
        config = factory.getConfiguration();
    }

    Configuration getConfiguration() {
        return config;
    }

    Item getItem() {
        return item;
    }

    public XQConnection getConnection() throws XQException {
        if (dataFactory instanceof XQConnection) {
            return (XQConnection)dataFactory;
        } else {
            return null;
        }
    }

    public void close() {
        item = null;
    }

    public boolean isClosed() {
        return item == null;
    }

    public String getAtomicValue() throws XQException {
        if (item instanceof AtomicValue) {
            return item.getStringValue();
        }
        throw new XQException("Failed to getAtomicValue: item is a node, or is closed");
    }

    public boolean getBoolean() throws XQException {
        if (item instanceof BooleanValue) {
            return ((BooleanValue)item).getBooleanValue();
        }
        throw new XQException("Failed in getBoolean: item is not a boolean, or is closed");
    }

    public byte getByte() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, -128, 127);
        }
        throw new XQException("Failed in getByte: item is not an atomic value, or is closed");
    }

    private static long longValue(AtomicValue value, long min, long max) throws XQException {
        if (value instanceof NumericValue) {
            if (value instanceof DoubleValue || value instanceof FloatValue) {
                throw new XQException("Value is a double or float");
            }
            if (!((NumericValue)value).isWholeNumber()) {
                throw new XQException("Value is not a whole number");
            }
            try {
                long val = ((NumericValue)value).longValue();
                if (val >= min && val <= max) {
                    return val;
                } else {
                    throw new XQException("Value is out of range for requested type");
                }
            } catch (XPathException err) {
                XQException xqe = new XQException(err.getMessage());
                xqe.initCause(err);
                throw xqe;
            }
        }
        throw new XQException("Value is not numeric");
    }

    public double getDouble() throws XQException {
        if (item instanceof DoubleValue) {
                return ((DoubleValue)item).getDoubleValue();
        }
        throw new XQException("Failed in getDouble: item is not a double, or is closed");
    }

    public float getFloat() throws XQException {
        if (item instanceof FloatValue) {
                return ((FloatValue)item).getFloatValue();
        }
        throw new XQException("Failed in getFloat: item is not a float, or is closed");
    }

    public int getInt() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        throw new XQException("Failed in getInt: item is not an atomic value, or is closed");
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return new PullToStax(new PullFromIterator(SingletonIterator.makeIterator(item)));
    }

    public String getItemAsString(Properties props) throws XQException {
        if (props == null) {
            props = new Properties();
            props.setProperty(OutputKeys.METHOD, "xml");
            props.setProperty(OutputKeys.INDENT, "yes");
            props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        }
        StringWriter writer = new StringWriter();
        writeItem(writer, props);
        return writer.toString();
    }

    public XQItemType getItemType() throws XQException {
        if (item instanceof AtomicValue) {
            return new SaxonXQItemType(
                    ((AtomicValue)item).getItemType(getConfiguration().getTypeHierarchy()),
                    getConfiguration());
        } else {
            return new SaxonXQItemType((NodeInfo)item);
        }
    }

    public long getLong() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Long.MIN_VALUE, Long.MAX_VALUE);
        }
        throw new XQException("Failed in getLong: item is not an atomic value, or is closed");
    }

    public Node getNode() throws XQException {
        if (!(item instanceof NodeInfo)) {
            throw new XQException("Failed in getNode: item is an atomic value, or is closed");
        }
        return NodeOverNodeInfo.wrap((NodeInfo)item);
    }

    public URI getNodeUri() throws XQException {
        if (item instanceof NodeInfo) {
            try {
                return new URI(((NodeInfo)item).getSystemId());
            } catch (URISyntaxException e) {
                throw new XQException("System ID of node is not a valid URI");
            }
        }
        throw new XQException("Item is not a node, or is closed");
    }

    public Object getObject() throws XQException {
        return dataFactory.getObjectConverter().toObject(this);
    }

    public short getShort() throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue prim = ((AtomicValue)item);
            return (byte)longValue(prim, Short.MIN_VALUE, Short.MAX_VALUE);
        }
        throw new XQException("Failed in getShort: item is not an atomic value, or is closed");
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return ((SaxonXQItemType)type).getSaxonItemType().matchesItem(item, false, config);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        writeItemToResult(new StreamResult(os), props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        writeItemToResult(new StreamResult(ow), props);
    }

    public void writeItemToResult(Result result) throws XQException {
        writeItemToResult(result, new Properties());
    }

    private void writeItemToResult(Result result, Properties props) throws XQException {
        try {
            SerializerFactory sf = config.getSerializerFactory();
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            Receiver out = sf.getReceiver(result, pipe, props);
            TreeReceiver tr = new TreeReceiver(out);
            tr.open();
            tr.append(item, 0, 0);
            tr.close();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        writeItemToResult(new SAXResult(saxHandler));
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