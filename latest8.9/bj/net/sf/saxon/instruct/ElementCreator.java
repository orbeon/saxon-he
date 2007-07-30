package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.pull.UnconstructedElement;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.ValidationException;


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
     * The validating flag is set if the type attribute is set or if validation is set to anything other than
     * preserve. This is used simply to fast-path the case where no validation is required.
     */

    protected boolean validating = false;

    public ElementCreator() { }

    /**
     * Get the item type of the value returned by this instruction
     * @return the item type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ELEMENT;
    }

    /**
     * Determine whether this elementCreator performs validation
     */

    public boolean isValidating() {
        return validating;
    }

    /**
     * Determine whether the inherit namespaces flag is set
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
        return super.computeSpecialProperties() |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
    }

    /**
     * Set the validation mode for the new element
     */

    public void setValidationMode(int mode) {
        super.setValidationMode(mode);
        if (mode != Validation.PRESERVE) {
            validating = true;
        }
    }


    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int validationMode) {
        if (validation == validationMode) {
            setValidationMode(Validation.PRESERVE);
        }
    }

    /**
     * Check statically whether the content of the element creates attributes or namespaces
     * after creating any child nodes
     * @param env the static context
     * @throws XPathException
     */

    protected void checkContentForAttributes(StaticContext env) throws XPathException {
        if (content instanceof Block) {
            TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            Expression[] components = ((Block)content).getChildren();
            boolean foundChild = false;
            boolean foundPossibleChild = false;
            int childNodeKinds = (1<<Type.TEXT | 1<<Type.ELEMENT | 1<<Type.COMMENT | 1<<Type.PROCESSING_INSTRUCTION);
            for (int i=0; i<components.length; i++) {
                ItemType it = components[i].getItemType(th);
                if (it instanceof NodeTest) {
                    boolean maybeEmpty = Cardinality.allowsZero(components[i].getCardinality());
                    int possibleNodeKinds = ((NodeTest)it).getNodeKindMask();
                    if ((possibleNodeKinds & 1<<Type.TEXT) != 0) {
                        // the text node might turn out to be zero-length. If that's a possibility,
                        // then we only issue a warning. Also, we need to completely ignore a known
                        // zero-length text node, which is included to prevent space-separation
                        // in an XQuery construct like <a>{@x}{@y}</b>
                        if (components[i] instanceof ValueOf &&
                                ((ValueOf)components[i]).select instanceof StringLiteral) {
                            String value = (((StringLiteral)((ValueOf)components[i]).select).getStringValue());
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
                    } else if (foundChild && possibleNodeKinds == 1<<Type.ATTRIBUTE && !maybeEmpty) {
                        DynamicError de = new DynamicError(
                                "Cannot create an attribute node after creating a child of the containing element");
                        de.setErrorCode(isXSLT() ? "XTDE0410" : "XQTY0024");
                        de.setLocator(components[i]);
                        throw de;
                    } else if (foundChild && possibleNodeKinds == 1<<Type.NAMESPACE && !maybeEmpty) {
                        DynamicError de = new DynamicError(
                                "Cannot create a namespace node after creating a child of the containing element");
                        de.setErrorCode(isXSLT() ? "XTDE0410" : "XQTY0024");
                        de.setLocator(components[i]);
                        throw de;
                    } else if ((foundChild ||foundPossibleChild) && possibleNodeKinds == 1<<Type.ATTRIBUTE) {
                        env.issueWarning(
                                "Creating an attribute here will fail if previous instructions create any children",
                                components[i]);
                    } else if ((foundChild ||foundPossibleChild) && possibleNodeKinds == 1<<Type.NAMESPACE) {
                        env.issueWarning(
                                "Creating a namespace node here will fail if previous instructions create any children",
                                components[i]);
                    }
                }
            }

        }
    }

    public abstract int getNameCode(XPathContext context)
    throws XPathException;

    /**
     * Get the base URI for the element being constructed
     * @param context
     */

    public abstract String getNewBaseURI(XPathContext context);

    /**
     * Callback to output namespace nodes for the new element.
     * @param context The execution context
     * @param receiver the Receiver where the namespace nodes are to be written
     * @throws net.sf.saxon.trans.XPathException
     */

    protected abstract void outputNamespaceNodes(XPathContext context, Receiver receiver)
    throws XPathException;

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * The result is an array of namespace codes, the codes either occupy the whole array or are
     * terminated by a -1 entry. A result of null is equivalent to a zero-length array.
     */

    public int[] getActiveNamespaces() throws XPathException {
        return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.EVALUATE_METHOD;
    }

    /**
     * Evaluate the instruction to produce a new element node. This method is typically used when there is
     * a parent element or document in a result tree, to which the new element is added.
     * @param context
     * @return null (this instruction never returns a tail call)
     * @throws XPathException
     */
    public TailCall processLeavingTail(XPathContext context)
    throws XPathException {

        try {

            int nameCode = getNameCode(context);
            int typeCode = (validation == Validation.PRESERVE ? StandardNames.XS_ANY_TYPE : StandardNames.XS_UNTYPED);

            XPathContext c2 = context;
            SequenceReceiver out = context.getReceiver();

            if (validating) {
                Controller controller = context.getController();

                Receiver validator = controller.getConfiguration().getElementValidator(
                        out, nameCode, locationId,
                        getSchemaType(), validation);

                if (validator != out) {
                    c2 = context.newMinorContext();
                    c2.setOrigin(this);
                    out = new TreeReceiver(validator);
                    final PipelineConfiguration pipe = controller.makePipelineConfiguration();
                    pipe.setHostLanguage(getHostLanguage());
                    out.setPipelineConfiguration(pipe);
                    c2.setReceiver(out);
                }
            }

            if (out.getSystemId() == null) {
                out.setSystemId(getNewBaseURI(c2));
            }
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            out.startElement(nameCode, typeCode, locationId, properties);

            // output the required namespace nodes via a callback

            outputNamespaceNodes(c2, out);

            // process subordinate instructions to generate attributes and content
            content.process(c2);

            // output the element end tag (which will fail if validation fails)
            out.endElement();
            return null;

        } catch (DynamicError e) {
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator()==null) {
                e.setLocator(this);
            }
            throw e;
        }
    }

   /**
     * Evaluate the constructor, returning the constructed element node. If lazy construction
     * mode is in effect, then an UnconstructedParent object is returned instead.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
       if (isLazyConstruction()) {
           UnconstructedElement e = new UnconstructedElement(this, context);
           // The name code is evaluated eagerly. It's usually already known, and it's usually needed.
           // Evaluating it now removes problems with error handling.
           e.setNameCode(getNameCode(context));
           return e;
       } else {
           return constructElement(context);
       }
    }

    /**
     * Construct the element node as a free-standing (parentless) node in a tiny tree
     * @param context
     * @return the constructed element node
     * @throws XPathException
     */
    private NodeInfo constructElement(XPathContext context) throws XPathException {
        try {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceOutputter seq = controller.allocateSequenceOutputter(1);
            final PipelineConfiguration pipe = controller.makePipelineConfiguration();
            pipe.setHostLanguage(getHostLanguage());
            seq.setPipelineConfiguration(pipe);

            int nameCode = getNameCode(c2);
            int typeCode = (validation == Validation.PRESERVE ? StandardNames.XS_ANY_TYPE : StandardNames.XS_UNTYPED);

            SequenceReceiver ini = seq;
            if (validating) {
                Receiver validator = controller.getConfiguration().getElementValidator(
                        ini, nameCode, locationId,
                        getSchemaType(), validation);

                if (ini.getSystemId() == null) {
                    ini.setSystemId(getNewBaseURI(c2));
                }
                if (validator == ini) {
                    c2.setTemporaryReceiver(ini);
                } else {
                    TreeReceiver tr = new TreeReceiver(validator);
                    tr.setPipelineConfiguration(seq.getPipelineConfiguration());
                    c2.setReceiver(tr);
                    ini = tr;
                }
            } else {
                c2.setTemporaryReceiver(ini);
                if (ini.getSystemId() == null) {
                    ini.setSystemId(getNewBaseURI(c2));
                }
            }

            ini.open();
            int properties = (inheritNamespaces ? 0 : ReceiverOptions.DISINHERIT_NAMESPACES);
            ini.startElement(nameCode, typeCode, locationId, properties);

            // output the namespace nodes for the new element
            outputNamespaceNodes(c2, ini);

            content.process(c2);

            ini.endElement();
            ini.close();

            // the constructed element is the first and only item in the sequence
            NodeInfo result = (NodeInfo)seq.popLastItem();
            seq.reset();
            return result;

        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException)err).setSourceLocator(this);
                ((ValidationException)err).setSystemId(getSystemId());
            }
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            if (err instanceof DynamicError && ((DynamicError)err).getXPathContext() == null) {
                ((DynamicError)err).setXPathContext(context);
            }
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
