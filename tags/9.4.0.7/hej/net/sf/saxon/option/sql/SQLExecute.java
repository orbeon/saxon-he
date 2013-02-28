package net.sf.saxon.option.sql;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.Declaration;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.value.ObjectValue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * An sql:execute element in the stylesheet.
 * <p/>
 * For example:
 * <pre>
 *   &lt;sql:execute statement="{$statement}"
 *                 xsl:extension-element-prefixes="sql"/ &gt;
 * <p/>
 * </pre>
 *
 *
 * @author Michael Kay
 */

public class SQLExecute extends ExtensionInstruction {

    Expression connection;
    Expression statement;


    public void prepareAttributes() throws XPathException {
        // Attributes for SQL-statement
        String statementAtt = getAttributeValue("", "statement");
        if (statementAtt == null) {
            reportAbsence("statement");
        }
        statement = makeAttributeValueTemplate(statementAtt);

        String connectAtt = getAttributeValue("", "connection");
        if (connectAtt == null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }


    }

    public void validate(Declaration decl) throws XPathException {
        super.validate(decl);
        statement = typeCheck("statement", statement);
        connection = typeCheck("connection", connection);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return new SqlStatementInstruction(connection, statement);
    }

    private static class SqlStatementInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int STATEMENT = 1;

        public SqlStatementInstruction(Expression connection,
                                Expression statement) {
            Expression[] sub = {connection, statement};
            setArguments(sub);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.PROCESS_METHOD;
        }

        /*@NotNull*/ public String getExpressionType() {
            return "sql:statement";
        }

        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
            Item conn = arguments[CONNECTION].next();
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection)) {
                XPathException de = new XPathException("Value of connection expression is not a JDBC Connection");
                de.setXPathContext(context);
                throw de;
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();

            String statementText = arguments[STATEMENT].next().getStringValue();

            try {
                if ("COMMIT WORK".equals(statementText)) {
                    connection.commit();
                } else if ("ROLLBACK WORK".equals(statementText)) {
                    connection.rollback();
                } else {
                    Statement s = connection.createStatement();
                    s.execute(statementText);
                }

            } catch (SQLException ex) {
                XPathException de = new XPathException("(SQL) " + ex.getMessage());
                de.setXPathContext(context);
                throw de;
            }

            return EmptyIterator.getInstance();
        }
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s): claudio.thomas@unix-ag.org (based on SQLInsert.java)
//