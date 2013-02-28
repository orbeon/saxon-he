package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Value;

import javax.xml.transform.SourceLocator;

/**
 * A mapping function for use in conjunction with an {@link ItemMappingIterator} that checks that
 * all the items in a sequence are instances of a given item type
 * @param <T> Defines a subtype of item to be checked
 */
public class ItemTypeCheckingFunction<T extends Item> implements ItemMappingFunction<T, T> {

    private XPathContext externalContext;
    private ItemType requiredItemType;
    private SourceLocator locator;
    private RoleLocator role;

    /**
     * Create the type-checking function
     * @param requiredItemType the item type that all items in the sequence must conform to
     * @param role  information for error messages
     * @param context  the external evaluation context
     */

    public ItemTypeCheckingFunction(ItemType requiredItemType, RoleLocator role, SourceLocator locator, XPathContext context) {
        this.externalContext = context;
        this.requiredItemType = requiredItemType;
        this.role = role;
        this.locator = locator;
    }

    public T mapItem(T item) throws XPathException {
        testConformance(item, externalContext);
        return item;
    }

    private void testConformance(T item, XPathContext context) throws XPathException {
        if (!requiredItemType.matchesItem(item, true, (context == null ? null : context.getConfiguration()))) {
            String message;
            if (context == null) {
                // no name pool available
                message = "Supplied value of type " + Type.displayTypeName(item) +
                        " does not match the required type of " + role.getMessage();
            } else {
                final NamePool pool = context.getNamePool();
                final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
                message = role.composeErrorMessage(requiredItemType, Value.asValue(item).getItemType(th), pool);
            }
            String errorCode = role.getErrorCode();
            if ("XPDY0050".equals(errorCode)) {
                // error in "treat as" assertion
                XPathException te = new XPathException(message, errorCode, context);
                te.setLocator(locator);
                te.setIsTypeError(false);
                throw te;
            } else {
                XPathException te = new XPathException(message, errorCode, context);
                te.setLocator(locator);
                te.setIsTypeError(true);
                throw te;
            }
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//