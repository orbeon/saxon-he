package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.TailIterator;
import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pull.PullFromIterator;
import net.sf.saxon.pull.PullToStax;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;

/**
 * Saxon implementation of the XQSequence interface in XQJ, which represents an XDM sequence together
 * with a current position. This class is used for a sequence that can be read forwards, backwards,
 * or by absolute position.
 */
public class SaxonXQSequence implements XQResultSequence {

    private Value value;
    private Configuration config;
    private int position;
    private boolean closed;
    private SaxonXQConnection connection;

    SaxonXQSequence(Value value, Configuration config) {
        this.value = value;
        this.config = config;
    }

    SaxonXQSequence(Value value, Configuration config, SaxonXQConnection connection) {
        this.value = value;
        this.config = config;
        this.connection = connection;
    }

    Value getValue() {
        return value;
    }

    Configuration getConfiguration() {
        return config;
    }

    public boolean absolute(int itempos) throws XQException {
        try {
            if (itempos > 0) {
                if (itempos <= value.getLength()) {
                    position = itempos;
                    return true;
                } else {
                    position = -1;
                    return false;
                }
            } else if (itempos < 0) {
                if (-itempos <= value.getLength()) {
                    position = value.getLength() + itempos + 1;
                    return true;
                } else {
                    position = 0;
                    return false;
                }
            } else {
                position = 0;
                return false;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void afterLast() throws XQException {
        position = -1;
    }

    public void beforeFirst() throws XQException {
        position = 0;
    }

    public void close() throws XQException {
        closed = true;
        value = null;
    }

    public int count() throws XQException {
        try {
            return value.getLength();
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean first() throws XQException {
        try {
            if (value.getLength() == 0) {
                position = 0;
                return false;
            } else {
                position = 1;
                return true;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XQItem getItem() throws XQException {
        try {
            return new SaxonXQItem(value.itemAt(position - 1), connection);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public int getPosition() throws XQException {
        try {
            if (position >= 0) {
                return position;
            } else {
                return value.getLength();
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XMLStreamReader getSequenceAsStream() throws XQException {
        return new PullToStax(new PullFromIterator(iterateRemainder()));
    }

    public String getSequenceAsString(Properties props) throws XQException {
        StringWriter sw = new StringWriter();
        writeSequence(sw, props);
        return sw.toString();
    }

    public boolean isAfterLast() throws XQException {
        return position < 0;
    }

    public boolean isBeforeFirst() throws XQException {
        return position == 0;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFirst() throws XQException {
        return position == 1;
    }

    public boolean isLast() throws XQException {
        try {
            return position == value.getLength();
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean isOnItem() throws XQException {
        return position >= 1;
    }

    public boolean isScrollable() throws XQException {
        return true;
    }

    public boolean last() throws XQException {
        try {
            int n = value.getLength();
            if (n == 0) {
                position = -1;
                return false;
            } else {
                position = 0;
                return true;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean next() throws XQException {
        try {
            if (position == value.getLength()) {
                position = -1;
                return false;
            } else {
                position++;
                return true;
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public boolean previous() throws XQException {
        position--;
        return (position != 0);
    }

    public boolean relative(int itempos) throws XQException {
        try {
            position += itempos;
            if (position <= 0) {
                position = 0;
                return false;
            }
            if (position > value.getLength()) {
                position = -1;
                return false;
            }
            return true;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequence(OutputStream os, Properties props) throws XQException {
        try {
            QueryResult.serializeSequence(iterateRemainder(), config, os, props);
            position = -1;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequence(Writer ow, Properties props) throws XQException {
        try {
            PrintWriter pw;
            if (ow instanceof PrintWriter) {
                pw = (PrintWriter)ow;
            } else {
                pw = new PrintWriter(ow);
            }
            QueryResult.serializeSequence(iterateRemainder(), config, pw, props);
            position = -1;
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToResult(Result result) throws XQException {
        try {
            QueryResult.serializeSequence(iterateRemainder(), config, result, new Properties());
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void writeSequenceToSAX(ContentHandler saxHandler) throws XQException {
        writeSequenceToResult(new SAXResult(saxHandler));
    }

    public String getAtomicValue() throws XQException {
        return getCurrentItem().getAtomicValue();
    }

    public boolean getBoolean() throws XQException {
        return getCurrentItem().getBoolean();
    }

    public byte getByte() throws XQException {
        return getCurrentItem().getByte();
    }

    public double getDouble() throws XQException {
        return getCurrentItem().getDouble();
    }

    public float getFloat() throws XQException {
        return getCurrentItem().getFloat();
    }

    public int getInt() throws XQException {
        return getCurrentItem().getInt();
    }

    public XMLStreamReader getItemAsStream() throws XQException {
        return getCurrentItem().getItemAsStream();
    }

    public String getItemAsString(Properties props) throws XQException {
        return getCurrentItem().getItemAsString(props);
    }

    public XQItemType getItemType() throws XQException {
        return getCurrentItem().getItemType();
    }

    public long getLong() throws XQException {
        return getCurrentItem().getLong();
    }

    public Node getNode() throws XQException {
        return getCurrentItem().getNode();
    }

    public URI getNodeUri() throws XQException {
        return getCurrentItem().getNodeUri();
    }

    public Object getObject() throws XQException {
        return getCurrentItem().getObject();
    }

    public short getShort() throws XQException {
        return getCurrentItem().getShort();
    }

    public boolean instanceOf(XQItemType type) throws XQException {
        return getCurrentItem().instanceOf(type);
    }

    public void writeItem(OutputStream os, Properties props) throws XQException {
        getCurrentItem().writeItem(os, props);
    }

    public void writeItem(Writer ow, Properties props) throws XQException {
        getCurrentItem().writeItem(ow, props);
    }

    public void writeItemToResult(Result result) throws XQException {
        getCurrentItem().writeItemToResult(result);
    }

    public void writeItemToSAX(ContentHandler saxHandler) throws XQException {
        getCurrentItem().writeItemToSAX(saxHandler);
    }

    private SaxonXQItem getCurrentItem() throws XQException {
        if (closed) {
            throw new XQException("Sequence is closed");
        }
        if (position == 0) {
            throw new XQException("Sequence is positioned before first item");
        }
        if (position < 0) {
            throw new XQException("Sequence is positioned after last item");
        }
        try {
            return new SaxonXQItem(value.itemAt(position-1), connection);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public XQConnection getConnection() {
        return connection;
    }

    private SequenceIterator iterateRemainder() throws XQException {
        try {
            if (position == 0) {
                return value.iterate();
            } else if (position < 0) {
                return EmptyIterator.getInstance();
            } else {
                return TailIterator.make(value.iterate(), position);
            }
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    private XQException newXQException(Exception err) {
        XQException xqe = new XQException(err.getMessage());
        xqe.initCause(err);
        return xqe;
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