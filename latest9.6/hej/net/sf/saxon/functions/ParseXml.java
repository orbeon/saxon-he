////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sender;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;

public class ParseXml extends SystemFunctionCall implements Callable {

    String expressionBaseURI = null;

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }


    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     *
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    public NodeInfo evaluateItem(XPathContext context) throws XPathException {
        return evalParseXml(
                (StringValue) argument[0].evaluateItem(context),
                context);
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public NodeInfo call(XPathContext context, Sequence[] arguments) throws XPathException {
        return evalParseXml(
                (StringValue) arguments[0].head(),
                context);
    }

    private NodeInfo evalParseXml(StringValue inputArg, XPathContext context) throws XPathException {
        String baseURI = expressionBaseURI;

        try {
            Controller controller = context.getController();
            if (controller == null) {
                throw new XPathException("parse-xml() function is not available in this environment");
            }
            Configuration configuration = controller.getConfiguration();

            StringReader sr = new StringReader(inputArg.getStringValue());
            InputSource is = new InputSource(sr);
            is.setSystemId(baseURI);
            Source source = new SAXSource(is);
            source.setSystemId(baseURI);

            Builder b = TreeModel.TINY_TREE.makeBuilder(controller.makePipelineConfiguration());
            Receiver s = b;
            ParseOptions options = new ParseOptions();
            options.setStripSpace(Whitespace.XSLT);
            options.setErrorListener(context.getConfiguration().getErrorListener());

            if (controller.getExecutable().stripsInputTypeAnnotations()) {
                s = configuration.getAnnotationStripper(s);
            }
            s.setPipelineConfiguration(b.getPipelineConfiguration());

            Sender.send(source, s, options);

            TinyDocumentImpl node = (TinyDocumentImpl) b.getCurrentRoot();
            node.setBaseURI(baseURI);
            node.setSystemId(null);
            b.reset();
            return node;
        } catch (XPathException err) {
            throw new XPathException("First argument to parse-xml() is not a well-formed and namespace-well-formed XML document. XML parser reported: " +
                    err.getMessage(), "FODC0006");
        }
    }
}

//Copyright (c) 2010 Saxonica Limited. All rights reserved.
