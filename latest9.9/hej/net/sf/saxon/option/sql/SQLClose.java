////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.sql;

import com.saxonica.functions.sql.SQLFunctionSet;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SimpleExpression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.ComponentDeclaration;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * An sql:close element in the stylesheet.
 */

public class SQLClose extends ExtensionInstruction {

    /*@Nullable*/ Expression connection = null;

    public void prepareAttributes() throws XPathException {
        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt == null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt, getAttributeList().getIndex("", "connection"));
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        super.validate(decl);
        connection = typeCheck("connection", connection);
    }

    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        return new CloseInstruction(connection);
    }

    private static class CloseInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;

        public CloseInstruction(Expression connect) {
            Expression[] sub = {connect};
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
            return "sql:close";
        }

        public int computeCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }

        public Sequence<?> call(XPathContext context, Sequence[] arguments) throws XPathException {
            Connection connection = SQLFunctionSet.expectConnection(arguments[CONNECTION], context);
            try {
                connection.close();
            } catch (SQLException ex) {
                dynamicError("(SQL) Failed to close connection: " + ex.getMessage(), SaxonErrorCode.SXSQ0002, context);
            }
            return EmptySequence.getInstance();
        }


    }
}

