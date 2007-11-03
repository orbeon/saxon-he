package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.expr.Token;
import net.sf.saxon.pattern.*;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.SchemaDeclaration;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

/**
 * This class is used for creating ItemType objects.
 */
public class ItemTypeFactory {

    private Processor processor;

    /**
     * Create an ItemTypeFactory
     * @param processor the processor used by this ItemTypeFactory. This must be supplied
     * in the case of user-defined types or types that reference element or attribute names;
     * for built-in types it can be omitted.
     */

    public ItemTypeFactory(Processor processor) {
        this.processor = processor;
    }

    /**
     * Get an item type representing an atomic type. This may be a built-in type in the
     * XML Schema namespace, or a user-defined atomic type.
     *
     * <p>It is undefined whether two calls supplying the same QName will return the same ItemType
     * object.</p>
     * @param name the name of the built-in atomic type required
     * @return an ItemType object representing this built-in atomic type
     * @throws SaxonApiException if the type name is not known, or if the type identified by the
     * name is not an atomic type.
     */

    public ItemType getAtomicType(QName name) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        int fp = config.getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
        SchemaType type = config.getSchemaType(fp);
        if (type == null || !type.isAtomicType()) {
            throw new SaxonApiException("Unknown atomic type " + name.getClarkName());
        }
        return new ItemType((AtomicType)type, processor);
//        String uri = name.getNamespaceURI();
//        if (!NamespaceConstant.SCHEMA.equals(uri)) {
//            SchemaType type = .getSchemaType()
//        }
//        String local = name.getLocalName();
//        switch (local.length()) {
//        case 2:
//            if (local.equals("ID")) {
//                return new ItemType(BuiltInAtomicType.ID, processor);
//            }
//            break;
//        case 3:
//            if (local.equals("int")) {
//                return new ItemType(BuiltInAtomicType.INT, processor);
//            }
//            break;
//        case 4:
//            if (local.equals("date")) {
//                return new ItemType(BuiltInAtomicType.DATE, processor);
//            } else if (local.equals("time")) {
//                return new ItemType(BuiltInAtomicType.TIME, processor);
//            } else if (local.equals("long")) {
//                return new ItemType(BuiltInAtomicType.LONG, processor);
//            } else if (local.equals("byte")) {
//                return new ItemType(BuiltInAtomicType.BYTE, processor);
//            } else if (local.equals("Name")) {
//                return new ItemType(BuiltInAtomicType.NAME, processor);
//            } else if (local.equals("gDay")) {
//                return new ItemType(BuiltInAtomicType.G_DAY, processor);
//            }
//            break;
//        case 5:
//            if (local.equals("gYear")) {
//                return new ItemType(BuiltInAtomicType.G_YEAR, processor);
//            } else if (local.equals("QName")) {
//                return new ItemType(BuiltInAtomicType.QNAME, processor);
//            } else if (local.equals("float")) {
//                return new ItemType(BuiltInAtomicType.FLOAT, processor);
//            } else if (local.equals("short")) {
//                return new ItemType(BuiltInAtomicType.SHORT, processor);
//            } else if (local.equals("IDREF")) {
//                return new ItemType(BuiltInAtomicType.IDREF, processor);
//            } else if (local.equals("token")) {
//                return new ItemType(BuiltInAtomicType.TOKEN, processor);
//            }
//            break;
//        case 6:
//            if (local.equals("anyURI")) {
//                return new ItemType(BuiltInAtomicType.ANY_URI, processor);
//            } else if (local.equals("gMonth")) {
//                return new ItemType(BuiltInAtomicType.G_MONTH, processor);
//            } else if (local.equals("string")) {
//                return new ItemType(BuiltInAtomicType.STRING, processor);
//            } else if (local.equals("double")) {
//                return new ItemType(BuiltInAtomicType.DOUBLE, processor);
//            } else if (local.equals("NCName")) {
//                return new ItemType(BuiltInAtomicType.NCNAME, processor);
//            } else if (local.equals("ENTITY")) {
//                return new ItemType(BuiltInAtomicType.ENTITY, processor);
//            }
//            break;
//        case 7:
//            if (local.equals("boolean")) {
//                return new ItemType(BuiltInAtomicType.BOOLEAN, processor);
//            } else if (local.equals("decimal")) {
//                return new ItemType(BuiltInAtomicType.DECIMAL, processor);
//            } else if (local.equals("integer")) {
//                return new ItemType(BuiltInAtomicType.INTEGER, processor);
//            } else if (local.equals("NMTOKEN")) {
//                return new ItemType(BuiltInAtomicType.NMTOKEN, processor);
//            }
//            break;
//        case 8:
//            if (local.equals("duration")) {
//                return new ItemType(BuiltInAtomicType.DURATION, processor);
//            } else if (local.equals("dateTime")) {
//                return new ItemType(BuiltInAtomicType.DATE_TIME, processor);
//            } else if (local.equals("NOTATION")) {
//                return new ItemType(BuiltInAtomicType.NOTATION, processor);
//            } else if (local.equals("language")) {
//                return new ItemType(BuiltInAtomicType.LANGUAGE, processor);
//            }
//            break;
//        case 9:
//            if (local.equals("gMonthDay")) {
//                return new ItemType(BuiltInAtomicType.G_MONTH_DAY, processor);
//            } else if (local.equals("hexBinary")) {
//                return new ItemType(BuiltInAtomicType.HEX_BINARY, processor);
//            }
//            break;
//        case 10:
//            if (local.equals("gYearMonth")) {
//                return new ItemType(BuiltInAtomicType.G_YEAR_MONTH, processor);
//            }
//            break;
//        case 11:
//            if (local.equals("unsignedInt")) {
//                return new ItemType(BuiltInAtomicType.UNSIGNED_INT, processor);
//            }
//            break;
//        case 12:
//            if (local.equals("base64Binary")) {
//                return new ItemType(BuiltInAtomicType.BASE64_BINARY, processor);
//            } else if (local.equals("unsignedLong")) {
//                return new ItemType(BuiltInAtomicType.UNSIGNED_LONG, processor);
//            } else if (local.equals("unsignedByte")) {
//                return new ItemType(BuiltInAtomicType.UNSIGNED_BYTE, processor);
//            }
//            break;
//        case 13:
//            if (local.equals("unsignedShort")) {
//                return new ItemType(BuiltInAtomicType.UNSIGNED_SHORT, processor);
//            } else if (local.equals("untypedAtomic")) {
//                return new ItemType(BuiltInAtomicType.UNTYPED_ATOMIC, processor);
//            } else if (local.equals("anyAtomicType")) {
//                return new ItemType(BuiltInAtomicType.ANY_ATOMIC, processor);
//            }
//            break;
//        case 15:
//            if (local.equals("negativeInteger")) {
//                return new ItemType(BuiltInAtomicType.NEGATIVE_INTEGER, processor);
//            }
//            break;
//        case 16:
//            if (local.equals("normalizedString")) {
//                return new ItemType(BuiltInAtomicType.NORMALIZED_STRING, processor);
//            }
//            break;
//        case 17:
//            if (local.equals("yearMonthDuration")) {
//                return new ItemType(BuiltInAtomicType.YEAR_MONTH_DURATION, processor);
//            }
//            break;
//        case 18:
//            if (local.equals("nonPositiveInteger")) {
//                return new ItemType(BuiltInAtomicType.NON_POSITIVE_INTEGER, processor);
//            } else if (local.equals("nonNegativeInteger")) {
//                return new ItemType(BuiltInAtomicType.NON_NEGATIVE_INTEGER, processor);
//            }
//            break;
//        }
//        throw new IllegalArgumentException("Unknown built-in atomic type " + name.getClarkName());
    }

    /**
     * Get an item type that matches any node of a specified kind.
     *
     * <p>This corresponds to the XPath syntactic forms element(), attribute(),
     * document-node(), text(), comment(), processing-instruction(). It also provides
     * an option, not available in the XPath syntax, that matches namespace nodes.</p>
     *
     * <p>It is undefined whether two calls supplying the same argument value will
     * return the same ItemType object.</p>
     *
     * @param kind the kind of node for which a NodeTest is required
     * @return an item type corresponding to the specified kind of node
     */

    public ItemType getNodeKindTest(XdmNodeKind kind) {
        switch (kind) {
        case DOCUMENT:
            return new ItemType(NodeKindTest.DOCUMENT, processor);
        case ELEMENT:
            return new ItemType(NodeKindTest.ELEMENT, processor);
        case ATTRIBUTE:
            return new ItemType(NodeKindTest.ATTRIBUTE, processor);
        case TEXT:
            return new ItemType(NodeKindTest.TEXT, processor);
        case COMMENT:
            return new ItemType(NodeKindTest.COMMENT, processor);
        case PROCESSING_INSTRUCTION:
            return new ItemType(NodeKindTest.PROCESSING_INSTRUCTION, processor);
        case NAMESPACE:
            return new ItemType(NodeKindTest.NAMESPACE, processor);
        default:
            throw new IllegalArgumentException("XdmNodeKind");
        }
    }

    /**
     * Get an item type that matches nodes of a specified kind with a specified name.
     *
     * <p>This corresponds to the XPath syntactic forms element(name), attribute(name),
     * and processing-instruction(name). In the case of processing-instruction, the supplied
     * QName must have no namespace.</p>
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param kind the kind of nodes that match
     * @param name the name of the nodes that match
     * @return an ItemType that matches nodes of a given node kind with a given name
     */

    public ItemType getItemType(XdmNodeKind kind, QName name) {
        // TODO: check it's an appropriate node kind
        NameTest type = new NameTest(kind.getNumber(),
                name.getNamespaceURI(), name.getLocalName(), processor.getUnderlyingConfiguration().getNamePool());
        return new ItemType(type, processor);
    }

    /**
     * Make an ItemType representing an element declaration in the schema. This is the
     * equivalent of the XPath syntax <code>schema-element(element-name)</code>
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param name the element name
     * @return the ItemType
     * @throws SaxonApiException if the schema does not contain a global element declaration
     * for the given name
     */

    public ItemType getSchemaElementTest(QName name) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        int fingerprint = config.getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
        SchemaDeclaration decl = config.getElementDeclaration(fingerprint);
        if (decl == null) {
            throw new SaxonApiException("No global declaration found for element " + name.getClarkName());
        }
        CombinedNodeTest combo = new CombinedNodeTest(
                new NameTest(Type.ELEMENT, fingerprint, config.getNamePool()),
                Token.INTERSECT,
                new ContentTypeTest(Type.ELEMENT, decl.getType(), config));
        combo.setGlobalComponentTest(true);
        return new ItemType(combo, processor);
    }

    /**
     * Make an ItemType that tests an element name and/or schema type. This is the
     * equivalent of the XPath syntax <code>element(element-name, type)</code>
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param name the element name, or null if there is no constraint on the name (equivalent to
     * specifying <code>element(*, type)</code>)
     * @param schemaType the name of the required schema type, or null of there is no constraint
     * on the type (equivalent to specifying <code>element(name)</code>)
     * @param nillable if a nilled element is allowed to match the type (equivalent to specifying
     * "?" after the type name). The value is ignored if schemaType is null.
     * @return the constructed ItemType
     * @throws SaxonApiException if the schema does not contain a global element declaration
     * for the given name
     */

    public ItemType getElementTest(QName name, QName schemaType, boolean nillable) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        NameTest nameTest = null;
        ContentTypeTest contentTest = null;
        if (name != null) {
            int elementFP = config.getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
            nameTest = new NameTest(Type.ELEMENT, elementFP, config.getNamePool());
        }
        if (schemaType != null) {
            int typeFP = config.getNamePool().allocate("", schemaType.getNamespaceURI(), schemaType.getLocalName());
            SchemaType type = config.getSchemaType(typeFP);
            if (type == null) {
                throw new SaxonApiException("Unknown schema type " + schemaType.getClarkName());
            }
            contentTest = new ContentTypeTest(Type.ELEMENT, type, config);
            contentTest.setNillable(nillable);
        }
        if (contentTest == null) {
            if (nameTest == null) {
                return getNodeKindTest(XdmNodeKind.ELEMENT);
            } else {
                return new ItemType(nameTest, processor);
            }
        } else {
            if (nameTest == null) {
                return new ItemType(contentTest, processor);
            } else {
                CombinedNodeTest combo = new CombinedNodeTest(
                        nameTest,
                        Token.INTERSECT,
                        contentTest);
                return new ItemType(combo, processor);
            }
        }
    }

    /**
     * Get an ItemType representing an attribute declaration in the schema. This is the
     * equivalent of the XPath syntax <code>schema-attribute(attribute-name)</code>
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param name the attribute name
     * @return the ItemType
     * @throws SaxonApiException if the schema does not contain a global attribute declaration
     * for the given name
     */

    public ItemType getSchemaAttributeTest(QName name) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        int fingerprint = config.getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
        SchemaDeclaration decl = config.getAttributeDeclaration(fingerprint);
        if (decl == null) {
            throw new SaxonApiException("No global declaration found for attribute " + name.getClarkName());
        }
        CombinedNodeTest combo = new CombinedNodeTest(
                new NameTest(Type.ATTRIBUTE, fingerprint, config.getNamePool()),
                Token.INTERSECT,
                new ContentTypeTest(Type.ATTRIBUTE, decl.getType(), config));
        combo.setGlobalComponentTest(true);
        return new ItemType(combo, processor);
    }

    /**
     * Get an ItemType that tests an element name and/or schema type. This is the
     * equivalent of the XPath syntax <code>element(element-name, type)</code>
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param name the element name, or null if there is no constraint on the name (equivalent to
     * specifying <code>element(*, type)</code>)
     * @param schemaType the name of the required schema type, or null of there is no constraint
     * on the type (equivalent to specifying <code>element(name)</code>)
     * @return the constructed ItemType
     * @throws SaxonApiException if the schema does not contain a global element declaration
     * for the given name
     */

    public ItemType getAttributeTest(QName name, QName schemaType) throws SaxonApiException {
        NameTest nameTest = null;
        ContentTypeTest contentTest = null;
        Configuration config = processor.getUnderlyingConfiguration();
        if (name != null) {
            int attributeFP = config.getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
            nameTest = new NameTest(Type.ATTRIBUTE, attributeFP, config.getNamePool());
        }
        if (schemaType != null) {
            int typeFP = config.getNamePool().allocate("", schemaType.getNamespaceURI(), schemaType.getLocalName());
            SchemaType type = config.getSchemaType(typeFP);
            if (type == null) {
                throw new SaxonApiException("Unknown schema type " + schemaType.getClarkName());
            }
            contentTest = new ContentTypeTest(Type.ATTRIBUTE, type, config);
        }
        if (contentTest == null) {
            if (nameTest == null) {
                return getNodeKindTest(XdmNodeKind.ATTRIBUTE);
            } else {
                return new ItemType(nameTest, processor);
            }
        } else {
            if (nameTest == null) {
                return new ItemType(contentTest, processor);
            } else {
                CombinedNodeTest combo = new CombinedNodeTest(
                        nameTest,
                        Token.INTERSECT,
                        contentTest);
                return new ItemType(combo, processor);
            }
        }
    }

    /**
     * Make an ItemType representing a document-node() test with an embedded element test.
     * This reflects the XPath syntax <code>document-node(element(N, T))</code> or
     * <code>document-node(schema-element(N))</code>.
     *
     * <p>It is undefined whether two calls supplying the same argument values will
     * return the same ItemType object.</p>
     *
     * @param elementTest the elementTest. An IllegalArgumentException is thrown if the supplied
     * ItemTest is not an elementTest or schemaElementTest.
     * @return a new ItemType representing the document test
     */

    public ItemType getDocumentTest(ItemType elementTest) {
        net.sf.saxon.type.ItemType test = elementTest.getUnderlyingItemType();
        if (test.getPrimitiveType() != Type.ELEMENT) {
            throw new IllegalArgumentException("Supplied itemType is not an element test");
        }
        DocumentNodeTest docTest = new DocumentNodeTest((NodeTest)test);
        return new ItemType(docTest, processor);
    }

    /**
     * Get an ItemType representing the type of a supplied XdmItem. If the supplied item is
     * an atomic value, the returned ItemType will reflect the most specific atomic type of the
     * item. If the supplied item is a node, the returned item type will reflect the node kind,
     * and if the node has a name, then its name. It will not reflect the type annotation.
     * @param item the supplied item whose type is required
     * @return the type of the supplied item
     */

    public ItemType getItemType(XdmItem item) {
        Configuration config = processor.getUnderlyingConfiguration();
        if (item.isAtomicValue()) {
            AtomicValue value = (AtomicValue)item.getUnderlyingValue();

            AtomicType type = (AtomicType)value.getItemType(config.getTypeHierarchy());
            return new ItemType(type, processor);
        } else {
            NodeInfo node = (NodeInfo)item.getUnderlyingValue();
            int kind = node.getNodeKind();
            int fp = node.getFingerprint();
            if (fp == -1) {
                return new ItemType(NodeKindTest.makeNodeKindTest(kind), processor);
            } else {
                return new ItemType(new NameTest(kind, fp, config.getNamePool()), processor);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

