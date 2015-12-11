////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.value.SequenceType;

/**
 * Information about a sub-expression and its relationship to the parent expression
 */

public class Operand {

    public Expression expression;
    private OperandRole role;

    /**
     * Create an operand object
     * @param expression the actual expression used as the operand
     * @param role information about the role this operand plays within the parent expression
     */

    public Operand(Expression expression, OperandRole role) {
        this.expression = expression;
        this.role = role;
    }

    /**
     * Get the expression used as the actual operand
     *
     * @return the child expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Ask whether the child expression sets a new focus for evaluation of other operands
     *
     * @return true if the child expression is evaluated with the same focus as its parent expression
     */
    public boolean setsNewFocus() {
        return role.setsNewFocus();
    }

    /**
     * Ask whether the child expression is evaluated with the same focus as its parent expression
     *
     * @return true if the child expression is evaluated with the same focus as its parent expression
     */
    public boolean hasSameFocus() {
        return role.hasSameFocus();
    }

    /**
     * Ask whether the operand is a higher-order operand, in the sense that the child expression
     * is evaluated repeatedly during a single evaluation of the parent expression
     *
     * @return true if the operand is higher-order
     */
    public boolean isEvaluatedRepeatedly() {
        return role.isEvaluatedRepeatedly();
    }

    /**
     * Get the usage of the operand
     *
     * @return the usage
     */
    public OperandUsage getUsage() {
        return role.getUsage();
    }

    /**
     * Get the required type of the operand
     *
     * @return the required type
     */
    public SequenceType getRequiredType() {
        return role.getRequiredType();
    }

    /**
     * Ask whether the operand is a member of a choice group
     * @return true if the operand is in a choice group
     */

    public boolean isInChoiceGroup() {
        return role.isInChoiceGroup();
    }

}

