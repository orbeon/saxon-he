package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.NamespaceCodeIterator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Whitespace;

import java.util.Iterator;
import java.util.Stack;

/**
* Handler for xsl:copy elements in stylesheet.
*/

public class Copy extends ElementCreator {

    private boolean copyNamespaces;
    private ItemType resultItemType;
    private Expression select;

    /**
     * Create a shallow copy instruction
     * @param select selects the node (or other item) to be copied
     * @param copyNamespaces true if namespace nodes are to be copied when copying an element
     * @param inheritNamespaces true if child elements are to inherit the namespace nodes of their parent
     * @param schemaType the Schema type against which the content is to be validated
     * @param validation the schema validation mode
     */

    public Copy(Expression select,
                boolean copyNamespaces,
                boolean inheritNamespaces,
                SchemaType schemaType,
                int validation) {
        this.copyNamespaces = copyNamespaces;
        this.inheritNamespaces = inheritNamespaces;
        this.select = select;
        setSchemaType(schemaType);
        this.validation = validation;
        preservingTypes = schemaType == null && validation == Validation.PRESERVE;
        if (copyNamespaces) {
            setLazyConstruction(false);
            // can't do lazy construction at present in cases where namespaces need to be copied from the
            // source document.
        }
    }

    /**
     * Get the expression that selects the node or other item to be copied
     * @return the select expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        preservingTypes |= !visitor.getExecutable().isSchemaAware();
        return super.simplify(visitor);
    }


    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        try {
            select = visitor.typeCheck(select, contextItemType);
            adoptChildExpression(select);
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart().equals("XPDY0002")) {
                // See spec bug 7624, test case copy903err
                err.setErrorCode("XTTE0945");
                err.maybeSetLocation(this);
            }
            select = new Literal(EmptySequence.getInstance()); // to prevent duplicate error reporting
            throw err;
        }
        ItemType selectItemType = select.getItemType(visitor.getConfiguration().getTypeHierarchy());

        if (selectItemType instanceof NodeTest) {
            switch (selectItemType.getPrimitiveType()) {
                // For elements and attributes, assume the type annotation will change
                case Type.ELEMENT:
                    this.resultItemType = NodeKindTest.ELEMENT;
                    break;
                case Type.ATTRIBUTE:
                    this.resultItemType = NodeKindTest.ATTRIBUTE;
                    break;
                case Type.DOCUMENT:
                    this.resultItemType = NodeKindTest.DOCUMENT;
                    break;
                default:
                    this.resultItemType = selectItemType;
            }
        } else {
            this.resultItemType = selectItemType;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Copy copy = new Copy(select, copyNamespaces, inheritNamespaces, getSchemaType(), validation);
        copy.setContentExpression(content.copy());
        return copy;
    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     * @return a set of bit-significant flags identifying the dependencies of
     *         the expression
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY;
    }

    /**
      * Get the immediate sub-expressions of this expression.
      * @return an iterator containing the sub-expressions of this expression
      */

    public Iterator<Expression> iterateSubExpressions() {
        return new PairIterator(select, content);
    }

