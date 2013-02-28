package net.sf.saxon.expr.instruct;

import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.evpull.*;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Cardinality;

import java.util.Stack;


/**
 * An instruction that creates an element node. There are two subtypes, FixedElement
 * for use where the name is known statically, and Element where it is computed
 * dynamically. To allow use in both XSLT and XQuery, the class acts both as an
 * Instruction and as an Expression.
*/

public abstract class ElementCreator extends ParentNodeConstructor {

    /**
     * The inheritNamespaces flag indicates that the namespace nodes on the element created by this instruction
     * are to be inherited (copied) on the children of this element. That is, if this flag is false, the child
     * elements must carry a namespace undeclaration for all the namespaces on the parent, unless they are
     * redeclared in some way.
     */

    protected boolean inheritNamespaces = true;

    /**
     * Construct an ElementCreator. Exists for the benefit of subclasses.
     */

    public ElementCreator() { }

    /**
     * Get the item type of the value returned by this instruction
     * @return the item type
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ELEMENT;
    }

    /**
     * Determine whether the inherit namespaces flag is set
     * @return true if namespaces constructed on a parent element are to be inherited by its children
     */

    public boolean isInheritNamespaces() {
        return inheritNamespaces;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        int p= super.computeSpecialProperties() |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
        if (getValidationAction() == Validation.STRIP) {
            p |= StaticProperty.ALL_NODES_UNTYPED;
        }
        return p;
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int parentValidationMode) {
        if (validation == parentValidationMode && getSchemaType() == null) {
            // TODO: is this safe? e.g. if the child has validation=strict but matches a skip wildcard in the parent
            setValidationAction(Validation.PRESERVE, null);
        }
    }

    /**
     * Check statically whether the content of the element creates attributes or namespaces
     * after creating any child nodes
     * @param env the static context
     * @throws XPathException
     */

    protected void checkContentSequence(StaticContext env) throws XPathException {
        if (content instanceof Block) {
            TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            Expression[] components = ((Block)content).getChildren();
            boolean foundChild = false;
            boolean foundPossibleChild = false;
            int childNodeKinds = (1<<Type.TEXT | 1<<Type.ELEMENT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION);
            for (Expression component : components) {

                ItemType it = component.getItemType(th);
                if (it instanceof NodeTest) {
                    boolean maybeEmpty = Cardinality.allowsZero(component.getCardinality());
                    int possibleNodeKinds = ((NodeTest) it).getNodeKindMask();
                    if ((possibleNodeKinds & 1 << Type.TEXT) != 0) {
                        // the text node might turn out to be zero-length. If that's a possibility,
                        // then we only issue a warning. Also, we need to completely ignore a known
                        // zero-length text node, which is included to prevent space-separation
                        // in an XQuery construct like <a>{@x}{@y}</b>
                        if (component instanceof ValueOf &&
                                ((ValueOf) component).select instanceof StringLiteral) {
                            String value = (((StringLiteral) ((ValueOf) component).select).getStringValue());
                            if (value.length() == 0) {
                                // continue;  // not an error
                            } else {
                                foundChild = true;
                            }
                        } else {
                            foundPossibleChild = true;
                        }
                    } else if ((possibleNodeKinds & ~childNodeKinds) == 0) {
                        if (maybeEmpty) {
                            foundPossibleChild = true;
                        } else {
                            foundChild = true;
                        }
                    } else if (foundChild && possibleNodeKinds == 1 << Type.ATTRIBUTE && !maybeEmpty) {
                        XPathException de = new XPathException(
                                "Cannot create an attribute node after creating a child of the containing element");
                        de.setErrorCode(isXSLT() ? "XTDE0410" : "XQTY0024");
                        de.setLocator(component);
                        throw de;
                    } else if (foundChild && possibleNodeKinds == 1 << Type.NAMESPACE && !maybeEmpty) {
                        XPathException de = new XPathException(
                                "Cannot create a namespace node after creating a child of the containing element");
                        de.setErrorCode(isXSLT() ? "XTDE0410" : "XQTY0024");
                        de.setLocator(component);
                        throw de;
                    } else if ((foundChild || foundPossibleChild) && possibleNodeKinds == 1 << Type.ATTRIBUTE) {
                        env.issueWarning(
                                "Creating an attribute here will fail if previous instructions create any children",
                                component);
                    } else if ((foundChild || foundPossibleChild) && possibleNodeKinds == 1 << Type.NAMESPACE) {
                        env.issueWarning(
                                "Creating a namespace node here will fail if previous instructions create any children",
                                component);
                    }
                }
            }

        }
    }

