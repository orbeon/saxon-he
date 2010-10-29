package net.sf.saxon;

import net.sf.saxon.expr.CollationMap;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import javax.xml.transform.SourceLocator;

/**
 * An abstraction of ExpressionVisitor that provides the minimal set of services needed by
 * the TypeChecker; used to allow run-time type-checking of higher order function arguments without
 * retaining the whole static context
 */
public interface TypeCheckerEnvironment {

    public Configuration getConfiguration();

    public void issueWarning(String message, SourceLocator locator);

    public CollationMap getCollationMap();

    public XPathContext makeDynamicContext();

    public Expression simplify(Expression exp) throws XPathException;

    public Expression typeCheck(Expression exp, ItemType contextItemType) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

