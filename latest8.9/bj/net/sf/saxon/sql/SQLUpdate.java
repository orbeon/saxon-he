package net.sf.saxon.sql;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
* An sql:update element in the stylesheet.
*  @author Mathias Payer <mathias.payer@nebelwelt.net>
*  @author Michael Kay
* <p/>
* For example:
* <pre>
*   &lt;sql:update connection="{$connection}" table="table-name" where="{$where}"
*                 xsl:extension-element-prefixes="sql"&gt;
*       &ltsql:column name="column-name" select="$new_value" /&gt;
*   &lt;/sql:update&gt;
* <p/>
* </pre>
*/

public class SQLUpdate extends ExtensionInstruction {

    Expression connection;
    String table;
    Expression where;

    public void prepareAttributes() throws XPathException {

		table = getAttributeList().getValue("", "table");
		if (table==null) {
            reportAbsence("table");
        }

        String dbWhere = getAttributeList().getValue("", "where");
        if (dbWhere == null) {
            where = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            where = makeAttributeValueTemplate(dbWhere);
        }


        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt==null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }
    }

    public void validate() throws XPathException {
        super.validate();
        where = typeCheck("where", where);
        connection = typeCheck("connection", connection);
    }

    public Expression compile(Executable exec) throws XPathException {

        // Collect names of columns to be added

        StringBuffer statement = new StringBuffer(120);
        statement.append("UPDATE " + table + " SET ");

        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo child;
		int cols = 0;
		while (true) {
            child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
		    if (child instanceof SQLColumn) {
    			if (cols++ > 0)	statement.append(',');
    			String colname = ((SQLColumn)child).getColumnName();
    			statement.append(colname);
    			statement.append("=?");
		    }
		}

        return new UpdateInstruction(connection, statement.toString(), getColumnInstructions(exec), where);
    }

    public List getColumnInstructions(Executable exec) throws XPathException {
        List list = new ArrayList(10);

        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo child;
		while (true) {
            child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
		    if (child instanceof SQLColumn) {
    			list.add(((SQLColumn)child).compile(exec));
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
            for (int i=0; i<columnInstructions.size(); i++) {
                sub[i+FIRST_COLUMN] = (Expression)columnInstructions.get(i);
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

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Prepare the SQL statement (only do this once)

            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection) ) {
                dynamicError("Value of connection expression is not a JDBC Connection", SaxonErrorCode.SXSQ0001, context);
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();
            PreparedStatement ps = null;

            String dbWhere = arguments[WHERE].evaluateAsString(context);
            String localstmt = statement;

            if (!dbWhere.equals("")) {
            	localstmt += " WHERE " + dbWhere;
            }

            try {
              	ps=connection.prepareStatement(localstmt);

                // Add the actual column values to be inserted

                int i = 1;
                for (int c=FIRST_COLUMN; c<arguments.length; c++) {
                    AtomicValue v = (AtomicValue)((SQLColumn.ColumnInstruction)arguments[c]).getSelectValue(context);

         			// TODO: the values are all strings. There is no way of adding to a numeric column
           		    String val = v.getStringValue();

           		    // another hack: setString() doesn't seem to like single-character string values
           		    if (val.length()==1) val += " ";
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
                   } catch (SQLException ignore) {}
               }
            }

            return null;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
// Original code in SQLInsert.java by Michael Kay
// Adaption to SQLUpdate.java by Mathias Payer <mathias.payer@nebelwelt.net>