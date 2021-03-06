package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Whitespace;

import java.util.Iterator;


/**
 * An xsl:copy-of element in the stylesheet.
 */

public class CopyOf extends Instruction {

    private Expression select;
    private boolean copyNamespaces;
    private int validation;
    private SchemaType schemaType;
    private boolean requireDocumentOrElement = false;
    private boolean rejectDuplicateAttributes;
    private boolean readOnce = false;
    private boolean validating;
    private boolean copyLineNumbers = true;

    /**
     * Create an xsl:copy-of instruction (also used in XQuery for implicit copying)
     * @param select expression that selects the nodes to be copied
     * @param copyNamespaces true if namespaces are to be copied
     * @param validation validation mode for the result tree
     * @param schemaType schema type for validating the result tree
     * @param rejectDuplicatAttributes true if duplicat attributes are to be rejected (XQuery). False
     * if duplicates are handled by discarding all but the first (XSLT).
     */

    public CopyOf(Expression select,
                  boolean copyNamespaces,
                  int validation,
                  SchemaType schemaType,
                  boolean rejectDuplicatAttributes) {
        this.select = select;
        this.copyNamespaces = copyNamespaces;
        this.validation = validation;
        this.schemaType = schemaType;
        validating = schemaType != null || validation != Validation.PRESERVE;
        rejectDuplicateAttributes = rejectDuplicatAttributes;
        adoptChildExpression(select);
    }

    /**
     * Get the expression that selects the nodes to be copied
     * @return the select expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Get the validation mode
     * @return the validation mode
     */

    public int getValidationMode() {
        return validation;
    }

    /**
     * Set the "saxon:read-once" optimization mode
     * @param b true to enable the optimization
     */

    public void setReadOnce(boolean b) {
        readOnce = b;
    }

    /**
     * Set whether line numbers are to be copied from the source to the result.
     * Default is false.
     * @param copy true if line numbers are to be copied
     */

    public void setCopyLineNumbers(boolean copy) {
        copyLineNumbers = copy;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * The result depends on the type of the select expression.
     */

    public final boolean createsNewNodes() {
        Executable exec = getExecutable();
        if (exec == null) {
            return true;    // This shouldn't happen, but we err on the safe side
        }
        final TypeHierarchy th = exec.getConfiguration().getTypeHierarchy();
        return !select.getItemType(th).isAtomicType();
    }

    /**
     * Get the name of this instruction, for diagnostics and tracing
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COPY_OF;
    }

    /**
     * For XQuery, the operand (select) must be a single element or document node.
     * @param requireDocumentOrElement true if the argument must be a single element or document node
     */
    public void setRequireDocumentOrElement(boolean requireDocumentOrElement) {
        this.requireDocumentOrElement = requireDocumentOrElement;
    }

    /**
     * Test whether this expression requires a document or element node
     * @return true if this expression requires the value of the argument to be a document or element node,
     * false if there is no such requirement
     */

    public boolean isDocumentOrElementRequired() {
        return requireDocumentOrElement;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
     * Determine whether namespaces are to be copied or not
     * @return true if namespaces are to be copied (the default)
     */

    public boolean isCopyNamespaces() {
        return copyNamespaces;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        CopyOf c = new CopyOf(select.copy(), copyNamespaces, validation, schemaType, rejectDuplicateAttributes);
        c.setContainer(getContainer());
            // we don't normally setContainer() in the copy() method, but it's needed here because of the
            // call on getContainer() in computeSpecialProperties()
        return c;
    }


    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        return this;
    }

    public ItemType getItemType(TypeHierarchy th) {
        if (schemaType != null) {
            Configuration config = getExecutable().getConfiguration();
            ItemType in = select.getItemType(th);
            int e = th.relationship(in, NodeKindTest.ELEMENT);
            if (e == TypeHierarchy.SAME_TYPE || e == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ELEMENT, schemaType, config);
            }
            int a = th.relationship(in, NodeKindTest.ATTRIBUTE);
            if (a == TypeHierarchy.SAME_TYPE || a == TypeHierarchy.SUBSUMED_BY) {
                return new ContentTypeTest(Type.ATTRIBUTE, schemaType, config);
            }
        }
        return select.getItemType(th);
    }