    /**
     * Determine (at run-time) the name code of the element being constructed
     *
     *
     * @param context the XPath dynamic evaluation context
     * @param copiedNode for the benefit of xsl:copy, the node being copied; otherwise null
     * @return the integer name code representing the element name
     * @throws XPathException if a failure occurs
     */

    public abstract NodeName getElementName(XPathContext context, /*@Nullable*/ NodeInfo copiedNode) throws XPathException;

    /**
     * Get the base URI for the element being constructed
     * @param context the XPath dynamic evaluation context
     * @param copiedNode the node being copied (for xsl:copy), otherwise null
     * @return the base URI of the constructed element
     */

    protected abstract String getNewBaseURI(XPathContext context, NodeInfo copiedNode);

    /**
     * Callback to output namespace nodes for the new element. This method is responsible
     * for ensuring that a namespace node is always generated for the namespace of the element
     * name itself.
     *
     *
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @param nameCode the name code of the element being created
     * @param copiedNode the node being copied (for xsl:copy) or null otherwise
     * @throws XPathException if a dynamic error occurs
     */

    protected abstract void outputNamespaceNodes(
            XPathContext context, Receiver receiver, NodeName nameCode, /*@Nullable*/ NodeInfo copiedNode)
    throws XPathException;

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * @return the set of namespace bindings.
     * @throws net.sf.saxon.trans.XPathException if an error occurs
     */

