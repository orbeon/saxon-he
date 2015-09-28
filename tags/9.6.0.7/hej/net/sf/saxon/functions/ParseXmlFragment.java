////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.Statistics;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.StringReader;

public class ParseXmlFragment extends SystemFunctionCall implements Callable {

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
        NodeInfo node;
        final String baseURI = expressionBaseURI;

        try {
            Controller controller = context.getController();
            if (controller == null) {
                throw new XPathException("parse-xml-fragment() function is not available in this environment");
            }
            Configuration configuration = controller.getConfiguration();
            final StringReader fragmentReader = new StringReader(inputArg.getStringValue());

            String skeleton = "<!DOCTYPE z [<!ENTITY e SYSTEM \"http://www.saxonica.com/parse-xml-fragment/actual.xml\">]>\n<z>&e;</z>";
            StringReader skeletonReader = new StringReader(skeleton);

            InputSource is = new InputSource(skeletonReader);
            is.setSystemId(baseURI);
            Source source = new SAXSource(is);
            XMLReader reader = configuration.getSourceParser();
            if(reader.getEntityResolver() != null) {
                try {
                    reader = XMLReaderFactory.createXMLReader();
                } catch (SAXException e) {
                    throw new XPathException(e);
                }
            }
            ((SAXSource)source).setXMLReader(reader);
            source.setSystemId(baseURI);

            Builder b = controller.makeBuilder();
            if (b instanceof TinyBuilder) {
                ((TinyBuilder) b).setStatistics(Statistics.FN_PARSE_STATISTICS);
            }
            Receiver s = b;
            ParseOptions options = new ParseOptions();
            reader.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    if ("http://www.saxonica.com/parse-xml-fragment/actual.xml".equals(systemId)) {
                        InputSource is = new InputSource(fragmentReader);
                        is.setSystemId(baseURI);
                        return is;
                    } else {
                        return null;
                    }
                }
            });
            options.setStripSpace(Whitespace.XSLT);
            options.setErrorListener(context.getConfiguration().getErrorListener());

            if (controller.getExecutable().stripsInputTypeAnnotations()) {
                s = configuration.getAnnotationStripper(s);
            }
            s.setPipelineConfiguration(b.getPipelineConfiguration());

            options.addFilter(new FilterFactory() {
                public ProxyReceiver makeFilter(Receiver next) {
                    return new OuterElementStripper(next);
                }
            });

            Sender.send(source, s, options);

            node = b.getCurrentRoot();
            b.reset();
        } catch (XPathException err) {
            throw new XPathException("First argument to parse-xml-fragment() is not a well-formed and namespace-well-formed XML fragment. XML parser reported: " +
                    err.getMessage(), "FODC0006");
        }
        return node;
    }

    /**
     * Filter to remove the element wrapper added to the document to satisfy the XML parser
     */

    private static class OuterElementStripper extends ProxyReceiver {

        public OuterElementStripper(Receiver next) {
            super(next);
        }

        private int level = 0;
        private boolean suppressStartContent = false;

        /**
         * Notify the start of an element
         *
         * @param elemName   integer code identifying the name of the element within the name pool.
         * @param typeCode   integer code identifying the element's type within the name pool.
         * @param properties properties of the element node
         */
        @Override
        public void startElement(NodeName elemName, SchemaType typeCode, int locationId, int properties) throws XPathException {
            if (level > 0) {
                super.startElement(elemName, typeCode, locationId, properties);
            } else {
                // The Linked Tree builder rejects two calls on startContent(), so we must suppress the next one
                suppressStartContent = true;
            }
            level++;
        }

        @Override
        public void startContent() throws XPathException {
            if (!suppressStartContent) {
                super.startContent();
            }
            suppressStartContent = false;
        }

        /**
         * End of element
         */
        @Override
        public void endElement() throws XPathException {
            level--;
            if (level > 0) {
                super.endElement();
            }
        }
    }
}

//Copyright (c) 2012 Saxonica Limited. All rights reserved.