    public int getCardinality() {
        return select.getCardinality();
    }

    public int getDependencies() {
        return select.getDependencies();
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.typeCheck(select, contextItemType);
        if (isDocumentOrElementRequired()) {
            TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
            ItemType t = select.getItemType(th);
            if (th.isSubType(t, NodeKindTest.ATTRIBUTE)) {
                throw new XPathException("validate{} expression cannot be applied to an attribute", "XQTY0030");
            }
            if (th.isSubType(t, NodeKindTest.TEXT)) {
                throw new XPathException("validate{} expression cannot be applied to a text node", "XQTY0030");
            }
            if (th.isSubType(t, NodeKindTest.COMMENT)) {
                throw new XPathException("validate{} expression cannot be applied to a comment node", "XQTY0030");
            }
            if (th.isSubType(t, NodeKindTest.PROCESSING_INSTRUCTION)) {
                throw new XPathException("validate{} expression cannot be applied to a processing instruction node", "XQTY0030");
            }
            if (th.isSubType(t, NodeKindTest.NAMESPACE)) {
                throw new XPathException("validate{} expression cannot be applied to a namespace node", "XQTY0030");
            }
        }
        adoptChildExpression(select);
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (readOnce) {
            Expression optcopy = visitor.getConfiguration().getOptimizer().optimizeCopy(select);
            if (optcopy != null) {
                return optcopy;
            }
        }
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (select.getItemType(th).isAtomicType()) {
            return select;
        }
        return this;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("copyOf");
        out.emitAttribute("validation", Validation.toString(validation));
        select.explain(out);
        out.endElement();
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(select);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
                return found;
    }

