////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Operand;
import net.sf.saxon.expr.OperandRole;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.List;

/**
 * An object derived from a xsl:with-param element in the stylesheet. <br>
 */

public class WithParam  {

    public static WithParam[] EMPTY_ARRAY = new WithParam[0];


    private Expression select;
    //private int parameterId;
    private boolean typeChecked = false;
    private int slotNumber;
    private SequenceType requiredType;
    private StructuredQName variableQName;
    private int evaluationMode = ExpressionTool.UNDECIDED;

    public WithParam() {
    }

    /**
     * Set the expression to which this variable is bound
     *
     * @param select the initializing expression
     */

    public void setSelectExpression(Expression select) {
        this.select = select;
    }

    /**
     * Get the expression to which this variable is bound
     *
     * @return the initializing expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Set the required type of this variable
     *
     * @param required the required type
     */

    public void setRequiredType(SequenceType required) {
        requiredType = required;
    }

    /**
     * Get the required type of this variable
     *
     * @return the required type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }


    /**
     * Allocate a number which is essentially an alias for the parameter name,
     * unique within a stylesheet
     *
     * @param id the parameter id
     */

//    public void setParameterId(int id) {
//        parameterId = id;
//    }

    /**
     * Get the parameter id, which is essentially an alias for the parameter name,
     * unique within a stylesheet
     *
     * @return the parameter id
     */

//    public int getParameterId() {
//        return parameterId;
//    }

    /**
     * Get the slot number allocated to this variable
     *
     * @return the slot number, that is the position allocated to the variable on its stack frame
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Set the slot number of this variable
     *
     * @param s the slot number, that is, the position allocated to this variable on its stack frame
     */

    public void setSlotNumber(int s) {
        slotNumber = s;
    }

    /**
     * Set the name of the variable
     *
     * @param s the name of the variable (a QName)
     */

    public void setVariableQName(StructuredQName s) {
        variableQName = s;
    }

    /**
     * Get the name of this variable
     *
     * @return the name of this variable (a QName)
     */

    public StructuredQName getVariableQName() {
        return variableQName;
    }

    /**
     * Say whether this parameter will have been typechecked by the caller to ensure it satisfies
     * the required type, in which case the callee need not do a dynamic type check
     *
     * @param checked true if the caller has done static type checking against the required type
     */

    public void setTypeChecked(boolean checked) {
        typeChecked = checked;
    }


    public int getInstructionNameCode() {
        return StandardNames.XSL_WITH_PARAM;
    }

    /**
     * Static method to simplify a set of with-param elements
     *
     * @param params  the set of parameters to be simplified
     * @param visitor the expression visitor
     * @throws XPathException if a static error is found
     */

    public static void simplify(WithParam[] params, ExpressionVisitor visitor) throws XPathException {
        if (params != null) {
            for (WithParam param : params) {
                param.select = visitor.simplify(param.select);
            }
        }
    }

    /**
     * Static method to typecheck a set of with-param elements
     *
     * @param params          the set of parameters to be checked
     * @param visitor         the expression visitor
     * @param contextItemType static information about the context item type and existence
     * @throws XPathException if a static error is found
     */


    public static void typeCheck(WithParam[] params, ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        if (params != null) {
            for (WithParam param : params) {
                param.select = visitor.typeCheck(param.select, contextItemType);
            }
        }
    }

    /**
     * Static method to optimize a set of with-param elements
     *
     * @param params          the set of parameters to be optimized
     * @param visitor         the expression visitor
     * @param contextItemType static information about the context item type and existence
     * @throws XPathException if a static error is found
     */

    public static void optimize(ExpressionVisitor visitor, WithParam[] params, ContextItemStaticInfo contextItemType) throws XPathException {
        if (params != null) {
            for (WithParam param : params) {
                param.select = visitor.typeCheck(param.select, contextItemType);
                param.computeEvaluationMode();
            }
        }
    }

    /**
     * Static method to do expression promotion on a set of with-param elements
     *
     * @param parent the parent instruction (for example apply-templates or call-template)
     * @param params the set of parameters to be be investigated for promotion
     * @param offer  the promotion offer to be passed to subexpressions
     * @throws XPathException if a static error is found
     */

