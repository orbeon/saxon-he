package net.sf.saxon.option.sql;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.Declaration;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
* An sql:connect element in the stylesheet.
*/

public class SQLConnect extends ExtensionInstruction {

    Expression database;
    Expression driver;
    Expression user;
    Expression password;
    Expression autoCommit = new Literal(EmptySequence.getInstance());

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    public void prepareAttributes() throws XPathException {

        // Get mandatory database attribute

        String dbAtt = getAttributeValue("", "database");
        if (dbAtt==null) {
            reportAbsence("database");
            dbAtt = ""; // for error recovery
        }
        database = makeAttributeValueTemplate(dbAtt);

	    // Get driver attribute

        String dbDriver = getAttributeValue("", "driver");
        if (dbDriver==null) {
            if (dbAtt.length()>9 && dbAtt.substring(0,9).equals("jdbc:odbc")) {
                dbDriver = "sun.jdbc.odbc.JdbcOdbcDriver";
            } else {
                reportAbsence("driver");
            }
        }
        driver = makeAttributeValueTemplate(dbDriver);


        // Get and expand user attribute, which defaults to empty string

        String userAtt = getAttributeValue("", "user");
        if (userAtt==null) {
            user = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            user = makeAttributeValueTemplate(userAtt);
        }

        // Get and expand password attribute, which defaults to empty string

        String pwdAtt = getAttributeValue("", "password");
        if (pwdAtt==null) {
            password = new StringLiteral(StringValue.EMPTY_STRING);
        } else {
            password = makeAttributeValueTemplate(pwdAtt);
        }

        // Get auto-commit attribute if specified

        String autoCommitAtt = getAttributeValue("", "auto-commit");
        if (autoCommitAtt!=null) {
            autoCommit = makeAttributeValueTemplate(autoCommitAtt);
        }
    }

    public void validate(Declaration decl) throws XPathException {
        super.validate(decl);
        getConfiguration().checkLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION, "sql:connect");
        database = typeCheck("database", database);
        driver = typeCheck("driver", driver);
        user = typeCheck("user", user);
        password = typeCheck("password", password);
        autoCommit = typeCheck("auto-commit", autoCommit);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return new ConnectInstruction(database, driver, user, password, autoCommit);
    }

    private static class ConnectInstruction extends SimpleExpression {

        public static final int DATABASE = 0;
        public static final int DRIVER = 1;
        public static final int USER = 2;
        public static final int PASSWORD = 3;
        public static final int AUTOCOMMIT = 4;

        public ConnectInstruction(Expression database,
            Expression driver, Expression user, Expression password, Expression autoCommit) {

            Expression[] subs = {database, driver, user, password, autoCommit};
            setArguments(subs);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.EVALUATE_METHOD;
        }

        public int computeCardinality() {
            return StaticProperty.EXACTLY_ONE;
        }

        public String getExpressionType() {
            return "sql:connect";
        }

        public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {

            // Establish the JDBC connection

            Connection connection = null;      // JDBC Database Connection

            String dbString = str(arguments[DATABASE]);
    	    String dbDriverString = str(arguments[DRIVER]);
            String userString = str(arguments[USER]);
            String pwdString = str(arguments[PASSWORD]);
            String autoCommitString = str(arguments[AUTOCOMMIT]);

            try {
                // the following hack is necessary to load JDBC drivers
    	        Class.forName(dbDriverString);
            } catch (ClassNotFoundException e) {
                XPathException err = new XPathException("Failed to load JDBC driver " + dbDriverString, e);
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXSQ0003);
                err.setLocator(this);
                throw err;
            }
            try {
                connection = DriverManager.getConnection(dbString, userString, pwdString);
            } catch (SQLException ex) {
                XPathException err = new XPathException("JDBC Connection Failure", ex);
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXSQ0003);
                err.setLocator(this);
                throw err;
            }

            try {
                if (autoCommitString.length() > 0) {
                    connection.setAutoCommit("yes".equals(autoCommitString));
                }
            } catch (SQLException e) {
                XPathException err = new XPathException("Failed to set autoCommit on JDBC connection " + dbDriverString, e);
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXSQ0003);
                err.setLocator(this);
                throw err;
            }

            return Value.asIterator(new ObjectValue(connection));

        }

        private String str(/*@NotNull*/ SequenceIterator iterator) throws XPathException {
            Item item = iterator.next();
            return (item == null ? "" : item.getStringValue());
        }
    }

    /**
     * Utility method to quote a SQL table or column name if it needs quoting.
     * @param name the supplied name
     * @return the supplied name, enclosed in double quotes if it does not satisfy the pattern [A-Za-z_][A-Za-z0-9_]*,
     * with any double quotes replaced by two double quotes
     */

    public static String quoteSqlName(String name) throws IllegalArgumentException {
        // TODO: allow an embedded double-quote to be escaped as two double-quotes
        if (namePattern.matcher(name).matches()) {
            return name;
        }
        return "\"" + name + "\"";
    }

    private static Pattern namePattern = Pattern.compile("\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_]*");
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
// Contributor(s): Rick Bonnett [rbonnett@acadia.net]
//
