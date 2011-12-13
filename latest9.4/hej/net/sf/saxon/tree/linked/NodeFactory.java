package net.sf.saxon.tree.linked;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.tree.util.AttributeCollectionImpl;
import net.sf.saxon.type.SchemaType;

import java.io.Serializable;


/**
  * Interface NodeFactory. <br>
  * A Factory for nodes used to build a tree. <br>
  * Currently only allows Element nodes to be user-constructed.
  * @author Michael H. Kay
  * @version 25 February 2000
  */

public interface NodeFactory extends Serializable {

    /**
    * Create an Element node
     * @param parent The parent element
      * @param nameCode The element name
     * @param elementType The type annotation of the element
     * @param isNilled true if the element is to be marked as nilled
     * @param attlist The attribute collection, excluding any namespace attributes
     * @param namespaces List of new namespace declarations for this element, as a sequence
* of namespace codes representing pairs of strings: (prefix1, uri1), (prefix2, uri2)...
     * @param namespacesUsed the number of elemnts of the namespaces array actually used
     * @param pipe The pipeline configuration (provides access to the error listener and the
* location provider)
     * @param locationId Indicates the source document and line number containing the node
     * @param sequenceNumber Sequence number to be assigned to represent document order.
     */

    /*@Nullable*/ public ElementImpl makeElementNode(
            NodeInfo parent,
            NodeName nameCode,
            SchemaType elementType,
            boolean isNilled,
            AttributeCollectionImpl attlist,
            NamespaceBinding[] namespaces,
            int namespacesUsed,
            PipelineConfiguration pipe,
            int locationId,
            int sequenceNumber);

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