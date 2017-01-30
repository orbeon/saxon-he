////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.stream.adjunct.SimpleNodeConstructorAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.String_1;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Cardinality;

/**
 * Common superclass for XSLT instructions whose content template produces a text
 * value: xsl:attribute, xsl:comment, xsl:processing-instruction, xsl:namespace,
 * and xsl:text, and their XQuery equivalents
 */

public abstract class SimpleNodeConstructor extends Instruction {

    protected Operand selectOp;

    /**
     * Default constructor used by subclasses
     */

    public SimpleNodeConstructor() {
        Expression select = Literal.makeEmptySequence();
        selectOp = new Operand(this, select, OperandRole.SINGLE_ATOMIC);
    }

    /**
     * Set the select expression: the value of this expression determines the string-value of the node
     *
     * @param select the expression that computes the string value of the node
     *
     */

    public void setSelect(Expression select) {
        selectOp.setChildExpression(select);
    }

    /**
     * Get the select expression, that is the expression that computes the content of the constructed node.
     * Note that this may correspond either to a select attribute or a contained sequence constructor
     * in the case of XSLT; and in XQuery it corresponds to whatever expression is contained in the node
     * constructpr.
     * @return the expression used to compute the content of the node.
     */

    public Expression getSelect() {
        return selectOp.getChildExpression();
    }

    @Override
    public Iterable<Operand> operands() {
        return selectOp;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     *
     * @return the static cardinality
     */

    public int computeCardinality() {
        return getSelect().getCardinality(); // may allow empty sequence
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
     * Method to perform type-checking specific to the kind of instruction
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of the context item
     * @throws XPathException if a type error is detected
     */

    public abstract void localTypeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException;

    /**
     * The typeCheck() method is called in XQuery, where node constructors
     * are implemented as Expressions. In this case the required type for the
     * select expression is a single string.
     *
     *
       param visitor an expression visitor
     * @param  contextInfo  information about the dynamic context
     * @return the rewritten expression
     * @throws XPathException if any static errors are found in this expression
     *                        or any of its children
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        localTypeCheck(visitor, contextInfo);

        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        if (getSelect() instanceof ValueOf) {
            Expression valSelect = ((ValueOf) getSelect()).getSelect();
            if (th.isSubType(valSelect.getItemType(), BuiltInAtomicType.STRING) &&
                    !Cardinality.allowsMany(valSelect.getCardinality())) {
                setSelect(valSelect);
            }
        }

        // Don't bother converting untypedAtomic to string
        if (getSelect().isCallOn(String_1.class)) {
            SystemFunctionCall fn = (SystemFunctionCall) getSelect();
            Expression arg = fn.getArg(0);
            if (arg.getItemType() == BuiltInAtomicType.UNTYPED_ATOMIC && !Cardinality.allowsMany(arg.getCardinality())) {
                setSelect(arg);
            }
        } else if (getSelect() instanceof CastExpression && ((CastExpression) getSelect()).getTargetType() == BuiltInAtomicType.STRING) {
            Expression arg = ((CastExpression) getSelect()).getBaseExpression();
            if (arg.getItemType() == BuiltInAtomicType.UNTYPED_ATOMIC && !Cardinality.allowsMany(arg.getCardinality())) {
                setSelect(arg);
            }
        }
        adoptChildExpression(getSelect());
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        optimizeChildren(visitor, contextItemType);
        if (getSelect().isCallOn(String_1.class)) {
            SystemFunctionCall sf = (SystemFunctionCall) getSelect();
            TypeHierarchy th = getConfiguration().getTypeHierarchy();
            if (th.isSubType(sf.getArg(0).getItemType(), BuiltInAtomicType.STRING) &&
                    !Cardinality.allowsMany(sf.getArg(0).getCardinality())) {
                setSelect(sf.getArg(0));
            }
        }
        return this;
    }

    //#ifdefined STREAM


    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public SimpleNodeConstructorAdjunct getStreamingAdjunct() {
        return new SimpleNodeConstructorAdjunct();
    }
//#endif

    /**
     * Process this instruction
     *
     * @param context the dynamic context of the transformation
     * @return a TailCall to be executed by the caller, always null for this instruction
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        CharSequence value;
        if (getHostLanguage() == Configuration.XSLT && getPackageData().getXPathVersion() < 30) {
            int savedOutputState = context.getTemporaryOutputState();
            context.setTemporaryOutputState(getInstructionNameCode());
            value = getSelect().evaluateAsString(context);
            context.setTemporaryOutputState(savedOutputState);
        } else {
            value = getSelect().evaluateAsString(context);
        }
        try {
            processValue(value, context);
        } catch (XPathException e) {
            e.maybeSetLocation(getLocation());
            throw e;
        }
        return null;
    }


    /**
     * Process the value of the node, to create the new node.
     *
     * @param value   the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException if a dynamic error occurs
     */

    public abstract void processValue(CharSequence value, XPathContext context) throws XPathException;

    /**
     * Evaluate as an expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item contentItem;

        if (getHostLanguage() == Configuration.XSLT && getRetainedStaticContext().getXPathVersion() < 30) {
            int savedOutputState = context.getTemporaryOutputState();
            context.setTemporaryOutputState(getInstructionNameCode());
            contentItem = getSelect().evaluateItem(context);
            context.setTemporaryOutputState(savedOutputState);
        } else {
            contentItem = getSelect().evaluateItem(context);
        }
        String content;
        if (contentItem == null) {
            content = "";
        } else {
            content = contentItem.getStringValue();
            content = checkContent(content, context);
        }
        Orphan o = new Orphan(context.getConfiguration());
        o.setNodeKind((short) getItemType().getPrimitiveType());
        o.setStringValue(content);
        o.setNodeName(evaluateNodeName(context));
        return o;
    }

    /**
     * Check the content of the node, and adjust it if necessary. The checks depend on the node kind.
     *
     * @param data    the supplied content
     * @param context the dynamic context
     * @return the original content, unless adjustments are needed
     * @throws XPathException if the content is invalid
     */

    protected String checkContent(String data, XPathContext context) throws XPathException {
        return data;
    }

    /**
     * Run-time method to compute the name of the node being constructed. This is overridden
     * for nodes that have a name. The default implementation returns -1, which is suitable for
     * unnamed nodes such as comments
     *
     * @param context the XPath dynamic evaluation context
     * @return the name pool nameCode identifying the name of the constructed node
     * @throws XPathException if any failure occurs
     */

    public NodeName evaluateNodeName(XPathContext context) throws XPathException {
        return null;
    }

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(evaluateItem(context));
    }

    /**
     * Offer promotion for subexpressions. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws XPathException if any error is detected
     */

    protected void promoteChildren(PromotionOffer offer) throws XPathException {
        setSelect(doPromotion(getSelect(), offer));
        super.promoteChildren(offer);
    }


    public boolean isLocal() {
        return ExpressionTool.isLocalConstructor(this);
    }


}

