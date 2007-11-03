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
 * Saxon implementation of the XQJ XQExpression interface
 */
public class SaxonXQExpression extends SaxonXQDynamicContext implements XQExpression {

    SaxonXQConnection connection;
    StaticQueryContext sqc;
    DynamicQueryContext context;
    Configuration config;
    boolean closed;

    SaxonXQExpression(SaxonXQConnection connection) {
        this.connection = connection;
        config = connection.getConfiguration();
        context = new DynamicQueryContext(config);
        sqc = new StaticQueryContext(config);
    }

    SaxonXQExpression(SaxonXQConnection connection, SaxonXQStaticContext staticContext) {
        this.connection = connection;
        config = connection.getConfiguration();
        context = new DynamicQueryContext(config);
        sqc = staticContext.getSaxonStaticQueryContext();
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        return connection;
    }

    protected void checkNotClosed() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        if (isClosed()) {
            throw new XQException("Expression has been closed");
        }
    }

    public void cancel() throws XQException {
        checkNotClosed();
        //
    }

    public void close() {
        closed = true;
    }

    public void executeCommand(Reader command) throws XQException {
        checkNotClosed();
        throw new XQException("Saxon does not recognize any non-XQuery commands");
    }

    public void executeCommand(String command) throws XQException {
        checkNotClosed();
        throw new XQException("Saxon does not recognize any non-XQuery commands");
    }

    public XQResultSequence executeQuery(InputStream query) throws XQException {
        checkNotClosed();
        try {
            XQueryExpression exp = sqc.compileQuery(query, null);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        } catch (IOException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQResultSequence executeQuery(Reader query) throws XQException {
        checkNotClosed();
        try {
            XQueryExpression exp = sqc.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        } catch (IOException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQResultSequence executeQuery(String query) throws XQException {
        checkNotClosed();
        try {
            XQueryExpression exp = sqc.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, context);
            return pe.executeQuery();
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQStaticContext getStaticContext() throws XQException {
        return connection.getStaticContext();
    }

    public boolean isClosed() {
        if (connection.isClosed()) {
            closed = true;
        }
        return closed;
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