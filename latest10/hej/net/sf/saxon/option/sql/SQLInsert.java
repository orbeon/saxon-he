////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.sql;

import com.saxonica.functions.sql.SQLFunctionSet;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.AttributeInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.ComponentDeclaration;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Whitespace;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * An sql:insert element in the stylesheet.
 */

public class SQLInsert extends ExtensionInstruction {

    Expression connection;
    String table;

    public void prepareAttributes() {

        table = attributes().getValue("", "table");
        // TODO: allow table to be an AVT
        if (table == null) {
            reportAbsence("table");
        }
        table = SQLConnect.quoteSqlName(table);

        AttributeInfo connectAtt = attributes().get("", "connection");
        if (connectAtt == null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt.getValue(), connectAtt);
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        super.validate(decl);
        connection = typeCheck("connection", connection);
        for (NodeInfo curr : children()) {
            if (curr instanceof SQLColumn) {
                // OK
            } else if (curr.getNodeKind() == Type.TEXT && Whitespace.isWhite(curr.getStringValueCS())) {
                // OK
            } else {
                compileError("Only sql:column is allowed as a child of sql:insert", "XTSE0010");
            }
        }

    }

    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {

        // Collect names of columns to be added

        StringBuilder statement = new StringBuilder(120);
        statement.append("INSERT INTO ").append(table).append(" (");

        int cols = 0;
        for (NodeInfo child : children(SQLColumn.class::isInstance)) {
            if (cols++ > 0) {
                statement.append(',');
            }
            String colname = ((SQLColumn) child).getColumnName();
            statement.append(colname);
        }
        statement.append(") VALUES (");

        // Add "?" marks for the variable parameters

        for (int i = 0; i < cols; i++) {
            if (i != 0) {
                statement.append(',');
            }
            statement.append('?');
        }

        statement.append(')');

        return new InsertInstruction(connection, statement.toString(), getColumnInstructions(exec, decl));
    }

    /*@NotNull*/
    public List<Expression> getColumnInstructions(Compilation exec, ComponentDeclaration decl) throws XPathException {
        List<Expression> list = new ArrayList<>(10);
        for (NodeInfo child : children(SQLColumn.class::isInstance)) {
            list.add(((SQLColumn) child).compile(exec, decl));
        }
        return list;
    }

    private static class InsertInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int FIRST_COLUMN = 1;
        String statement;

        public InsertInstruction(Expression connection, String statement, List<Expression> columnInstructions) {
            Expression[] sub = new Expression[columnInstructions.size() + 1];
            sub[CONNECTION] = connection;
            for (int i = 0; i < columnInstructions.size(); i++) {
                sub[i + FIRST_COLUMN] = columnInstructions.get(i);
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
            return "sql:insert";
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {

            // Prepare the SQL statement (only do this once)

            Connection connection = SQLFunctionSet.expectConnection(arguments[CONNECTION], context);

            try (PreparedStatement ps = connection.prepareStatement(statement)) {
                ParameterMetaData metaData = ps.getParameterMetaData();

                // Add the actual column values to be inserted

                int i = 1;
                for (int c = FIRST_COLUMN; c < arguments.length; c++) {
                    AtomicValue v = (AtomicValue) arguments[c].head();
                    String parameterClassName = null;
                    try {
                        parameterClassName = metaData.getParameterClassName(c);
                    } catch (SQLException ex) {
                        parameterClassName = "java.lang.String";
                    }
                    Object value;
                    switch (parameterClassName) {
                        case "java.lang.String":
                            value = v.getStringValue();
                            break;
                        case "java.sql.Date":
                            value = java.sql.Date.valueOf(v.getStringValue());
                            break;
                        default:
                            try {
                                Class targetClass = Class.forName(parameterClassName);
                                PJConverter converter = PJConverter.allocate(context.getConfiguration(), v.getPrimitiveType(), StaticProperty.ALLOWS_ONE, targetClass);
                                value = converter.convert(v, targetClass, context);
                            } catch (ClassNotFoundException err) {
                                throw new XPathException("xsl:insert - cannot convert value to required class " + parameterClassName);
                            }
                            break;
                    }

                    ps.setObject(i++, value);

                }

                ps.executeUpdate();
                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

            } catch (SQLException ex) {
                dynamicError("SQL INSERT failed: " + ex.getMessage(), SaxonErrorCode.SXSQ0004, context);
            }

            return EmptySequence.getInstance();
        }

    }


}

