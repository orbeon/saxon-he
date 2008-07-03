package net.sf.saxon.s9api;

import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.om.Item;

/**
 * An item type, as defined in the XPath/XQuery specifications.
 *
 * <p>This class contains a number of static properties
 * to obtain instances representing simple item types, such as
 * <code>item()</code>, <code>node()</code>, and <code>xs:anyAtomicType</code>.</p>
 *
 * <p>More complicated item types, especially those that are dependent on information in a schema,
 * are available using factory methods on the {@link ItemTypeFactory} object.</p>
 */

public class ItemType {

    /**
     * ItemType representing the type item(), that is, any item at all
     */

    public static ItemType ANY_ITEM = new ItemType(AnyItemType.getInstance(), null);

    /**
     * ItemType representing the type node(), that is, any node
     */

    public static ItemType ANY_NODE = new ItemType(AnyNodeTest.getInstance(), null);

    /**
     * ItemType representing the type xs:anyAtomicType, that is, any atomic value
     */

    public static ItemType ANY_ATOMIC_VALUE = new ItemType(BuiltInAtomicType.ANY_ATOMIC, null);

    private net.sf.saxon.type.ItemType underlyingType;
    private Processor processor;

    protected ItemType(net.sf.saxon.type.ItemType underlyingType, Processor processor) {
        this.processor = processor;
        this.underlyingType = underlyingType;
    }

    /**
     * Determine whether this item type matches a given item.
     *
     * @param item the item to be tested against this item type
     * @return true if the item matches this item type, false if it does not match.
     */

    public boolean matches(XdmItem item) {
        return underlyingType.matchesItem(
                (Item)item.getUnderlyingValue(), false, processor.getUnderlyingConfiguration());
    }

    /**
     * Determine whether this ItemType subsumes another ItemType. Specifically,
     * <code>A.subsumes(B) is true if every value that matches the ItemType B also matches
     * the ItemType A.
     * @param other the other ItemType
     * @return true if this ItemType subsumes the other ItemType. This includes the case where A and B
     * represent the same ItemType.
     * @since 9.1
     */

    public boolean subsumes(ItemType other) {
        TypeHierarchy th = processor.getUnderlyingConfiguration().getTypeHierarchy();
        return th.isSubType(other.getUnderlyingItemType(), underlyingType);
    }

    /**
     * Method to get the underlying Saxon implementation object
     *
     * <p>This gives access to Saxon methods that may change from one release to another.</p>
     * @return the underlying Saxon implementation object
     */

    public net.sf.saxon.type.ItemType getUnderlyingItemType() {
        return underlyingType;
    }

    /**
     * Get the underlying Processor
     * @return the processor used to create this ItemType, if any
     */

    protected Processor getProcessor() {
        return processor;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

