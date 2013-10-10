package net.sf.saxon.xqj;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.GlobalParam;
import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.z.IntHashSet;
import net.sf.saxon.z.IntIterator;

import javax.xml.namespace.QName;
import javax.xml.xquery.*;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Saxon implementation of the XQJ interface XQPreparedExpression. This represents a compiled XQuery
 * expression, together with the dynamic context for its evaluation. Note that this means the object
 * should not be used in more than one thread concurrently.
 * <p>
 * Note that an expression is scrollable or not depending on the scrollability property of the XQConnection
 * that was used to compile this expression (at the time it was compiled). If the expression is scrollable then
 * its results are delivered in an XQSequence that supports scrolling backwards as well as forwards.
 * <p>
 * For full Javadoc details, see the XQJ interface specification.
 */
public class SaxonXQPreparedExpression extends SaxonXQDynamicContext implements XQPreparedExpression {

    private XQueryExpression expression;
    private SaxonXQStaticContext staticContext;
    private DynamicQueryContext context;
    private boolean scrollable;

    protected SaxonXQPreparedExpression(SaxonXQConnection connection,
                                        XQueryExpression expression,
                                        /*@NotNull*/ SaxonXQStaticContext sqc,
                                        DynamicQueryContext context)
    throws XQException {
        this.connection = connection;
        this.expression = expression;
        this.staticContext = new SaxonXQStaticContext(sqc); // take a snapshot of the supplied static context
        this.context = context;
        scrollable = sqc.getScrollability() == XQConstants.SCROLLTYPE_SCROLLABLE;
        setClosableContainer(connection);
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected SaxonXQConnection getConnection() {
        return connection;
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        checkNotClosed();
        return connection;
    }

    protected XQueryExpression getXQueryExpression() {
        return expression;
    }

    protected SaxonXQStaticContext getSaxonXQStaticContext() {
        return staticContext;
    }

    public void cancel()  throws XQException {
        checkNotClosed();
    }

    /*@NotNull*/ public XQResultSequence executeQuery() throws XQException {
        checkNotClosed();
        try {
            SequenceIterator iter = expression.iterator(context);
            if (scrollable) {
                SequenceExtent value = new SequenceExtent(iter);
                return new SaxonXQSequence(value, this);
            } else {
                return new SaxonXQForwardSequence(iter, this);
            }
        } catch (XPathException de) {
            XQException xqe = new XQException(de.getMessage());
            xqe.initCause(de);
            throw xqe;
        }
    }

    /*@NotNull*/ public QName[] getAllExternalVariables() throws XQException {
        checkNotClosed();
        HashMap<StructuredQName, GlobalVariable> vars = expression.getExecutable().getCompiledGlobalVariables();
        if (vars == null || vars.isEmpty()) {
            return EMPTY_QNAME_ARRAY;
        } else {
            HashSet<StructuredQName> params = new HashSet<StructuredQName>(vars.size());
            for (GlobalVariable var : vars.values()) {
                if (var instanceof GlobalParam) {
                    StructuredQName q = var.getVariableQName();
                    if (!(q.getURI().equals(NamespaceConstant.SAXON) && q.getLocalPart().equals("context-item"))) {
                        params.add(q);
                    }
                }
            }
            QName[] qnames = new QName[params.size()];
            int q=0;
            for (StructuredQName name : params) {
                qnames[q++] = new QName(name.getURI(), name.getLocalPart(), name.getPrefix());
            }
            return qnames;
        }
    }

    /*@NotNull*/ private static QName[] EMPTY_QNAME_ARRAY = new QName[0];

    /*@NotNull*/ public QName[] getAllUnboundExternalVariables() throws XQException {
        checkNotClosed();
        java.util.Collection<StructuredQName> boundParameters = getDynamicContext().getParameters().getKeys();
        IntHashSet unbound = new IntHashSet(boundParameters.size());
        QName[] all = getAllExternalVariables();
        for (int i=0; i<all.length; i++) {
            StructuredQName sq = new StructuredQName("", all[i].getNamespaceURI(), all[i].getLocalPart());
            if (!boundParameters.contains(sq)) {
                unbound.add(i);
            }
        }
        QName[] unboundq = new QName[unbound.size()];
        int c = 0;
        IntIterator iter = unbound.iterator();
        while (iter.hasNext()) {
            int x = iter.next();
            unboundq[c++] = all[x];
        }
        return unboundq;
    }

    public XQStaticContext getStaticContext() throws XQException {
        checkNotClosed();
        return staticContext;   // a snapshot of the static context at the time the expression was created
        // old code in Saxon 9.2:
        // return new SaxonXQExpressionContext(expression);
    }

    /*@NotNull*/ public XQSequenceType getStaticResultType() throws XQException {
        checkNotClosed();
        Expression exp = expression.getExpression();    // unwrap two layers!
        ItemType itemType = exp.getItemType(connection.getConfiguration().getTypeHierarchy());
        int cardinality = exp.getCardinality();
        SequenceType staticType = SequenceType.makeSequenceType(itemType, cardinality);
        return new SaxonXQSequenceType(staticType, connection.getConfiguration());
    }

    /*@NotNull*/ public XQSequenceType getStaticVariableType(QName name) throws XQException {
        checkNotClosed();
        checkNotNull(name);
        StructuredQName qn = new StructuredQName(
                name.getPrefix(), name.getNamespaceURI(), name.getLocalPart());
        HashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        GlobalVariable var = (vars == null ? null : (GlobalVariable)vars.get(qn));
        if (var == null) {
            throw new XQException("Variable " + name + " is not declared");
        }
        return new SaxonXQSequenceType(var.getRequiredType(), connection.getConfiguration());
    }


    protected boolean externalVariableExists(QName name) {
        StructuredQName qn = new StructuredQName(
                name.getPrefix(), name.getNamespaceURI(), name.getLocalPart());
        HashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        GlobalVariable var = (vars == null ? null : (GlobalVariable)vars.get(qn));
        return var != null && var instanceof GlobalParam;
    }

    private void checkNotNull(/*@Nullable*/ Object arg) throws XQException {
        if (arg == null) {
            throw new XQException("Argument is null");
        }
    }

}

// Copyright (c) 2013 Saxonica Limited. All rights reserved.