    public static void promoteParams(Expression parent, WithParam[] params, PromotionOffer offer) throws XPathException {
        if (params != null) {
            for (WithParam param : params) {
                Expression select = param.select;
                if (select != null) {
                    param.select = select.promote(offer, parent);
                    param.computeEvaluationMode();
                }
            }
        }
    }

    public static void setSlotNumbers(WithParam[] params) {

    }

    /**
     * Get the evaluation mode of the variable
     *
     * @return the evaluation mode (a constant in {@link ExpressionTool}
     */

    public int getEvaluationMode() {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            computeEvaluationMode();
        }
        return evaluationMode;
    }

    private void computeEvaluationMode() {
        evaluationMode = ExpressionTool.lazyEvaluationMode(select);
    }


    /**
     * Static method to copy a set of parameters
     *
     * @param params the parameters to be copied
     * @return the resulting copy
     */

    public static WithParam[] copy(WithParam[] params) {
        if (params == null) {
            return null;
        }
        WithParam[] result = new WithParam[params.length];
        for (int i = 0; i < params.length; i++) {
            result[i] = new WithParam();
            //result[i].parameterId = params[i].parameterId;
            result[i].slotNumber = params[i].slotNumber;
            result[i].typeChecked = params[i].typeChecked;
            result[i].select = params[i].select.copy();
            result[i].requiredType = params[i].requiredType;
            result[i].variableQName = params[i].variableQName;
        }
        return result;
    }

    /**
     * Static method to gather the XPath expressions used in an array of WithParam parameters (add them to the supplied list)
     *
     * @param params the set of with-param elements to be searched
     * @param list   the list to which the subexpressions will be added
     */

    public static void gatherXPathExpressions(WithParam[] params, List<Expression> list) {
        if (params != null) {
            for (WithParam param : params) {
                list.add(param.select);
            }
        }
    }

    /**
     * Static method to gather the XPath expressions used in an array of WithParam parameters (add them to the supplied list)
     *
     * @param params the set of with-param elements to be searched
     * @param list   the list to which the subexpressions will be added
     */

    public static void gatherOperands(WithParam[] params, List<Operand> list) {
        if (params != null) {
            for (WithParam param : params) {
                list.add(new Operand(param.select, OperandRole.NAVIGATE));
            }
        }
    }


    /**
     * Static method to explain a set of parameters
     *
     * @param params the set of parameters to be explained
     * @param out    the destination for the explanation
     */

    public static void explainParameters(WithParam[] params, ExpressionPresenter out) {
        if (params != null) {
            for (WithParam param : params) {
                out.startElement("withParam");
                out.emitAttribute("name", param.variableQName.getDisplayName());
                param.select.explain(out);
                out.endElement();
            }
        }
    }

    /**
     * Static method to replace a subexpression within any parameter within which it is found
     *
     * @param params      the set of parameters to be examined
     * @param original    the subexpression to be replaced
     * @param replacement the replacement expression
     * @return true if a replacement was made
     */

    public static boolean replaceXPathExpression(WithParam[] params, Expression original, Expression replacement) {
        boolean found = false;
        if (params != null) {
            for (WithParam param : params) {
                boolean f = param.replaceSubExpression(original, replacement);
                found |= f;
            }
        }
        return found;
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    private boolean replaceSubExpression(Expression original, Expression replacement) {
        if (select == original) {
            select = replacement;
            return true;
        }
        return false;
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
     *
     * @param context the XPath dynamic context
     * @return the result of evaluating the variable
     * @throws net.sf.saxon.trans.XPathException
     *          if evaluation of the select expression fails
     *          with a dynamic error
     */

    public Sequence getSelectValue(XPathContext context) throws XPathException {
        if (select == null) {
            throw new XPathException("Internal error: No select expression on xsl:with-param");
            // The value of the variable is a sequence of nodes and/or atomic values

        } else {
            // There is a select attribute: do a lazy evaluation of the expression,
            // which will already contain any code to force conversion to the required type.
            int savedOutputState = context.getTemporaryOutputState();
            context.setTemporaryOutputState(StandardNames.XSL_WITH_PARAM);
            Sequence result = ExpressionTool.evaluate(select, evaluationMode, context, 10);
            context.setTemporaryOutputState(savedOutputState);
            return result;
        }
    }


    /**
     * Ask whether static type checking has been done
     *
     * @return true if the caller has done static type checking against the type required by the callee
     */

    public boolean isTypeChecked() {
        return typeChecked;
    }
}

