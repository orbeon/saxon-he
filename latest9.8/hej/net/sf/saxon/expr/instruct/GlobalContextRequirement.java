////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about the requirements placed by a query or stylesheet on the global
 * context item: whether it is mandatory or optional, what its type must be, whether
 * it is streamable, whether it has a default value.
 *
 * In XSLT, if more than one module specifies a global context item type, they must be the same.
 * In XQuery, several modules can specify different required types, and the actual context item
 * must satisfy them all.
 */
public class GlobalContextRequirement {

    private boolean mayBeOmitted = true;
    private boolean absentFocus;
    private List<ItemType> requiredItemTypes = new ArrayList<ItemType>();
    private Expression defaultValue = null;  // Used in XQuery only
    private boolean external; // Used in Xquery only

    public ItemType getRequiredItemType() {
        if (requiredItemTypes.isEmpty()) {
            return AnyItemType.getInstance();
        } else {
            return requiredItemTypes.get(0);
        }
    }

    public List<ItemType> getRequiredItemTypes() {
        return requiredItemTypes;
    }

    public void addRequiredItemType(ItemType requiredItemType) {
        requiredItemTypes.add(requiredItemType);
    }



    public Expression getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Expression defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("glob");
        String use;
        if (isMayBeOmitted()) {
            if (isAbsentFocus()) {
                use = "pro";
            } else {
                use = "opt";
            }
        } else {
            use = "req";
        }
        out.emitAttribute("use", use);
        if (!getRequiredItemType().equals(AnyItemType.getInstance())) {
            out.emitAttribute("type", getRequiredItemType().toExportString());
        }
        if ("JS".equals(out.getOption("target"))) {
            int targetVersion = out.getIntOption("targetVersion", 1);
            out.emitAttribute("jsTest", getRequiredItemType().generateJavaScriptItemTypeTest(
                    AnyItemType.getInstance(), targetVersion));

        }
        exportAccumulatorUsages(out);
        out.endElement();
    }

    protected void exportAccumulatorUsages(ExpressionPresenter out) {
    }

    public void setAbsentFocus(boolean absent) {
        this.absentFocus = absent;
    }

    public boolean isAbsentFocus() {
        return absentFocus;
    }

    public void setMayBeOmitted(boolean mayOmit) {
        this.mayBeOmitted = mayOmit;
    }

    public boolean isMayBeOmitted() {
        return mayBeOmitted;
    }


    /**
     * Say whether (in XQuery) the global context item is declared as external
     *
     * @param external true if the global context item is declared as external
     */

    public void setExternal(boolean external) {
        this.external = external;
    }

    /**
     * Ask whether (in XQuery) the global context item is declared as external
     *
     * @return true if the global context item is declared as external
     */

    public boolean isExternal() {
        return external;
    }
}

// Copyright (c) 2017 Saxonica Limited