    public NamespaceBinding[] getActiveNamespaces() throws XPathException {
        return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.EVALUATE_METHOD;
    }

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        return iterateEvents(context, null);
    }

    protected EventIterator iterateEvents(XPathContext context, /*@Nullable*/ NodeInfo copiedNode) throws XPathException {
        if (!preservingTypes && validation != Validation.STRIP) {
            // Schema validation can't be done in pull mode
            return new SingletonEventIterator(evaluateItem(context));
        }
        final Controller controller = context.getController();
        assert controller != null;
        StartElementEvent start = new StartElementEvent(controller.makePipelineConfiguration());
        start.setElementName(getElementName(context, copiedNode));
        start.setTypeCode(validation == Validation.PRESERVE ? AnyType.getInstance() : Untyped.getInstance());
        start.setLocalNamespaces(getActiveNamespaces());
        start.setLocationId(locationId);
        EventIterator result = new BracketedElementIterator(
                start, content.iterateEvents(context), EndElementEvent.getInstance());
        if (validation == Validation.STRIP && controller.getExecutable().isSchemaAware()) {
            return new EventAnnotationStripper(result);
        } else {
            return result;
        }
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context XPath dynamic evaluation context
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context)
    throws XPathException {
        return processLeavingTail(context, null);
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context XPath dynamic evaluation context
     * @param copiedNode null except in the case of xsl:copy, when it is the node being copied; otherwise null
     * @return null (this instruction never returns a tail call)
     * @throws XPathException if a dynamic error occurs
     */
    protected final TailCall processLeavingTail(XPathContext context, /*@Nullable*/ NodeInfo copiedNode)
    throws XPathException {

        try {

            NodeName elemName = getElementName(context, copiedNode);
            SchemaType typeCode = (validation == Validation.PRESERVE ? AnyType.getInstance() : Untyped.getInstance());

            SequenceReceiver out = context.getReceiver();
            SequenceReceiver saved = out;
            boolean pop = false;
            Receiver elemOut = out;
            if (!preservingTypes) {
                Receiver validator = context.getConfiguration().getElementValidator(
                        out, elemName, locationId,
                        getSchemaType(), validation);

                if (validator != out) {
                    out = new TreeReceiver(validator);
                    context.setReceiver(out);
                    pop = true;
                }
                elemOut = out;
            }

            if (elemOut.getSystemId() == null) {
                elemOut.setSystemId(getNewBaseURI(context, copiedNode));
            }
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            elemOut.startElement(elemName, typeCode, locationId, properties);

            // output the required namespace nodes via a callback

            outputNamespaceNodes(context, elemOut, elemName, copiedNode);

            // process subordinate instructions to generate attributes and content
            content.process(context);

            // output the element end tag (which will fail if validation fails)
            elemOut.endElement();
            if (pop) {
                context.setReceiver(saved);
            }
            return null;

        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }

    /**
     * In streaming mode, process the first half of the instruction (to start a new document or element)
     * @param contextStack the dynamic evaluation context
     * @param state a stack on which the instruction can save state information for use during the corresponding
     */

    public void processLeft(Stack<XPathContext> contextStack, Stack<Object> state) throws XPathException {
        processLeft(contextStack, state, null);
    }

    /**
     * In streaming mode, process the first half of the instruction (to start a new document or element)
     * @param contextStack the dynamic evaluation context
     * @param state a stack on which the instruction can save state information for use during the corresponding
     * @param copiedNode for the benefit of xsl:copy, the node being copied; otherwise null
     * @throws XPathException if a dynamic error occurs
     */

    protected final void processLeft(Stack<XPathContext> contextStack, Stack<Object> state, /*@Nullable*/ NodeInfo copiedNode) throws XPathException {
        XPathContext context = contextStack.peek();
        try {
            NodeName elemName = getElementName(context, copiedNode);
            SchemaType typeCode = (validation == Validation.PRESERVE ? AnyType.getInstance() : Untyped.getInstance());

            SequenceReceiver out = context.getReceiver();
            state.push(out);
            Receiver elemOut = out;
            if (!preservingTypes) {
                Controller controller = context.getController();
                assert controller != null;
                Receiver validator = controller.getConfiguration().getElementValidator(
                        out, elemName, locationId,
                        getSchemaType(), validation);

                if (validator != out) {
                    out = new TreeReceiver(validator);
                    context.setReceiver(out);
                }
                elemOut = out;
            }

            if (elemOut.getSystemId() == null) {
                elemOut.setSystemId(getNewBaseURI(context, copiedNode));
            }
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            elemOut.startElement(elemName, typeCode, locationId, properties);

            // output the required namespace nodes via a callback

            outputNamespaceNodes(context, elemOut, elemName, copiedNode);

        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }

    /**
     * In streaming mode, proecss the right half of the instruction (to end a new document or element)
     * @param contextStack the stack of XPath context objects for the current execution state
     * @param state   a stack on which the instruction can save state information for use during the corresponding
     * @throws XPathException if a dynamic error occurs
     */

    public void processRight(Stack<XPathContext> contextStack, Stack<Object> state) throws XPathException {
        XPathContext context = contextStack.peek();
        SequenceReceiver out = (SequenceReceiver)state.pop();
        out.endElement();
        context.setReceiver(out);
    }

    /**
     * Evaluate the constructor, returning the constructed element node. If lazy construction
     * mode is in effect, then an UnconstructedParent object is returned instead.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
       if (isLazyConstruction() && preservingTypes) {
           return context.getConfiguration().makeUnconstructedElement(this, context);
       } else {
           return constructElement(context, null);
       }
    }

    /**
     * Construct the element node as a free-standing (parentless) node in a tiny tree
     * @param context XPath dynamic evaluation context
     * @param copiedNode for the benefit of xsl:copy, the node being copied
     * @return the constructed element node
     * @throws XPathException if a dynamic error occurs
     */
    private NodeInfo constructElement(XPathContext context, /*@Nullable*/ NodeInfo copiedNode) throws XPathException {
        try {
            Controller controller = context.getController();
            assert controller != null;
            SequenceReceiver saved = context.getReceiver();
            SequenceOutputter seq = controller.allocateSequenceOutputter(1);
            seq.getPipelineConfiguration().setHostLanguage(getHostLanguage());

            NodeName elemName = getElementName(context, copiedNode);
            SchemaType typeCode = (validation == Validation.PRESERVE ? AnyType.getInstance() : Untyped.getInstance());

            SequenceReceiver ini = seq;
            if (!preservingTypes) {
                Receiver validator = controller.getConfiguration().getElementValidator(
                        ini, elemName, locationId,
                        getSchemaType(), validation);

                if (ini.getSystemId() == null) {
                    ini.setSystemId(getNewBaseURI(context, copiedNode));
                }
                if (validator == ini) {
                    context.setReceiver(ini);
                } else {
                    TreeReceiver tr = new TreeReceiver(validator);
                    tr.setPipelineConfiguration(seq.getPipelineConfiguration());
                    context.setReceiver(tr);
                    ini = tr;
                }
            } else {
                context.setReceiver(ini);
                if (ini.getSystemId() == null) {
                    ini.setSystemId(getNewBaseURI(context, copiedNode));
                }
            }

            ini.open();
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            ini.startElement(elemName, typeCode, locationId, properties);

            // output the namespace nodes for the new element
            outputNamespaceNodes(context, ini, elemName, null);

            content.process(context);

            ini.endElement();
            ini.close();
            context.setReceiver(saved);

            // the constructed element is the first and only item in the sequence
            NodeInfo result = (NodeInfo)seq.popLastItem();
            seq.reset();
            return result;

        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException)err).setSourceLocator(this);
                ((ValidationException)err).setSystemId(getSystemId());
            }
            err.maybeSetLocation(this);
            err.maybeSetContext(context);
            throw err;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//