    /**
     * Get the item type of the result of this instruction.
     * @return The context item type.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (resultItemType != null) {
            return resultItemType;
        } else {
            resultItemType = computeItemType(th);
            return resultItemType;
        }
    }

    private ItemType computeItemType(TypeHierarchy th) {
        ItemType selectItemType = select.getItemType(th);
        Executable exec = getExecutable();
        if (!exec.isSchemaAware()) {
            return selectItemType;
        }
        // The rest of the code handles the complications of schema-awareness
        Configuration config = exec.getConfiguration();
        if (getSchemaType() != null) {
            int e = th.relationship(selectItemType, NodeKindTest.ELEMENT);
            if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ELEMENT, getSchemaType(), config);
            }
            int a = th.relationship(selectItemType, NodeKindTest.ATTRIBUTE);
            if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ATTRIBUTE, getSchemaType(), config);
            }
            return AnyNodeTest.getInstance();
        } else switch (validation) {
            case Validation.PRESERVE:
                return selectItemType;
            case Validation.STRIP: {
                int e = th.relationship(selectItemType, NodeKindTest.ELEMENT);
                if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                    return new ContentTypeTest(Type.ELEMENT, Untyped.getInstance(), config);
                }
                int a = th.relationship(selectItemType, NodeKindTest.ATTRIBUTE);
                if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                    return new ContentTypeTest(Type.ATTRIBUTE, BuiltInAtomicType.UNTYPED_ATOMIC, config);
                }
                if (e != TypeHierarchy.DISJOINT || a != TypeHierarchy.DISJOINT) {
                    // it might be an element or attribute
                    return AnyNodeTest.getInstance();
                } else {
                    // it can't be an element or attribute, so stripping type annotations can't affect it
                    return selectItemType;
                }
            }
            case Validation.STRICT:
            case Validation.LAX:
                if (selectItemType instanceof NodeTest) {
                    int fp = ((NodeTest)selectItemType).getFingerprint();
                    if (fp != -1) {
                        int e = th.relationship(selectItemType, NodeKindTest.ELEMENT);
                        if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                            SchemaDeclaration elem = config.getElementDeclaration(fp);
                            if (elem != null) {
                                return new ContentTypeTest(Type.ELEMENT, elem.getType(), config);
                            } else {
                                // No element declaration now, but there might be one at run-time
                                return new ContentTypeTest(Type.ELEMENT, AnyType.getInstance(), config);
                            }
                        }
                        int a = th.relationship(selectItemType, NodeKindTest.ATTRIBUTE);
                        if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                            SchemaDeclaration attr = config.getElementDeclaration(fp);
                            if (attr != null) {
                                return new ContentTypeTest(Type.ATTRIBUTE, attr.getType(), config);
                            } else {
                                 // No attribute declaration now, but there might be one at run-time
                                return new ContentTypeTest(Type.ATTRIBUTE, AnySimpleType.getInstance(), config);
                            }
                        }
                    } else {
                        int e = th.relationship(selectItemType, NodeKindTest.ELEMENT);
                        if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                            return NodeKindTest.ELEMENT;
                        }
                        int a = th.relationship(selectItemType, NodeKindTest.ATTRIBUTE);
                        if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                            return NodeKindTest.ATTRIBUTE;
                        }
                    }
                    return AnyNodeTest.getInstance();
                } else if (selectItemType instanceof AtomicType) {
                    return selectItemType;
                } else {
                    return AnyItemType.getInstance();
                }
            default:
                throw new IllegalStateException();
        }
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp == this) {
            if (resultItemType == null) {
                resultItemType = computeItemType(visitor.getConfiguration().getTypeHierarchy());
            }
            if (resultItemType.isAtomicType()) {
                return select;
            }
        }
        return exp;
    }

    /**
     * Callback from ElementCreator when constructing an element
     * @param context XPath dynamic evaluation context
     * @param copiedNode the node being copied
     * @return the namecode of the element to be constructed
     * @throws XPathException
     */

    public int getNameCode(XPathContext context, NodeInfo copiedNode)
            throws XPathException {
        return copiedNode.getNameCode();
    }

    /**
     * Get the base URI of a copied element node (the base URI is retained in the new copy)
     * @param context XPath dynamic evaluation context
     * @param copiedNode
     * @return the base URI
     */

    public String getNewBaseURI(XPathContext context, NodeInfo copiedNode) {
        return copiedNode.getBaseURI();
    }

    /**
     * Callback to output namespace nodes for the new element.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @param nameCode
     * @param copiedNode
     * @throws XPathException
     */

    protected void outputNamespaceNodes(XPathContext context, Receiver receiver, int nameCode, NodeInfo copiedNode)
    throws XPathException {
        if (copyNamespaces) {
            NamespaceCodeIterator.sendNamespaces(copiedNode, receiver);
        } else {
            // Always output the namespace of the element name itself
            receiver.namespace(context.getNamePool().getNamespaceCode(nameCode), 0);
        }
    }

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * The result is an array of namespace codes, the codes either occupy the whole array or are
     * terminated by a -1 entry. A result of null is equivalent to a zero-length array.
     */

