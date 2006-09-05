package net.sf.saxon.tree;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.ExtendedNodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;

/**
  * A node in the XML parse tree representing an attribute. Note that this is
  * generated only "on demand", when the attribute is selected by a select pattern.<P>
  * @author Michael H. Kay
  */

final class AttributeImpl extends NodeImpl implements ExtendedNodeInfo {

    private int nameCode;
    private int typeCode;
    private String value;

    /**
    * Construct an Attribute node for the n'th attribute of a given element
    * @param element The element containing the relevant attribute
    * @param index The index position of the attribute starting at zero
    */

    public AttributeImpl(ElementImpl element, int index) {
        parent = element;
        this.index = index;
        AttributeCollection atts = element.getAttributeList();
        this.nameCode = atts.getNameCode(index);
        this.value = atts.getValue(index);
        this.typeCode = atts.getTypeAnnotation(index);
    }

	/**
	* Get the name code, which enables the name to be located in the name pool
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
     * Get the type annotation of this node, if any
     */

    public int getTypeAnnotation() {
        return typeCode;
    }

    /**
     * Determine whether this node has the is-id property
     * @return true if the node is an ID
     */

    public boolean isId() {
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        return th.isIdCode(typeCode);
    }

    /**
     * Determine whether this node has the is-idref property
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        return th.isIdrefsCode(typeCode);
    }

    /**
     * Determine whether the node has the is-nilled property
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return false;  
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        if (!(other instanceof AttributeImpl)) return false;
        if (this==other) return true;
        AttributeImpl otherAtt = (AttributeImpl)other;
        return (parent.isSameNodeInfo(otherAtt.parent) &&
        		 ((nameCode&0xfffff)==(otherAtt.nameCode&0xfffff)));
    }

     /**
      * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
      * (represent the same node) then they must have the same hashCode()
      * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
      * should therefore be aware that third party implementations of the NodeInfo interface may
      * not implement the correct semantics.
      */

     public int hashCode() {
         return parent.hashCode() ^ getFingerprint();
     }

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected long getSequenceNumber() {
        return parent.getSequenceNumber() + 0x8000 + index;
        // note the 0x8000 is to leave room for namespace nodes
    }

    /**
    * Return the type of node.
    * @return Node.ATTRIBUTE
    */

    public final int getNodeKind() {
        return Type.ATTRIBUTE;
    }

    /**
    * Return the character value of the node.
    * @return the attribute value
    */

    public String getStringValue() {
        return value;
    }

    /**
    * Get next sibling - not defined for attributes
    */

    public NodeInfo getNextSibling() {
        return null;
    }

    /**
    * Get previous sibling - not defined for attributes
    */

    public NodeInfo getPreviousSibling() {
        return null;
    }

    /**
    * Get the previous node in document order (skipping attributes)
    */

    public NodeImpl getPreviousInDocument() {
        return (NodeImpl)getParent();
    }

    /**
    * Get the next node in document order (skipping attributes)
    */

    public NodeImpl getNextInDocument(NodeImpl anchor) {
        if (anchor==this) return null;
        return ((NodeImpl)getParent()).getNextInDocument(anchor);
    }

    /**
    * Get sequential key. Returns key of owning element with the attribute index as a suffix
     * @param buffer
     */

    public void generateId(FastStringBuffer buffer) {
        getParent().generateId(buffer);
        buffer.append('a');
        buffer.append(Integer.toString(index));
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
		int nameCode = getNameCode();
    	int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        out.attribute(nameCode, typeCode, getStringValue(), locationId, 0);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
