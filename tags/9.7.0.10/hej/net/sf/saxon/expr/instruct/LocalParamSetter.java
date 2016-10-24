////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LocalParamCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;

/**
 * The compiled form of an xsl:param element within a template in an XSLT stylesheet. An xsl:param
 * element used as a template parameter is compiled into two objects: a LocalParam which represents the
 * parameter definition and the Binding for the value, and a LocalParamSetter which is an Instruction
 * to set a default value for the parameter if none has been supplied.
 * <p/>
 * <p>The xsl:param element in XSLT has mandatory attribute name and optional attribute select. It can also
 * be specified as required="yes" or required="no".</p>
 * <p/>
 * <p>This is used only for parameters to XSLT templates. For function calls, the caller of the function
 * places supplied arguments onto the callee's stackframe and the callee does not need to do anything.
 * Global parameters (XQuery external variables) are handled using {@link net.sf.saxon.expr.instruct.GlobalParam}.</p>
 * <p/>
 * <p>The LocalParam class is also used to represent parameters with the saxon:iterate instruction</p>
 */

public final class LocalParamSetter extends Instruction {

    /*@NotNull*/
    private LocalParam binding;

    public LocalParamSetter(/*@NotNull*/ LocalParam binding) {
        this.binding = binding;
        adoptOperands();
    }

    private void adoptOperands() {
        if (binding != null) {
            Expression select = binding.getSelectExpression();
            if (select != null) {
                select.setParentExpression(this);
            }
            Expression conversion = binding.getConversion();
            if (conversion != null) {
                conversion.setParentExpression(this);
            }
        }
    }

    /**
     * Get the LocalParam element representing the binding for this parameter
     *
     * @return the binding element
     */

    /*@NotNull*/
    public LocalParam getBinding() {
        return binding;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     */
    /*@NotNull*/
    @Override
    public ItemType getItemType() {
        return ErrorType.getInstance();
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     *
     * @return the static cardinality
     */
    @Override
    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */
    @Override
    public int computeSpecialProperties() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns a default value of false
     *
     * @return true if the instruction creates new nodes (or if it can't be proved that it doesn't)
     */
    @Override
    public boolean createsNewNodes() {
        return false;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     *
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression
     *          rewriting
     */
    /*@NotNull*/
    @Override
    public Expression simplify() throws XPathException {
        Expression select = binding.getSelectExpression();
        if (select != null) {
            Expression s2 = select.simplify();
            binding.setSelectExpression(s2);
            adoptOperands();
        }
        return this;
    }

    /**
     * Perform type checking of an expression and its subexpressions. This is the second phase of
     * static optimization.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     * <p/>
     * <p>If the implementation returns a value other than "this", then it is required to ensure that
     * the location information in the returned expression have been set up correctly.
     * It should not rely on the caller to do this, although for historical reasons many callers do so.</p>
     *
     *
       param visitor         an expression visitor
     * @param  contextInfo
     * @return the original expression, rewritten to perform necessary run-time type checks,
     *         and to perform other type-related optimizations
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */
    /*@NotNull*/
    @Override
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        Expression select = binding.getSelectExpression();
        if (select != null) {
            binding.setSelectExpression(select.typeCheck(visitor, contextInfo));
        }
        binding.checkAgainstRequiredType(visitor);
        adoptOperands();
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions. This is the third and final
     * phase of static optimization.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */
    /*@NotNull*/
    @Override
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression select = binding.getSelectExpression();
        if (select != null) {
            binding.setSelectExpression(select.optimize(visitor, contextItemType));
            adoptOperands();
            binding.computeEvaluationMode();
        }
        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings if a local variable is encountered whose binding is present in this
     *                   map, then the binding will be replaced with a new binding.
     */
    /*@NotNull*/
    @Override
    public Expression copy(RebindingMap rebindings) {
        LocalParam newBinding = binding.copy(rebindings);
        LocalParamSetter exp = new LocalParamSetter(newBinding);
        rebindings.put(binding, newBinding);
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_PARAM;
    }

    @Override
    public Iterable<Operand> operands() {
        return binding.operands(this);
    }

    @Override
    public void adoptChildExpression(Expression child) {
        // no action (because of the unusual structure of this expression)
    }

    /**
     * Process the local parameter declaration
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        int slotNumber = binding.getSlotNumber();
        int wasSupplied = context.useLocalParameter(binding.getVariableQName(), slotNumber, binding.isTunnelParam());
        switch (wasSupplied) {
            case ParameterSet.SUPPLIED_AND_CHECKED:
                // No action needed
                break;

            case ParameterSet.SUPPLIED:
                // if a parameter was supplied by the caller, with no type-checking by the caller,
                // then we may need to convert it to the type required
                Expression conversion = binding.getConversion();
                int conversionEvaluationMode = binding.getConversionEvaluationMode();
                if (conversion != null) {
                    context.setLocalVariable(slotNumber,
                            ExpressionTool.evaluate(conversion, conversionEvaluationMode, context, 10));
                    // We do an eager evaluation here for safety, because the result of the
                    // type conversion overwrites the slot where the actual supplied parameter
                    // is contained.
                }
                break;

            // don't evaluate the default if a value has been supplied or if it has already been
            // evaluated by virtue of a forwards reference

            case ParameterSet.NOT_SUPPLIED:
                if (binding.isImplicitlyRequiredParam()) {
                    String name = "$" + binding.getVariableQName().getDisplayName();
                    XPathException e = new XPathException("A value must be supplied for parameter "
                            + name + " because " +
                            "the default value is not a valid instance of the required type");
                    e.setXPathContext(context);
                    e.setErrorCode("XTDE0700");
                    throw e;
                } else if (binding.isRequiredParam()) {
                    String name = "$" + binding.getVariableQName().getDisplayName();
                    XPathException e = new XPathException("No value supplied for required parameter " + name);
                    e.setXPathContext(context);
                    e.setErrorCode("XTDE0700");
                    throw e;
                }
                context.setLocalVariable(slotNumber, binding.getSelectValue(context));
        }
        return null;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the LocalParamSetter expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LocalParamCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("param", this);
        out.emitAttribute("name", binding.getVariableQName());
        out.emitAttribute("slot", "" + binding.getSlotNumber());
        String flags = getFlags();
        if (!flags.isEmpty()) {
            out.emitAttribute("flags", flags);
        }
//        if (binding.isTunnelParam()) {
//            out.emitAttribute("tunnel", "1");
//        }
        if (binding.getRequiredType() != SequenceType.ANY_SEQUENCE) {
            out.emitAttribute("as", binding.getRequiredType().toString());
        }
        if (binding.getSelectExpression() != null) {
            out.setChildRole("select");
            binding.getSelectExpression().export(out);
        }
        Expression conversion = binding.getConversion();
        if (conversion != null) {
            out.setChildRole("conversion");
            conversion.export(out);
        }
        out.endElement();
    }

    private String getFlags() {
        String flags = "";
        if (binding.isTunnelParam()) {
            flags += "t";
        }
        if (binding.isRequiredParam()) {
            flags += "r";
        }
        if (binding.isImplicitlyRequiredParam()) {
            flags += "i";
        }
        return flags;
    }

}

