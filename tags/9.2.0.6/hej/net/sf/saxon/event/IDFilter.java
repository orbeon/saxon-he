package net.sf.saxon.event;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.sort.IntHashSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;


/**
* IDFilter is a ProxyReceiver that extracts the subtree of a document rooted at the
* element with a given ID value. Namespace declarations outside this subtree are
* treated as if they were present on the identified element.
*/

public class IDFilter extends StartTagBuffer {

    private String requiredId;
    private int activeDepth = 0;
    private boolean matched = false;
    private IntHashSet nonIDs;

    public IDFilter (String id) {
        // System.err.println("IDFilter, looking for " + id);
        this.requiredId = id;
    }

    /**
     * startElement
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        matched = false;
        if (activeDepth>0) {
            activeDepth++;
        }
        super.startElement(nameCode, typeCode, locationId, properties);  // this remembers the details
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        super.attribute(nameCode, typeCode, value, locationId, properties);
        if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID || isIDCode(typeCode)) {
            if (value.toString().equals(requiredId)) {
                matched = true;
            }
        }
    }

    /**
     * startContent: Test if a matching ID attribute was found; if so, start outputting.
     */

    public void startContent() throws XPathException {
        if (activeDepth>0) {
            super.startContent();
        } else if (matched) {
            activeDepth = 1;
            super.startContent();
        }
    }

    protected void declareNamespacesForStartElement() throws XPathException {
        if (activeDepth == 1) {
            declareAllNamespaces();
        } else {
            super.declareNamespacesForStartElement();
        }
    }

    /**
     * endElement:
     */

    public void endElement() throws XPathException {
        if (activeDepth > 0) {
            nextReceiver.endElement();
            activeDepth--;
        } else {
            undeclareNamespacesForElement();
        }
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (activeDepth > 0) {
            super.characters(chars, locationId, properties);
        }
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (activeDepth > 0) {
            super.processingInstruction(target, data, locationId, properties);
        }
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        if (activeDepth > 0) {
            super.comment(chars, locationId, properties);
        }
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return true;
    }

    /**
     * Test whether a type annotation code represents the type xs:ID or one of its subtypes
     */

    private boolean isIDCode(int typeCode) {
        if ((typeCode & NamePool.FP_MASK) == StandardNames.XS_ID) {
            return true;
        } else if (typeCode < 1024) {
            // No other built-in type is an ID
            return false;
        } else {
            if (nonIDs == null) {
                nonIDs = new IntHashSet(20);
            }
            if (nonIDs.contains(typeCode)) {
                return false;
            }
            SchemaType type = getConfiguration().getSchemaType(typeCode);
            if (type.isAtomicType()) {
                if (getConfiguration().getTypeHierarchy().isSubType((AtomicType)type, BuiltInAtomicType.ID)) {
                    return true;
                } else {
                    nonIDs.add(typeCode);
                    return false;
                }
            } else {
                return false;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
