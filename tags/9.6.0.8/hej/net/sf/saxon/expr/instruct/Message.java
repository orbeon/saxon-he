////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.InterpretedExpressionCompiler;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;
import java.util.Properties;

/**
 * An xsl:message element in the stylesheet.
 */

public class Message extends Instruction {

    /*@NotNull*/ private Expression terminate;
    /*@NotNull*/ private Expression select;
    private Expression errorCode;
    private NamespaceResolver nsResolver;
    private boolean isAssert;

    /**
     * Create an xsl:message instruction
     *
     * @param select    the expression that constructs the message (composite of the select attribute
     *                  and the contained sequence constructor)
     * @param terminate expression that calculates terminate = yes or no.
     * @param errorCode expression used to compute the error code
     */

    public Message(/*@NotNull*/ Expression select, /*@NotNull*/ Expression terminate, Expression errorCode) {
        this.terminate = terminate;
        this.select = select;
        if (errorCode == null) {
            this.errorCode = new StringLiteral("Q{" + NamespaceConstant.ERR + "}XTMM9000", select.getContainer());
        } else {
            this.errorCode = errorCode;
        }
        adoptChildExpression(terminate);
        adoptChildExpression(select);
    }

    /**
     * Say whether this instruction is implementing xsl:message or xsl:assert
     *
     * @param isAssert true if this is xsl:assert; false if it is xsl:message
     */

    public void setIsAssert(boolean isAssert) {
        this.isAssert = isAssert;
    }

    /**
     * Set a namespace resolver for resolving any namespace prefix appearing in the value of the error-code
     * attribute
     *
     * @param resolver the namespace resolver to be used
     */

    public void setNamespaceResolver(NamespaceResolver resolver) {
        this.nsResolver = resolver;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        select = visitor.typeCheck(select, contextInfo);
        adoptChildExpression(select);
        terminate = visitor.typeCheck(terminate, contextInfo);
        adoptChildExpression(terminate);
        errorCode = visitor.typeCheck(errorCode, contextInfo);
        adoptChildExpression(errorCode);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        terminate = visitor.optimize(terminate, contextItemType);
        adoptChildExpression(terminate);
        errorCode = visitor.optimize(errorCode, contextItemType);
        adoptChildExpression(errorCode);
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new Message(select.copy(), terminate.copy(), errorCode.copy());
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return isAssert ? StandardNames.XSL_ASSERT : StandardNames.XSL_MESSAGE;
    }

    /**
     * Get the item type. To avoid spurious compile-time type errors, we falsely declare that the
     * instruction can return anything
     *
     * @return AnyItemType
     */
    /*@NotNull*/
    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Get the static cardinality. To avoid spurious compile-time type errors, we falsely declare that the
     * instruction returns zero or one items - this is always acceptable
     *
     * @return zero or one
     */

