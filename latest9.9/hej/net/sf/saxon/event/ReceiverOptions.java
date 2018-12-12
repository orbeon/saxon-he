////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;


/**
 * ReceiverOptions defines a set of constants, which can be used in
 * calls to methods on the Receiver interface. The values are
 * bit-significant.
 *
 * @author Michael H. Kay
 */


public class ReceiverOptions {

    /**
     * Flag to disable output escaping
     */

    public static final int DISABLE_ESCAPING = 1;

    /**
     * Flag to disable use of character maps
     */

    public static final int DISABLE_CHARACTER_MAPS = 2;

    /**
     * Flag indicating that the value contains no special characters
     * that need to be escaped
     */

    public static final int NO_SPECIAL_CHARS = 4;

    /**
     * Flag indicating that an attribute value or text node was added by the schema processor
     * because a default value was specified
     */

    public static final int DEFAULTED_ATTRIBUTE = 8;

    /**
     * Flag used with character content that has been validated against a nillable element
     * declaration. Needed because of a peculiar rule for validating xs:key values
     */

    public static final int NILLED_ELEMENT = 0x10;

    /**
     * Flag indicating that duplicate values should be rejected
     */

    public static final int REJECT_DUPLICATES = 0x20;

    /**
     * Flag indicating that the namespace (of an element or attribute name)
     * has already been declared; it does not need to be generated by the namespace
     * fixup process.
     */

    public static final int NAMESPACE_OK = 0x40;

    /**
     * Flag passed on startElement indicating that the element does not pass
     * namespaces on to its children.
     */

    public static final int DISINHERIT_NAMESPACES = 0x80;

    /**
     * Flag used when an attribute value or text node contains null characters
     * before and after strings generated by character mapping; these strings
     * are to be output without escaping
     */

    public static final int USE_NULL_MARKERS = 0x100;

    /**
     * Flag used with character content that has been validated against a nillable element
     * declaration. Needed because of a peculiar rule for validating xs:key values
     */

    public static final int NILLABLE_ELEMENT = 0x200;

    /**
     * Flag used with the characters() event to indicate that the event represents an entire
     * text node, that is, the text node has not been fragmented over several characters() events
     */

    public static final int WHOLE_TEXT_NODE = 0x400;

    /**
     * Flag indicating an element or attribute that has the is-id property
     */

    public static final int IS_ID = 0x800;

    /**
     * Flag indicating an element or attribute that has the is-idref property (indicating that it is an IDREF
     * or IDREFS attribute)
     */

    public static final int IS_IDREF = 0x1000;

    /**
     * Flag indicating that the ID/IDREF properties have been set if applicable: if this bit is set,
     * then the absence of the IS_ID bit means the node is not an ID, and similarly for IS_IDREF
     */

    public static final int ID_IDREF_CHECKED = 0x2000;

    /**
     * Flag set on startDocument() in relation to an xsl:message call with terminate="yes"
     */

    public static final int TERMINATE = 0x4000;

    /**
     * Flag set on startDocument() to indicate that the constructed document must be updateable
     */

    public static final int MUTABLE_TREE = 0x8000;

    /**
     * Flag set on startElement() to indicate that the element does not inherit namespaces
     * from its parent
     */

    public static final int REFUSE_NAMESPACES = 0x10000;

    /**
     * Flag set on startElement() if the element is known to have children
     */

    public static final int HAS_CHILDREN = 0x20000;

    /**
     * Flag set on append() to indicate that namespaces declared (or undeclared) on this element
     * should be copied, but not namespaces inherited from a parent element
     */
    public static final int LOCAL_NAMESPACES = 0x40000;

    /**
     * Flag set on append() to indicate that all in-scope namespaces should be copied
     */
    public static final int ALL_NAMESPACES = 0x80000;

    /**
     * Flag set on attribute() to indicate that there is no need to check for duplicate attributes
     */
    public static final int NOT_A_DUPLICATE = 0x100000;


}

