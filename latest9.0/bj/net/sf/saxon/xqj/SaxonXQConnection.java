package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Saxon implementation of the XQL interface XQConnection. This interface represents a
 * "connection" between an XQuery application and an XQuery server. In Saxon the client
 * and server run in the same process so the concept of a connection is rather notional,
 * and some of the properties have little meaning. However, the connection is the factory
 * object used to compile queries.
 * <p>
 * For Javadoc descriptions of the public methors, see the XQJ documentation.
 */
public class SaxonXQConnection extends SaxonXQDataFactory implements XQConnection {

    private Configuration config;
    private SaxonXQStaticContext staticContext;
    private boolean closed;

    /**
     * Create an SaxonXQConnection from a SaxonXQDataSource
     * @param dataSource the data source.
     */
    SaxonXQConnection(SaxonXQDataSource dataSource) {
        config = dataSource.getConfiguration();
        staticContext = new SaxonXQStaticContext(config);
        init();
    }

    public Configuration getConfiguration() {
        return config;
    }

    public void close() {
        closed = true;
    }

    public void commit() throws XQException {
        checkNotClosed();
    }

    public XQExpression createExpression() throws XQException {
        checkNotClosed();
        return new SaxonXQExpression(this);
    }

    public XQExpression createExpression(XQStaticContext properties) throws XQException {
        return new SaxonXQExpression(this, (SaxonXQStaticContext)properties);
    }


    public boolean getAutoCommit() throws XQException {
        return false;
    }

    public XQMetaData getMetaData() throws XQException {
        checkNotClosed();
        return new SaxonXQMetaData(config);
    }


    public XQStaticContext getStaticContext() throws XQException {
        // create a new context object so that it can be modified without upsetting an existing one
        return new SaxonXQStaticContext(config);
    }

    public boolean isClosed() {
        return closed;
    }

    public XQPreparedExpression prepareExpression(InputStream xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(InputStream xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = ((SaxonXQStaticContext)properties).getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery, null);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        } catch (IOException e) {
            throw newXQException(e);
        }
    }

    public XQPreparedExpression prepareExpression(Reader xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(Reader xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = ((SaxonXQStaticContext)properties).getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        } catch (IOException e) {
            throw newXQException(e);
        }
    }

    public XQPreparedExpression prepareExpression(String xquery) throws XQException {
        return prepareExpression(xquery, staticContext);
    }

    public XQPreparedExpression prepareExpression(String xquery, XQStaticContext properties) throws XQException {
        checkNotClosed();
        try {
            StaticQueryContext sqc = ((SaxonXQStaticContext)properties).getSaxonStaticQueryContext();
            XQueryExpression exp = sqc.compileQuery(xquery);
            DynamicQueryContext dqc = new DynamicQueryContext(config);
            return new SaxonXQPreparedExpression(this, exp, dqc);
        } catch (XPathException e) {
            throw newXQException(e);
        }
    }

    public void rollback() throws XQException {
        checkNotClosed();
        // no-op
    }


    public void setAutoCommit(boolean autoCommit) throws XQException {
        checkNotClosed();
        // no-op
    }

    public void setStaticContext(XQStaticContext properties) throws XQException {
        staticContext = (SaxonXQStaticContext)properties;
    }

    private void checkNotClosed() throws XQException {
        if (closed) {
            throw new XQException("Connection has been closed");
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//