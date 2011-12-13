package net.sf.saxon.s9api;

/**
 * A SequenceType is the combination of an ItemType and an OccurrenceIndicator
 */
public class SequenceType {

    private ItemType itemType;
    private OccurrenceIndicator occurrenceIndicator;

    /**
     * Construct a SequenceType
     * @param itemType the ItemType
     * @param occurrenceIndicator the permitted number of occurrences of the item in the sequence
     */

    private SequenceType(ItemType itemType, OccurrenceIndicator occurrenceIndicator) {
        this.itemType = itemType;
        this.occurrenceIndicator = occurrenceIndicator;
    }

    /**
     * Factory method to construct a SequenceType
     * @param itemType the ItemType
     * @param occurrenceIndicator the permitted number of occurrences of the item in the sequence
     * @return the constricted SequenceType
     */

     /*@NotNull*/ public static SequenceType makeSequenceType(ItemType itemType, OccurrenceIndicator occurrenceIndicator) {
        return new SequenceType(itemType, occurrenceIndicator);
     }

    /**
     * Get the item type
     * @return the item type
     */

    public ItemType getItemType() {
        return itemType;
    }

    /**
     * Get the occurrence indicator
     * @return the occurrence indicator
     */

    public OccurrenceIndicator getOccurrenceIndicator() {
        return occurrenceIndicator;
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