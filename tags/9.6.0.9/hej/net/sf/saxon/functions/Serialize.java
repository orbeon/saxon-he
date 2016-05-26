////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.om.*;
import net.sf.saxon.serialize.SerializationParamsHandler;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Properties;

/**
 * Implementation of fn:serialize() as defined in XPath 3.0
 */

public class Serialize extends SystemFunctionCall implements Callable {

    int locationId = 0;

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    public StringValue evaluateItem(XPathContext context) throws XPathException {
        return evalSerialize(argument[0].iterate(context),
                argument.length == 1 ? null : (NodeInfo) argument[1].evaluateItem(context), context);
    }

    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        return evalSerialize(arguments[0].iterate(),
                arguments.length == 1 ? null : (NodeInfo) arguments[1].head(), context);
    }

    private StringValue evalSerialize(SequenceIterator iter, /*@Nullable*/ NodeInfo param, XPathContext context) throws XPathException {

        try {
            FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.LARGE);
            boolean first = true;
            Item item;
            while ((item = iter.next()) != null) {
                if (!first) {
                    buffer.append(" ");
                } else {
                    first = false;
                }
                if (item instanceof NodeInfo) {
                    NodeInfo node = (NodeInfo) item;

                    if (node.getNodeKind() == Type.ATTRIBUTE || node.getNodeKind() == Type.NAMESPACE) {
                        throw new XPathException("Attribute and namespace nodes cannot be serialized", "SENR0001");
                    }

                    Properties props = new Properties();
                    props.setProperty(OutputKeys.METHOD, "xml");
                    if (param != null) {
                        if (param.getNodeKind() != Type.ELEMENT ||
                                !NamespaceConstant.OUTPUT.equals(param.getURI()) ||
                                !"serialization-parameters".equals(param.getLocalPart())) {
                            throw new XPathException("Second argument to fn:serialize() must be an element named {"
                                    + NamespaceConstant.OUTPUT + "}serialization-parameters", "XPTY0004");
                        }
                        SerializationParamsHandler sph = new SerializationParamsHandler();
                        sph.setLocator(this);
                        sph.setSerializationParams(param);
                        props = sph.getSerializationProperties();

                    }
                    try {
                        StringWriter result = new StringWriter();
                        XPathContext c2 = context.newMinorContext();

                        SerializerFactory sf = context.getConfiguration().getSerializerFactory();
                        PipelineConfiguration pipe = context.getConfiguration().makePipelineConfiguration();
                        pipe.setController(context.getController());
                        Receiver receiver = sf.getReceiver(new StreamResult(result), pipe, props);

                        c2.changeOutputDestination(receiver, null);
                        SequenceReceiver out = c2.getReceiver();
                        out.open();
                        node.copy(out, CopyOptions.ALL_NAMESPACES | CopyOptions.TYPE_ANNOTATIONS, locationId);
                        out.close();
                        buffer.append(result.toString());
                    } catch (Exception e) {
                        if (e instanceof XPathException) {
                            throw (XPathException) e;
                        } else {
                            XPathException se = new XPathException("Serialization unsuccessful", e);
                            se.setErrorCode("SEPM0016");
                            throw se;
                        }
                    }
                } else if (item instanceof AtomicValue || item instanceof ObjectValue) {
                    buffer.append(item.getStringValue());
                } else {
                    throw new XPathException("Function items cannot be serialized", "SENR0001");
                }
            }
            return new StringValue(buffer.condense());
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }

}

// Copyright (c) 2011 Saxonica Limited. All rights reserved.
