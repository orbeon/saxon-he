package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInListType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.StringValue;

import java.util.Set;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class TypeAvailable extends Available implements CallableExpression {

    private Set<String> importedSchemaNamespaces;
    private boolean isXSLT20basic;

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        StaticContext env = visitor.getStaticContext();
        isXSLT20basic = !env.isSchemaAware() && !env.getXPathLanguageLevel().equals(DecimalValue.THREE);
        super.checkArguments(visitor);
        if (!(argument[0] instanceof Literal &&
                (argument.length==1 || argument[1] instanceof Literal))) {
            importedSchemaNamespaces = visitor.getStaticContext().getImportedSchemaNamespaces();
        }
    }

    /**
    * preEvaluate: this method uses the static context to do early evaluation of the function
    * if the argument is known (which is the normal case)
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        nsContext = visitor.getStaticContext().getNamespaceResolver();
        String lexicalQName = ((Literal)argument[0]).getValue().getStringValue();
        Configuration config = visitor.getConfiguration();
        boolean b = typeAvailable(lexicalQName, config, visitor.getStaticContext().getImportedSchemaNamespaces());
        return Literal.makeLiteral(BooleanValue.get(b));
    }

    /**
     * Run-time evaluation.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        StringValue nameValue = (StringValue)av1;
        boolean b = typeAvailable(nameValue.getStringValue(), context.getConfiguration(), importedSchemaNamespaces);
        return BooleanValue.get(b);
    }

    private boolean typeAvailable(String lexicalName, Configuration config, Set<String> importedSchemaNamespaces) throws XPathException {
        StructuredQName qName;
        try {
            if (lexicalName.indexOf(':') < 0) {
                String uri = nsContext.getURIForPrefix("", true);
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                qName = StructuredQName.fromLexicalQName(lexicalName,
                    false,
                    config.getNameChecker(),
                    nsContext);
            }
        } catch (XPathException e) {
            e.setErrorCode("XTDE1428");
            e.setLocator(this);
            throw e;
        }

        String uri = qName.getURI();
        if (uri.equals(NamespaceConstant.JAVA_TYPE)) {
            try {
                config.getClass(qName.getLocalPart(), false, null);
                return(true);
            } catch (XPathException err) {
                return(false);
            }
        } else if (uri.equals(NamespaceConstant.SCHEMA) || importedSchemaNamespaces.contains(uri)) {
            final int fp = config.getNamePool().allocate(
                    qName.getPrefix(), uri, qName.getLocalPart()) & 0xfffff;
            SchemaType type = config.getSchemaType(fp);
            return type != null &&
                    !(isXSLT20basic && (type instanceof BuiltInListType ||
                                (type instanceof BuiltInAtomicType && !((BuiltInAtomicType) type).isAllowedInBasicXSLT())));
        } else {
            return false;
        }
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, /*@NotNull*/ XPathContext context) throws XPathException {
        String lexicalQName = arguments[0].next().getStringValue();
        boolean b = typeAvailable(lexicalQName, context.getConfiguration(), importedSchemaNamespaces);
        return SingletonIterator.makeIterator(BooleanValue.get(b));
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
// Contributor(s):
//