    /**
     * Process this xsl:copy-of instruction
     *
     * @param context the dynamic context for the transformation
     * @return null - this implementation of the method never returns a TailCall
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();
        boolean copyBaseURI = (out.getSystemId() == null);

        int whichNamespaces = (copyNamespaces ? NodeInfo.ALL_NAMESPACES : NodeInfo.NO_NAMESPACES);

        SequenceIterator iter = select.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                NodeInfo source = (NodeInfo) item;
                int kind = source.getNodeKind();
                if (requireDocumentOrElement &&
                        !(kind == Type.ELEMENT || kind == Type.DOCUMENT)) {
                    XPathException e = new XPathException("Operand of validate expression must be a document or element node");
                    e.setXPathContext(context);
                    e.setErrorCode("XQTY0030");
                    throw e;
                }
                switch (kind) {

                    case Type.ELEMENT: {
                        Receiver eval = out;
                        if (validating) {
                            eval = controller.getConfiguration().getElementValidator(out, source.getNameCode(),
                                    locationId, schemaType, validation);
                        }
                        if (copyBaseURI) {
                            eval.setSystemId(source.getBaseURI());
                        }
                        Receiver savedReceiver = null;
                        PipelineConfiguration savedPipe = null;
                        if (copyLineNumbers) {
                            savedReceiver = eval;
                            savedPipe = new PipelineConfiguration(eval.getPipelineConfiguration());
                            LocationCopier copier = new LocationCopier(eval);
                            eval.getPipelineConfiguration().setLocationProvider(copier);
                            eval = copier;
                        }
                        try {
                            source.copy(eval, whichNamespaces, true, locationId);
                        } catch (CopyNamespaceSensitiveException e) {
                            e.setErrorCode((getHostLanguage() == Configuration.XSLT ? "XTTE0950" : "XQTY0086"));
                            throw e;
                        }
                        if (copyLineNumbers) {
                            eval = savedReceiver;
                            eval.setPipelineConfiguration(savedPipe);
                        }
                        break;
                    }
                    case Type.ATTRIBUTE:
                        try {
                            copyAttribute(source, schemaType, validation, this, context, rejectDuplicateAttributes);
                        } catch (NoOpenStartTagException err) {
                            XPathException e = new XPathException(err.getMessage());
                            e.setLocator(this);
                            e.setXPathContext(context);
                            e.setErrorCode(err.getErrorCodeLocalPart());
                            throw dynamicError(this, e, context);
                        }
                        break;
                    case Type.TEXT:
                        out.characters(source.getStringValueCS(), locationId, 0);
                        break;

                    case Type.PROCESSING_INSTRUCTION:
                        if (copyBaseURI) {
                            out.setSystemId(source.getBaseURI());
                        }
                        out.processingInstruction(source.getDisplayName(), source.getStringValueCS(), locationId, 0);
                        break;

                    case Type.COMMENT:
                        out.comment(source.getStringValueCS(), locationId, 0);
                        break;

                    case Type.NAMESPACE:
                        try {
                            source.copy(out, NodeInfo.NO_NAMESPACES, false, locationId);
                        } catch (NoOpenStartTagException err) {
                            XPathException e = new XPathException(err.getMessage());
                            e.setXPathContext(context);
                            e.setErrorCode(err.getErrorCodeLocalPart());
                            //context.getController().recoverableError(e);
                            throw dynamicError(this, e, context);
                        }
                        break;

                    case Type.DOCUMENT: {
                        Receiver val = controller.getConfiguration().
                                getDocumentValidator(out,
                                        source.getBaseURI(),
                                        validation, Whitespace.NONE, schemaType, -1);
                        val.setPipelineConfiguration(out.getPipelineConfiguration());
                        if (copyBaseURI) {
                            val.setSystemId(source.getBaseURI());
                        }
                        Receiver savedReceiver = null;
                        PipelineConfiguration savedPipe = null;
                        if (copyLineNumbers) {
                            savedReceiver = val;
                            savedPipe = new PipelineConfiguration(val.getPipelineConfiguration());
                            LocationCopier copier = new LocationCopier(val);
                            val.getPipelineConfiguration().setLocationProvider(copier);
                            val = copier;
                        }
                        try {
                            source.copy(val, whichNamespaces, true, locationId);
                        } catch (CopyNamespaceSensitiveException e) {
                            e.setErrorCode((getHostLanguage() == Configuration.XSLT ? "XTTE0950" : "XQTY0086"));
                            throw e;
                        }
                        if (copyLineNumbers) {
                            val = savedReceiver;
                            val.setPipelineConfiguration(savedPipe);
                        }
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown node kind " + source.getNodeKind());
                }

            } else {
                out.append(item, locationId, NodeInfo.ALL_NAMESPACES);
            }
        }
        return null;
    }

    /**
     * Method shared by xsl:copy and xsl:copy-of to copy an attribute node
     * @param source            the node to be copied
     * @param schemaType        the simple type against which the value is to be validated, if any
     * @param validation        one of preserve, strip, strict, lax
     * @param instruction       the calling instruction, used for diagnostics
     * @param context           the dynamic context
     * @param rejectDuplicates  true if duplicate attributes with the same name are disallowed (XQuery)
     * @throws XPathException
     */

