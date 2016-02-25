////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;

/**
 * Information about the requirements placed by a query or stylesheet on the global
 * context item: whether it is mandatory or optional, what its type must be, whether
 * it is streamable, whether it has a default value.
 */
public class GlobalContextRequirement {

    public boolean mayBeOmitted = true;
    public boolean mayBeSupplied = true;
    public ItemType requiredItemType = AnyItemType.getInstance();
    public boolean isDeclaredStreamable = false;
    private Expression defaultValue = null;  // Used in XQuery only

    public Expression getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Expression defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void export(ExpressionPresenter out) {
        out.startElement("glob");
        String use;
        if (mayBeOmitted) {
            if (mayBeSupplied) {
                use = "opt";
            } else {
                use = "pro";
            }
        } else {
            use = "req";
        }
        out.emitAttribute("use", use);
        out.emitAttribute("streamable", isDeclaredStreamable ? "1" : "0");
        if (!requiredItemType.equals(AnyItemType.getInstance())) {
            out.emitAttribute("type", requiredItemType.toString());
        }
        exportAccumulatorUsages(out);
        out.endElement();
    }

    protected void exportAccumulatorUsages(ExpressionPresenter out) {
    }
}

// Copyright (c) 2015 Saxonica Limited
