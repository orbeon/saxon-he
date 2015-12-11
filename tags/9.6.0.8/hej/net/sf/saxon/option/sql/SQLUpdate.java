////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.sql;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.ComponentDeclaration;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An sql:update element in the stylesheet.
 *
 * @author Mathias Payer <mathias.payer@nebelwelt.net>
 * @author Michael Kay
 *         <p/>
 *         For example:
 *         <pre>
 *                   &lt;sql:update connection="$connection" table="table-name" where="{$where}"
 *                                 xsl:extension-element-prefixes="sql"&gt;
 *                       &ltsql:column name="column-name" select="$new_value" /&gt;
 *                   &lt;/sql:update&gt;
 *
 *                 </pre>
 */

public class SQLUpdate extends ExtensionInstruction {

    Expression connection;
    String table;
    Expression where;

    public void prepareAttributes() throws XPathException {

        table = getAttributeList().getValue("", "table");
        if (table == null) {
            reportAbsence("table");
        }
        table = SQLConnect.quoteSqlName(table);

        String dbWhere = getAttributeList().getValue("", "where");
        if (dbWhere == null) {
            where = new StringLiteral(StringValue.EMPTY_STRING, this);
        } else {
            where = makeAttributeValueTemplate(dbWhere);
        }


        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt == null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        super.validate(decl);
        where = typeCheck("where", where);
        connection = typeCheck("connection", connection);
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo curr = (NodeInfo) kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof SQLColumn) {
                // OK
            } else if (curr.getNodeKind() == Type.TEXT && Whitespace.isWhite(curr.getStringValueCS())) {
                // OK
            } else {
                compileError("Only sql:column is allowed as a child of sql:update", "XTSE0010");
            }
        }
    }

    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {

        // Collect names of columns to be added

        FastStringBuffer statement = new FastStringBuffer(FastStringBuffer.MEDIUM);
        statement.append("UPDATE " + table + " SET ");

        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo child;
        int cols = 0;
        while (true) {
            child = (NodeInfo) kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof SQLColumn) {
                if (cols++ > 0) statement.append(',');
                String colname = ((SQLColumn) child).getColumnName();
                statement.append(colname);
                statement.append("=?");
            }
        }

        return new UpdateInstruction(connection, statement.toString(), getColumnInstructions(exec, decl), where);
    }

    /*@NotNull*/
    public List getColumnInstructions(Compilation exec, ComponentDeclaration decl) throws XPathException {
        List list = new ArrayList(10);

        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo child;
        while (true) {
            child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof SQLColumn) {
                list.add(((SQLColumn) child).compile(exec, decl));
            }
        }

        return list;
    }

    private static class UpdateInstruction extends SimpleExpression {

        private static final long serialVersionUID = -4234440812734827279L;
        public static final int CONNECTION = 0;
        public static final int WHERE = 1;
        public static final int FIRST_COLUMN = 2;
        String statement;

        public UpdateInstruction(Expression connection, String statement, List columnInstructions, Expression where) {
            Expression[] sub = new Expression[columnInstructions.size() + 2];
            sub[CONNECTION] = connection;
            sub[WHERE] = where;
            for (int i = 0; i < columnInstructions.size(); i++) {
                sub[i + FIRST_COLUMN] = (Expression) columnInstructions.get(i);
            }
            this.statement = statement;
            setArguments(sub);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.EVALUATE_METHOD;
        }

        public String getExpressionType() {
            return "sql:update";
        }

//        public Expression promote(PromotionOffer offer) throws XPathException {
//            if (offer.action != PromotionOffer.FOCUS_INDEPENDENT && offer.action != PromotionOffer.EXTRACT_GLOBAL_VARIABLES) {
//                // See comments in corresponding method for SQLInsert
//                return super.promote(offer);
//            } else {
//                return this;
//            }
//        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {

            // Prepare the SQL statement (only do this once)

            Item conn = arguments[CONNECTION].head();
            if (!(conn instanceof ObjectValue && ((ObjectValue) conn).getObject() instanceof Connection)) {
                dynamicError("Value of connection expression is not a JDBC Connection", SaxonErrorCode.SXSQ0001, context);
            }
            Connection connection = (Connection) ((ObjectValue) conn).getObject();
            PreparedStatement ps = null;

            String dbWhere = arguments[WHERE].head().getStringValue();
            String localstmt = statement;

            if (dbWhere.length() != 0) {
                localstmt += " WHERE " + dbWhere;
            }

            try {
                ps = connection.prepareStatement(localstmt);

                // Add the actual column values to be inserted

                int i = 1;
                for (int c = FIRST_COLUMN; c < arguments.length; c++) {
                    AtomicValue v = (AtomicValue) arguments[c].head();

                    // TODO: the values are all strings. There is no way of adding to a numeric column
                    String val = v.getStringValue();

                    // another hack: setString() doesn't seem to like single-character string values
                    if (val.length() == 1) val += " ";
                    //System.err.println("Set statement parameter " + i + " to " + val);
                    ps.setObject(i++, val);

                }

                ps.executeUpdate();
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

            } catch (SQLException ex) {
                dynamicError("SQL UPDATE failed: " + ex.getMessage(), SaxonErrorCode.SXSQ0004, context);
            } finally {
                if (ps != null) {
                    try {
                        ps.close();
                    } catch (SQLException ignore) {
                    }
                }
            }

            return EmptySequence.getInstance();
        }

    }


}