    static void copyAttribute(NodeInfo source,
                                        SchemaType schemaType,
                                        int validation,
                                        Instruction instruction,
                                        XPathContext context,
                                        boolean rejectDuplicates)
            throws XPathException {
        int nameCode = source.getNameCode();
        int annotation = StandardNames.XS_UNTYPED_ATOMIC;
        int opt = 0;
        if (rejectDuplicates) {
            opt |= ReceiverOptions.REJECT_DUPLICATES;
        }
        CharSequence value = source.getStringValueCS();
        if (schemaType != null) {
            if (schemaType.isSimpleType()) {
                if (((SimpleType) schemaType).isNamespaceSensitive()) {
                    XPathException err = new XPathException("Cannot create a parentless attribute whose " +
                            "type is namespace-sensitive (such as xs:QName)");
                    err.setErrorCode("XTTE1545");
                    err.setXPathContext(context);
                    err.setLocator(instruction);
                    throw err;
                }
                try {
                    ValidationFailure err = ((SimpleType) schemaType).validateContent(
                            value, DummyNamespaceResolver.getInstance(), context.getConfiguration().getNameChecker());
                    if (err != null) {
                        throw new ValidationException("Attribute being copied does not match the required type. " +
                                err.getMessage());
                    }
                    annotation = schemaType.getFingerprint();
                } catch (UnresolvedReferenceException ure) {
                    throw new ValidationException(ure);
                }
            } else {
                XPathException e = new XPathException("Cannot validate an attribute against a complex type");
                e.setXPathContext(context);
                e.setErrorCode("XTSE1530");
                e.setIsStaticError(true);
                throw e;
            }
        } else if (validation == Validation.STRICT || validation == Validation.LAX) {
            try {
                annotation = context.getConfiguration().validateAttribute(nameCode, value, validation);
            } catch (ValidationException e) {
                XPathException err = XPathException.makeXPathException(e);
                err.setErrorCode(e.getErrorCodeLocalPart());
                err.setXPathContext(context);
                err.setLocator(instruction);
                err.setIsTypeError(true);
                throw err;
            }

        } else if (validation == Validation.PRESERVE) {
            annotation = source.getTypeAnnotation() & NamePool.FP_MASK;
            if (annotation != StandardNames.XS_UNTYPED_ATOMIC) {
                SchemaType type = context.getConfiguration().getSchemaType(annotation);
                if (((AtomicType) type).isNamespaceSensitive()) {
                    XPathException err = new XPathException("Cannot preserve type annotation when copying an attribute with namespace-sensitive content");
                    err.setErrorCode("XTTE0950");
                    err.setIsTypeError(true);
                    err.setXPathContext(context);
                    throw err;
                }
            }
        }

        context.getReceiver().attribute(nameCode, annotation, value, instruction.getLocationId(), opt);
    }

    /**
     * Return the first item if there is one, or null if not
     * @param context the XPath dynamic context
     * @return the result of evaluating the instruction
     * @throws XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return super.evaluateItem(context);
    }

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        if (schemaType == null && copyNamespaces) {
            if (validation == Validation.PRESERVE) {
                // create a virtual copy of the underlying nodes
                ItemMappingFunction copier = new ItemMappingFunction() {
                    public Item map(Item item) {
                        if (item instanceof AtomicValue) {
                            return item;
                        }
                        VirtualCopy vc = VirtualCopy.makeVirtualCopy((NodeInfo)item, (NodeInfo) item);
                        int documentNumber =
                                context.getController().getConfiguration().getDocumentNumberAllocator().allocateDocumentNumber();
                        vc.setDocumentNumber(documentNumber);
                        // TODO: here (and below): set Base URI as described in bugzilla 3417. See test baseuri20_044,
                        // and discussion in bug 569 against the test suite.
                        return vc;
                    }
                };
                return new ItemMappingIterator(select.iterate(context), copier);
            } else if (validation == Validation.STRIP) {
                // create a virtual copy of the underlying nodes
                ItemMappingFunction copier = new ItemMappingFunction() {
                    public Item map(Item item) {
                        if (item instanceof AtomicValue) {
                            return item;
                        }
                        VirtualCopy vc = VirtualUntypedCopy.makeVirtualUntypedCopy((NodeInfo) item, (NodeInfo) item);
                        int documentNumber =
                                context.getController().getConfiguration().getDocumentNumberAllocator().allocateDocumentNumber();
                        vc.setDocumentNumber(documentNumber);
                        return vc;
                    }
                };
                return new ItemMappingIterator(select.iterate(context), copier);
            }
        }
        Controller controller = context.getController();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        SequenceOutputter out = new SequenceOutputter();
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setHostLanguage(getHostLanguage());
        out.setPipelineConfiguration(pipe);
        c2.setReceiver(out);
        try {
            process(c2);
            return Value.getIterator(out.getSequence());
        } catch (XPathException err) {
            if (err instanceof ValidationException) {
                ((ValidationException) err).setSourceLocator(this);
                ((ValidationException) err).setSystemId(getSystemId());
            }
            err.maybeSetLocation(this);
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
