package net.sf.saxon.s9api;

import net.sf.saxon.om.Item;
import net.sf.saxon.type.TypeHierarchy;

/**
 * An item type constructed by the ItemTypeFactory (as distinct from one that is predefined)
 *
 * This class is not user-visible.
 */

class ConstructedItemType extends ItemType {

    private net.sf.saxon.type.ItemType underlyingType;
    private Processor processor;

    /**
     * Protected constructor
     * @param underlyingType the Saxon internal item type. Must not be null.
     * @param processor The s9api processor Must not be null
     */

    protected ConstructedItemType(net.sf.saxon.type.ItemType underlyingType, Processor processor) {
        if (processor == null) {
            throw new NullPointerException("processor");
        }
        if (underlyingType == null) {
            throw new NullPointerException("underlyingType");
        }
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
     * @return the processor used to create this ItemType. This will be null if the ItemType is one of the three
     * static constant item types {@link #ANY_ITEM}, {@link #ANY_NODE}, or {@link #ANY_ATOMIC_VALUE}
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