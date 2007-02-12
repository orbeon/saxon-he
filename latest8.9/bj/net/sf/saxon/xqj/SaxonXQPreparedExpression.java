package net.sf.saxon.xqj;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.GlobalParam;
import net.sf.saxon.instruct.GlobalVariable;
import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.sort.IntHashSet;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Set;

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
    private DynamicQueryContext context;
    private SaxonXQConnection connection;
    private boolean closed;
    private boolean scrollable;

    protected SaxonXQPreparedExpression(SaxonXQConnection connection,
                                        XQueryExpression expression,
                                        DynamicQueryContext context)
    throws XQException {
        this.connection = connection;
        this.expression = expression;
        this.context = context;
        this.scrollable = connection.getScrollability() == XQConstants.SCROLLTYPE_SCROLLABLE;
    }

    protected DynamicQueryContext getDynamicContext() {
        return context;
    }

    protected void checkNotClosed() throws XQException {
        if (isClosed()) {
            throw new XQException("Expression has been closed");
        }
    }

    protected SaxonXQDataFactory getDataFactory() throws XQException {
        if (connection.isClosed()) {
            close();
        }
        checkNotClosed();
        return connection;
    }

    public void cancel()  throws XQException {
        checkNotClosed();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearWarnings() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() {
        closed = true;
    }

    public XQResultSequence executeQuery() throws XQException {
        checkNotClosed();
        try {
            SequenceIterator iter = expression.iterator(context);
            if (scrollable) {
                Value value = Value.asValue(SequenceExtent.makeSequenceExtent(iter));
                return new SaxonXQSequence(value, connection.getConfiguration(), connection);
            } else {
                return new SaxonXQForwardSequence(iter, connection);
            }
        } catch (XPathException de) {
            throw new XQException(de.getMessage(), de, null, null);
        }
    }

    public QName[] getAllExternalVariables() throws XQException {
        checkNotClosed();
        IntHashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        if (vars == null || vars.size() == 0) {
            return EMPTY_QNAME_ARRAY;
        } else {
            IntHashSet params = new IntHashSet(vars.size());
            Iterator iter = vars.valueIterator();
            while (iter.hasNext()) {
                GlobalVariable var = (GlobalVariable)iter.next();
                if (var instanceof GlobalParam) {
                    params.add(var.getVariableFingerprint());
                };
            }
            QName[] qnames = new QName[params.size()];
            int q=0;
            NamePool pool = connection.getConfiguration().getNamePool();
            for (IntIterator it=params.iterator(); it.hasNext();) {
                int fp = it.next();
                qnames[q++] = new QName(pool.getURI(fp), pool.getLocalName(fp), pool.getPrefix(fp));
            }
            return qnames;
        }
    }

    private static QName[] EMPTY_QNAME_ARRAY = new QName[0];

    public QName[] getUnboundExternalVariables() throws XQException {
        checkNotClosed();
        Set boundParameters = getDynamicContext().getParameters().keySet();
        IntHashSet unbound = new IntHashSet(boundParameters.size());
        QName[] all = getAllExternalVariables();
        for (int i=0; i<all.length; i++) {
            String clark = "{" + all[i].getNamespaceURI() + "}" + all[i].getLocalPart();
            if (!boundParameters.contains(clark)) {
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

    public int getQueryTimeout() throws XQException {
        checkNotClosed();
        return 0;  
    }

    public XQSequenceType getStaticResultType() throws XQException {
        checkNotClosed();
        Expression exp = expression.getExpression();    // unwrap two layers!
        ItemType itemType = exp.getItemType(connection.getConfiguration().getTypeHierarchy());
        int cardinality = exp.getCardinality();
        SequenceType staticType = SequenceType.makeSequenceType(itemType, cardinality);
        return new SaxonXQSequenceType(staticType, connection.getConfiguration());
    }

    public XQSequenceType getStaticVariableType(QName name) throws XQException {
        checkNotClosed();
        NamePool pool = connection.getConfiguration().getNamePool();
        int fp = pool.allocate(name.getPrefix(), name.getNamespaceURI(), name.getLocalPart()) & NamePool.FP_MASK;
        IntHashMap vars = expression.getExecutable().getCompiledGlobalVariables();
        GlobalVariable var = (vars == null ? null : (GlobalVariable)vars.get(fp));
        if (var == null) {
            throw new XQException("Variable " + name + " is not declared");
        }
        return new SaxonXQSequenceType(var.getRequiredType(), connection.getConfiguration());
    }

    public XQWarning getWarnings() throws XQException {
        checkNotClosed();
        return null;  
    }

    public boolean isClosed() {
        if (connection.isClosed()) {
            close();
        }
        return closed;
    }

    public void setQueryTimeout(int seconds) {
        // no-op
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