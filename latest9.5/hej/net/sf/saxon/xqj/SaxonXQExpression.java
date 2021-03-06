package net.sf.saxon.xqj;

import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;

import javax.xml.namespace.QName;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQResultSequence;
import javax.xml.xquery.XQStaticContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Saxon implementation of the XQJ XQExpression interface
 */
public class SaxonXQExpression extends SaxonXQDynamicContext implements XQExpression {

    private SaxonXQStaticContext sqc;
    private DynamicQueryContext context;

    SaxonXQExpression(SaxonXQConnection connection) throws XQException {
        this.connection = connection;
        context = new DynamicQueryContext(connection.getConfiguration());
        context.setApplyFunctionConversionRulesToExternalVariables(false);
        sqc = (SaxonXQStaticContext)connection.getStaticContext(); // this takes a snapshot copy
        setClosableContainer(connection);
    }

    SaxonXQExpression(SaxonXQConnection connection, SaxonXQStaticContext staticContext) {
        this.connection = connection;
        context = new DynamicQueryContext(connection.getConfiguration());
        context.setApplyFunctionConversionRulesToExternalVariables(false);
        sqc = new SaxonXQStaticContext(staticContext);  // take a snapshot copy
        setClosableContainer(connection);
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        return connection;
    }

    public void cancel() throws XQException {
        checkNotClosed();
        //
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
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query, null);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
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
        SaxonXQDataSource.checkNotNull(query, "query");
        try {
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
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
            StaticQueryContext env = sqc.getSaxonStaticQueryContext();
            XQueryExpression exp = env.compileQuery(query);
            SaxonXQPreparedExpression pe = new SaxonXQPreparedExpression(connection, exp, sqc, context);
            XQResultSequence result = pe.executeQuery();
            ((Closable)result).setClosableContainer(this);
            return result;
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
        }
    }

    public XQStaticContext getStaticContext() throws XQException {
        checkNotClosed();
        return sqc;
    }

    protected boolean externalVariableExists(QName name) {
        return true;
    }
}

// Copyright (c) 2013 Saxonica Limited. All rights reserved.