    public int[] getActiveNamespaces() throws XPathException {
        if (copyNamespaces) {
            // we should have disabled lazy construction, so this shouldn't be called.
            throw new UnsupportedOperationException();
        } else {
            return null;
        }
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();
        Item item = select.evaluateItem(context);

        if (item == null) {
            // do nothing (XSLT 3.0)
//            // See spec bug 7624, test case copy904err
//            XPathException err = new XPathException("Context item for xsl:copy is undefined", "XTTE0945");
//            err.setLocator(this);
//            throw err;
        }
        if (!(item instanceof NodeInfo)) {
            out.append(item, locationId, NodeInfo.ALL_NAMESPACES);
            return null;
        }
        NodeInfo source = (NodeInfo)item;
        //out.getPipelineConfiguration().setBaseURI(source.getBaseURI());

        // Processing depends on the node kind.

        switch(source.getNodeKind()) {

        case Type.ELEMENT:
            // use the generic code for creating new elements
            return super.processLeavingTail(context, (NodeInfo)item);

        case Type.ATTRIBUTE:
            try {
                CopyOf.copyAttribute(source, getSchemaType(), validation, this, context, false);
            } catch (NoOpenStartTagException err) {
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
            }
            break;

        case Type.TEXT:
            out.characters(source.getStringValueCS(), locationId, 0);
            break;

        case Type.PROCESSING_INSTRUCTION:
            out.processingInstruction(source.getDisplayName(), source.getStringValueCS(), locationId, 0);
            break;

        case Type.COMMENT:
            out.comment(source.getStringValueCS(), locationId, 0);
            break;

        case Type.NAMESPACE:
            try {
                source.copy(out, 0, locationId);
            } catch (NoOpenStartTagException err) {
                XPathException e = new XPathException(err.getMessage());
                e.setXPathContext(context);
                e.setErrorCodeQName(err.getErrorCodeQName());
                throw dynamicError(this, e, context);
            }
            break;

        case Type.DOCUMENT:
            XPathContext c2 = context;
            if (!preservingTypes) {
                Receiver val = controller.getConfiguration().
                        getDocumentValidator(out, source.getBaseURI(),
                                validation, Whitespace.NONE, getSchemaType(), -1);
                if (val != out) {
                    SequenceReceiver sr = new TreeReceiver(val);
                    sr.setPipelineConfiguration(out.getPipelineConfiguration());
                    c2 = c2.newMinorContext();
                    c2.setReceiver(sr);
                    out = sr;
                }
            }
            out.startDocument(0);
            content.process(c2);
            out.endDocument();
            break;

        default:
            throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());

        }
        return null;
    }

    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceOutputter seq = controller.allocateSequenceOutputter(1);
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setHostLanguage(getHostLanguage());
        seq.setPipelineConfiguration(pipe);
        c2.setTemporaryReceiver(seq);
        process(c2);
        seq.close();
        Item item = seq.getFirstItem();
        seq.reset();
        return item;
    }

    /**
     * Process the first half of the instruction in streaming mode
     */

    public void processLeft(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        NodeInfo node = (NodeInfo)select.evaluateItem(context);
        int nodeKind = node.getNodeKind();
        switch (nodeKind) {
            case Type.DOCUMENT:
                XPathContext c2 = context;
                SequenceReceiver out = context.getReceiver();
                state.push(out);
                if (!preservingTypes) {
                    Controller controller = c2.getController();
                    Receiver val = controller.getConfiguration().
                            getDocumentValidator(out, node.getBaseURI(),
                                    validation, Whitespace.NONE, getSchemaType(), -1);
                    if (val != out) {
                        SequenceReceiver sr = new TreeReceiver(val);
                        sr.setPipelineConfiguration(out.getPipelineConfiguration());
                        c2 = c2.newMinorContext();   // TODO: we don't seem to use this
                        c2.setReceiver(sr);
                        out = sr;
                    }
                }
                out.startDocument(0);
                break;
            case Type.ELEMENT:
                super.processLeft(contextStack, state, node);
                break;
            default:
                try {
                    TailCall tc = processLeavingTail(context, node);
                    while (tc != null) {
                        tc = tc.processLeavingTail();
                    }
                } catch (XPathException err) {
                    err.maybeSetLocation(this);
                    throw err;
                }
                // TODO: need to ensure that the contained instructions within xsl:copy are ignored
                break;
        }
        state.push(nodeKind);
    }

    /**
     * Process the second half of the instruction in streaming mode
     */

    public void processRight(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        int nodeKind = (Integer)state.pop();
        switch (nodeKind) {
            case Type.DOCUMENT: {
                SequenceReceiver out = context.getReceiver();
                out.endDocument();
                out = (SequenceReceiver)state.pop();
                context.setReceiver(out);
                break;
            }
            case Type.ELEMENT: {
                super.processRight(contextStack, state);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("copy");
        content.explain(out);
        out.endElement();
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
//