    public int getCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        terminate = doPromotion(terminate, offer);
        errorCode = doPromotion(errorCode, offer);
    }

    @Override
    public Iterable<Operand> operands() {
        return operandList(
                new Operand(select, OperandRole.SINGLE_ATOMIC),
                new Operand(terminate, OperandRole.SINGLE_ATOMIC));
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (terminate == original) {
            terminate = replacement;
            found = true;
        }
        if (errorCode == original) {
            errorCode = replacement;
            found = true;
        }
        return found;
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        assert controller != null;
        Receiver emitter = controller.getMessageEmitter();
        if (emitter != null) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (emitter) {
                // In Saxon-EE, multithreading can cause different messages to be entangled unless we synchronize.

                SequenceReceiver rec = new TreeReceiver(emitter);
                rec = new AttributeMasker(rec);

                SequenceReceiver saved = context.getReceiver();
                int savedOutputState = context.getTemporaryOutputState();

                Properties props = new Properties();
                props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                SerializerFactory sf = context.getConfiguration().getSerializerFactory();
                PipelineConfiguration pipe = controller.makePipelineConfiguration();
                pipe.setHostLanguage(Configuration.XSLT);
                Receiver receiver = sf.getReceiver(rec, pipe, props);
                context.changeOutputDestination(receiver, null);
                context.setTemporaryOutputState(StandardNames.XSL_MESSAGE);

                boolean abort = false;
                String term = Whitespace.trim(terminate.evaluateAsString(context));
                if (term.equals("no")||term.equals("false")||term.equals("0")) {
                    // no action
                } else if (term.equals("yes")||term.equals("true")||term.equals("1")) {
                    abort = true;
                } else {
                    XPathException e = new XPathException("The terminate attribute of xsl:message must be yes|no|true|false|1|0");
                    e.setXPathContext(context);
                    e.setErrorCode("XTDE0030");
                    throw e;
                }


                rec.startDocument(abort ? ReceiverOptions.TERMINATE : 0);

                SequenceIterator iter = select.iterate(context);
                Item item;
                while ((item = iter.next()) != null) {
                    rec.append(item, locationId, NodeInfo.ALL_NAMESPACES);
                }

                rec.endDocument();

                context.setReceiver(saved);
                context.setTemporaryOutputState(savedOutputState);
                if (abort) {
                    TerminationException te = new TerminationException(
                            "Processing terminated by " + StandardErrorListener.getInstructionName(this) +
                                    " at line " + getLineNumber() +
                                    " in " + StandardErrorListener.abbreviatePath(getSystemId()));
                    te.setLocator(this);
                    try {
                        String code = errorCode.evaluateAsString(context).toString();
                        StructuredQName errorCode = StructuredQName.fromLexicalQName(
                                code, false, true, nsResolver);
                        te.setErrorCodeQName(errorCode);
                    } catch (XPathException err) {
                        // no action, ignore the error
                    }
                    throw te;
                }
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("xslMessage");
        out.endElement();
    }

    /**
     * The AttributeMasker is a filter applied to the message pipeline which is designed to ensure that outputting an attribute
     * with no containing element (for example &lt;xsl:message select="@x"/>) is not an error. Such an attribute is wrapped in
     * a processing instruction so it can exist as a child of a document node.
     */

    private static class AttributeMasker extends ProxyReceiver {
        private boolean contentStarted = true;

        public AttributeMasker(SequenceReceiver next) {
            super(next);
        }

        public void startElement(NodeName nameCode, SchemaType typeCode, int locationId, int properties) throws XPathException {
            contentStarted = false;
            super.startElement(nameCode, typeCode, locationId, properties);
        }

        public void startContent() throws XPathException {
            contentStarted = true;
            super.startContent();
        }


        public void attribute(NodeName attributeName, SimpleType typeCode, CharSequence value, int locationId, int properties)
                throws XPathException {
            if (contentStarted) {
                String attName = attributeName.getDisplayName();
                processingInstruction("attribute", "name=\"" + attName + "\" value=\"" + value + "\"", 0, 0);
            } else {
                super.attribute(attributeName, typeCode, value, locationId, properties);
            }
        }

        public void namespace(NamespaceBinding namespaceBinding, int properties) throws XPathException {
            if (contentStarted) {
                String prefix = namespaceBinding.getPrefix();
                String uri = namespaceBinding.getURI();
                processingInstruction("namespace", "prefix=\"" + prefix + "\" uri=\"" + uri + "\"", 0, 0);
            } else {
                super.namespace(namespaceBinding, properties);
            }
        }

        public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
            if (item instanceof NodeInfo) {
                int kind = ((NodeInfo) item).getNodeKind();
                if (kind == Type.ATTRIBUTE || kind == Type.NAMESPACE) {
                    ((NodeInfo) item).copy(this, 0, 0);
                    return;
                }
            }
            ((SequenceReceiver) nextReceiver).append(item, locationId, copyNamespaces);
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Message expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new InterpretedExpressionCompiler();
    }
//#endif
}