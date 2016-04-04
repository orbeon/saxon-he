////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LocalParamBlockCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;

import java.util.Arrays;


/**
 * Represents the set of xsl:param elements at the start of an xsl:iterate instruction
 */

public class LocalParamBlock extends Instruction {

    Operand[] operanda;

    /**
     * Create the block of parameters
     *
     * @param params the parameters
     */

    public LocalParamBlock(LocalParamSetter[] params) {
        operanda = new Operand[params.length];
        for (int i=0; i<params.length; i++) {
            operanda[i] = new Operand(this, params[i], OperandRole.NAVIGATE);
        }
    }

    public String getExpressionName() {
        return "params";
    }

    @Override
    public Iterable<Operand> operands() {
        return Arrays.asList(operanda);
    }

    public int getNumberOfParams() {
        return operanda.length;
    }

    public int computeSpecialProperties() {
        return 0;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        LocalParamSetter[] c2 = new LocalParamSetter[getNumberOfParams()];
        int i=0;
        for (Operand o : operands()) {
            c2[i++] = (LocalParamSetter)o.getChildExpression().copy();
        }
        return new LocalParamBlock(c2);
    }

    /**
     * Determine the data type of the items returned by this expression
     *
     * @return the data type
     */

    /*@NotNull*/
    public final ItemType getItemType() {
        return ErrorType.getInstance();
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        return StaticProperty.EMPTY;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws net.sf.saxon.trans.XPathException
     *
     */

    protected void promoteChildren(PromotionOffer offer) throws XPathException {
        for (Operand o : operands()) {
            LocalParamSetter p = (LocalParamSetter)o.getChildExpression();
            p.getBinding().setSelectExpression(doPromotion(p.getBinding().getSelectExpression(), offer));
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("params", this);
        for (Operand o : operands()) {
            o.getChildExpression().export(out);
        }
        out.endElement();
    }


    /*@Nullable*/
    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        for (Operand o : operands()) {
            LocalParamSetter param = (LocalParamSetter)o.getChildExpression();
            try {
                context.setLocalVariable(param.getBinding().getSlotNumber(), param.getBinding().getSelectValue(context));
            } catch (XPathException e) {
                e.maybeSetLocation(param.getLocation());
                e.maybeSetContext(context);
                throw e;
            }
        }
        return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return PROCESS_METHOD;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the LocalParamBlock expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LocalParamBlockCompiler();
    }
//#endif

}

