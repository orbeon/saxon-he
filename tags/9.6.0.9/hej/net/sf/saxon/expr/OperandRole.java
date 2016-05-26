////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;


import net.sf.saxon.value.SequenceType;

/**
 * Defines the role of a child expression relative to its parent expression. The properties
 * of an operand role depend only on the kind of expression and not on the actual arguments
 * supplied to a specific instance of the kind of operand.
 */
public class OperandRole {

    public final static int SETS_NEW_FOCUS = 1;
    public final static int USES_NEW_FOCUS = 2;
    public final static int HIGHER_ORDER = 4;
    public final static int IN_CHOICE_GROUP = 8;

    public final static OperandRole SAME_FOCUS_ACTION =
            new OperandRole(0, OperandUsage.TRANSMISSION, SequenceType.ANY_SEQUENCE);
    public final static OperandRole FOCUS_CONTROLLING_SELECT =
            new OperandRole(OperandRole.SETS_NEW_FOCUS, OperandUsage.INSPECTION, SequenceType.ANY_SEQUENCE);
    public final static OperandRole FOCUS_CONTROLLED_ACTION =
            new OperandRole(OperandRole.USES_NEW_FOCUS | OperandRole.HIGHER_ORDER, OperandUsage.TRANSMISSION, SequenceType.ANY_SEQUENCE);
    public final static OperandRole INSPECT =
            new OperandRole(0, OperandUsage.INSPECTION,  SequenceType.ANY_SEQUENCE);
    public final static OperandRole NAVIGATE =
            new OperandRole(0, OperandUsage.NAVIGATION,  SequenceType.ANY_SEQUENCE);
    public final static OperandRole SINGLE_ATOMIC =
            new OperandRole(0, OperandUsage.ABSORPTION,  SequenceType.SINGLE_ATOMIC);
    public final static OperandRole ATOMIC_SEQUENCE =
            new OperandRole(0, OperandUsage.ABSORPTION,  SequenceType.ATOMIC_SEQUENCE);
    public final static OperandRole NEW_FOCUS_ATOMIC =
            new OperandRole(OperandRole.USES_NEW_FOCUS | OperandRole.HIGHER_ORDER, OperandUsage.ABSORPTION, SequenceType.ATOMIC_SEQUENCE);

    int properties;
    private OperandUsage usage;
    private SequenceType requiredType = SequenceType.ANY_SEQUENCE;

    public OperandRole(int properties, OperandUsage usage) {
        this.properties = properties;
        this.usage = usage;
    }

    public OperandRole(int properties, OperandUsage usage, SequenceType requiredType) {
        this.properties = properties;
        this.usage = usage;
        this.requiredType = requiredType;
    }

    /**
     * Ask whether the child expression sets the focus for evaluation of other child expressions
     * @return true if the child expression is evaluated with the same focus as its parent expression
     */

    public boolean setsNewFocus() {
        return (properties & SETS_NEW_FOCUS) != 0;
    }

    /**
     * Ask whether the child expression is evaluated with the same focus as its parent expression
     * @return true if the child expression is evaluated with the same focus as its parent expression
     */

    public boolean hasSameFocus() {
        return (properties & USES_NEW_FOCUS) == 0;
    }

    /**
     * Ask whether the operand is a higher-order operand, in the sense that the child expression
     * is evaluated repeatedly during a single evaluation of the parent expression
     * @return true if the operand is higher-order
     */

    public boolean isEvaluatedRepeatedly() {
        return (properties & HIGHER_ORDER) != 0;
    }

    /**
     * Get the usage of the operand
     * @return the usage
     */

    public OperandUsage getUsage() {
        return usage;
    }

    /**
     * Get the required type of the operand
     * @return the required type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Ask whether the operand is a member of a choice group
     * @return true if the operand is in a choice group
     */

    public boolean isInChoiceGroup() {
        return (properties & IN_CHOICE_GROUP) != 0;
    }

}

