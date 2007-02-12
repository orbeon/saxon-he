using System;
using System.IO;
using System.Xml;
using System.Collections;
using JConfiguration = net.sf.saxon.Configuration;
using JAtomicValue = net.sf.saxon.value.AtomicValue;
using JStandardNames = net.sf.saxon.om.StandardNames;
using DotNetReceiver = net.sf.saxon.dotnet.DotNetReceiver;
using JBigDecimal = java.math.BigDecimal;
using JArrayList = java.util.ArrayList;
using net.sf.saxon.om;
using net.sf.saxon.value;
using net.sf.saxon.pattern;
using JAtomicType = net.sf.saxon.type.AtomicType;
using JSchemaType = net.sf.saxon.type.SchemaType;
using JType = net.sf.saxon.type.Type;


namespace Saxon.Api {

    /// <summary>
    /// An value in the XDM data model. A value is a sequence of zero or more
    /// items, each item being either an atomic value or a node.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XdmValue</c> is immutable.</para>
    /// <para>A sequence consisting of a single item <i>may</i> be represented
    /// as an instance of <c>XdmItem</c>, which is a subtype of <c>XdmValue</c>.</para>
    /// </remarks>

    [Serializable]
    public class XdmValue : IEnumerable {

        internal ValueRepresentation value;

        internal XdmValue() { }

        /// <summary>
        /// Create a value from a collection of items
        /// </summary>
        /// <param name="items">An enumerable collection providing the items to make up the sequence. Every
        /// member of this collection must be an instance of <c>XdmItem</c>
        /// </param>

        public XdmValue(IEnumerable items) {
            JArrayList list = new JArrayList();
            foreach (XdmItem c in items) {
                list.add((Item)c.Unwrap());
            }
            value = new SequenceExtent(list);
        }

        /// <summary>
        /// Create an XdmValue from an underlying Saxon ValueRepresentation object.
        /// This method is provided for the benefit of applications that need to mix
        /// use of the Saxon .NET API with direct use of the underlying objects
        /// and methods offered by the Java implementation.
        /// </summary>
        /// <param name="value">An object representing an XDM value in the
        /// underlying Saxon implementation.</param>
        /// <returns>An XdmValue that wraps the underlying Saxon value
        /// representation.</returns>

        public static XdmValue Wrap(ValueRepresentation value) {
            XdmValue result;
            if (value == null || value is EmptySequence) {
                return XdmEmptySequence.INSTANCE;
            } else if (value is JAtomicValue) {
                result = new XdmAtomicValue();
            } else if (value is NodeInfo) {
                result = new XdmNode();
            } else {
                result = new XdmValue();
            }
            result.value = value;
            return result;
        }

        /// <summary>
        /// Extract the underlying Saxon ValueRepresentation object from an XdmValue.
        /// This method is provided for the benefit of applications that need to mix
        /// use of the Saxon .NET API with direct use of the underlying objects
        /// and methods offered by the Java implementation.
        /// </summary>
        /// <returns>An object representing the XDM value in the
        /// underlying Saxon implementation.</returns>


        public ValueRepresentation Unwrap() {
            return value;
        }

        /// <summary>
        /// Get the sequence of items in the form of an <c>IList</c>
        /// </summary>
        /// <returns>
        /// The list of items making up this value. Each item in the list
        /// will be an object of type <c>XdmItem</c>
        /// </returns>        

        public IList GetList() {
            if (value == null) {
                return new ArrayList();
            } else if (value is Item) {
                ArrayList list = new ArrayList(1);
                list.Add((NodeInfo)value);
                return list;
            } else {
                ArrayList list = new ArrayList();
                SequenceIterator iter = ((Value)value).iterate();
                while (true) {
                    Item jitem = iter.next();
                    if (jitem == null) {
                        break;
                    }
                    list.Add((XdmItem)XdmValue.Wrap(jitem));
                }
                return list;
            }
        }

        /// <summary>
        /// Get the sequence of items in the form of an <c>IEnumerator</c>
        /// </summary>
        /// <returns>
        /// An enumeration over the list of items making up this value. Each item in the list
        /// will be an object of type <c>XdmItem</c>
        /// </returns>    

        public IEnumerator GetEnumerator() {
            if (value == null) {
                return EmptyEnumerator.INSTANCE;
            } else if (value is Item) {
                return new SequenceEnumerator(SingletonIterator.makeIterator((Item)value));
            } else {
                return new SequenceEnumerator(((Value)value).iterate());
            }
        }

        /// <summary>
        /// Get the number of items in the sequence
        /// </summary>
        /// <returns>
        /// The number of items in the sequence
        /// </returns> 

        public int Count {
            get {
                if (value == null) {
                    return 0;
                } else if (value is Item) {
                    return 1;
                } else {
                    return ((Value)value).getLength();
                }
            }
        }


    }

    /// <summary>
    /// The class <c>XdmItem</c> represents an item in a sequence, as defined
    /// by the XDM data model. An item is either an atomic value or a node.
    /// </summary>
    /// <remarks>
    /// <para>An item is a member of a sequence, but it can also be considered as
    /// a sequence (of length one) in its own right. <c>XdmItem</c> is a subtype
    /// of <c>XdmValue</c> because every Item in the XDM data model is also a
    /// value.</para>
    /// <para>It cannot be assumed that every sequence of length one will be 
    /// represented by an <c>XdmItem</c>. It is quite possible for an <c>XdmValue</c>
    /// that is not an <c>XdmItem</c> to hold a singleton sequence.</para>
    /// </remarks> 

    [Serializable]
    public abstract class XdmItem : XdmValue {

        /// <summary>
        /// Determine whether the item is an atomic value
        /// </summary>
        /// <returns>
        /// true if the item is an atomic value, false if it is a Node
        /// </returns>

        public abstract bool IsAtomic();

    }

    /// <summary>
    /// The class <c>XdmAtomicValue</c> represents an item in an XPath 2.0 sequence
    /// that is an atomic value. The value may belong to any of the 19 primitive types
    /// defined in XML Schema, or to a type derived from these primitive types, or to 
    /// the XPath 2.0 type <c>xdt:untypedAtomic</c>
    /// </summary>

    [Serializable]
    public class XdmAtomicValue : XdmItem {

        //internal JAtomicValue atomicValue;

        internal XdmAtomicValue() { }

        /// <summary>
        /// Determine whether the item is an atomic value
        /// </summary>
        /// <returns>
        /// true (the item is an atomic value)
        /// </returns>

        public override bool IsAtomic() {
            return true;
        }

        //internal AtomicValue(JAtomicValue value) {
        //    this.value = value;
        //}

        /// <summary>
        /// Construct an atomic value of type <c>xs:string</c>
        /// </summary>
        /// <param name="str">The string value</param>

        public XdmAtomicValue(String str) {
            this.value = new StringValue(str);
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:integer</c>
        /// </summary>
        /// <param name="i">The integer value</param>

        public XdmAtomicValue(long i) {
            this.value = new Int64Value(i);
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:decimal</c>
        /// </summary>
        /// <param name="d">The decimal value</param>

        public XdmAtomicValue(decimal d) {
            this.value = new DecimalValue(new JBigDecimal(d.ToString()));
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:float</c>
        /// </summary>
        /// <param name="f">The float value</param>        

        public XdmAtomicValue(float f) {
            this.value = new FloatValue(f);
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:double</c>
        /// </summary>
        /// <param name="d">The double value</param>

        public XdmAtomicValue(double d) {
            this.value = new DoubleValue(d);
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:boolean</c>
        /// </summary>
        /// <param name="b">The boolean value</param>

        public XdmAtomicValue(bool b) {
            this.value = BooleanValue.get(b);
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:anyURI</c>
        /// </summary>
        /// <param name="u">The uri value</param>

        public XdmAtomicValue(Uri u) {
            this.value = new AnyURIValue(u.ToString());
        }

        /// <summary>
        /// Construct an atomic value of type <c>xs:QName</c>
        /// </summary>
        /// <param name="q">The QName value</param>                

        public XdmAtomicValue(QName q) {
            this.value = new QNameValue(
                q.Prefix, q.Uri, q.LocalName, null);
        }

        /// <summary>
        /// Construct an atomic value of a given built-in or user-defined type
        /// </summary>
        /// <example>
        ///   <code>AtomicValue("abcd", QName.XDT_UNTYPED_ATOMIC)</code>
        ///   <para>creates an untyped atomic value containing the string "abcd"</para>
        /// </example>
        /// <param name="lexicalForm">The string representation of the value (any value that is acceptable
        /// in the lexical space, as defined by XML Schema Part 2). Whitespace normalization as defined by
        /// the target type will be applied to the value.</param>
        /// <param name="type">The QName giving the name of the target type. This must be an atomic
        /// type, and it must not be a type that is namespace-sensitive (QName, NOTATION, or types derived
        /// from these). If the type is a user-defined type then its definition must be present
        /// in the schema cache maintained by the <c>SchemaManager</c>.</param> 
        /// <param name="processor">The <c>Processor</c> object. This is needed for looking up user-defined
        /// types, and also because some conversions are context-sensitive, for example they depend on the
        /// implicit timezone or the choice of XML 1.0 versus XML 1.1 for validating names.</param>
        /// <exception name="ArgumentException">Thrown if the type is unknown or unsuitable, or if the supplied string is not
        /// a valid lexical representation of a value of the given type.</exception>

        public XdmAtomicValue(String lexicalForm, QName type, Processor processor) {
            JConfiguration jconfig = processor.config;
            int fp = jconfig.getNamePool().getFingerprint(type.Uri, type.LocalName);
            if (fp == -1) {
                throw new ArgumentException("Unknown name " + type);
            }
            JSchemaType st = jconfig.getSchemaType(fp);
            if (st == null) {
                throw new ArgumentException("Unknown type " + type);
            }
            if (!(st is JAtomicType)) {
                throw new ArgumentException("Specified type " + type + " is not atomic");
            }
            if (((JAtomicType)st).isNamespaceSensitive()) {
                throw new ArgumentException("Specified type " + type + " is namespace-sensitive");
            }
            JAtomicValue result = new StringValue(lexicalForm).convert(
                    (JAtomicType)st,
                    jconfig.getConversionContext(),
                    true);
            if (result is ValidationErrorValue) {
                throw new ArgumentException(((ValidationErrorValue)result).getException().getMessage());
            }
            this.value = result;
        }


        /// <summary>
        /// Convert the atomic value to a string
        /// </summary>
        /// <returns>The value converted to a string, according to the rules
        /// of the XPath 2.0 cast operator</returns>        

        public override String ToString() {
            return ((JAtomicValue)value).getStringValue();
        }

        /// <summary>
        /// Get the name of the value's XDM type
        /// </summary>
        /// <param name="processor">The <c>Processor</c> object. 
        /// This is needed for access to the NamePool,
        /// which maps the internal form of type names to their external form.</param>
        /// <returns>The type of the value, as a QName. This may be a built-in type or a user-defined
        /// atomic type.
        /// </returns>


        public QName GetTypeName(Processor processor) {
            int fp = ((JAtomicType)((JAtomicValue)value).getItemType(null)).getFingerprint();
            NamePool pool = processor.config.getNamePool();
            return new QName(pool.getPrefix(fp),
                             pool.getURI(fp),
                             pool.getLocalName(fp));
        }

        /// <summary>
        /// Get the name of the primitive type of the value
        /// </summary>
        /// <returns>The primitive type of the value, as a QName. This will be the name of
        /// one of the primitive types defined in XML Schema Part 2, or the XPath-defined
        /// type <c>xdt:untypedAtomic</c>. For the purposes of this method, <c>xs:integer</c> is considered
        /// to be a primitive type.
        /// </returns>


        public QName GetPrimitiveTypeName() {
            int fp = ((JAtomicValue)value).getItemType(null).getPrimitiveType();
            return new QName(JStandardNames.getPrefix(fp),
                             JStandardNames.getURI(fp),
                             JStandardNames.getLocalName(fp));
        }

        /// <summary>Get the value as a CLI object of the nearest equivalent type.</summary>
        /// <remarks>
        /// <para>The return type is as follows:</para>
        /// <para>xs:string - String</para>
        /// <para>xs:integer - Long</para>
        /// <para>xs:decimal - Decimal</para>
        /// <para>xs:double - Double</para>
        /// <para>xs:float - Float</para>
        /// <para>xs:boolean - Bool</para>
        /// <para>xs:QName - QName</para>
        /// <para>xs:anyURI - Uri</para>
        /// <para>xdt:untypedAtomic - String</para>
        /// <para>Other types - currently String, but this may change in the future</para>
        /// </remarks>
        /// <returns>The value converted to the most appropriate CLI type</returns>

        public Object Value {
            get {
                if (value is IntegerValue) {
                    return ((IntegerValue)value).longValue();
                } else if (value is DoubleValue) {
                    return ((DoubleValue)value).getDoubleValue();
                } else if (value is FloatValue) {
                    return ((FloatValue)value).getFloatValue();
                } else if (value is DecimalValue) {
                    return Decimal.Parse(((DecimalValue)value).getStringValue());
                } else if (value is BooleanValue) {
                    return ((BooleanValue)value).getBooleanValue();
                } else if (value is AnyURIValue) {
                    return new Uri(((AnyURIValue)value).getStringValue());
                } else if (value is QNameValue) {
                    return new QName((QNameValue)value);
                } else {
                    return ((JAtomicValue)value).getStringValue();
                }
            }
        }


    }

    /// <summary inherits="XdmItem">
    /// The class <c>XdmNode</c> represents a Node in the XDM Data Model. A Node
    /// is an <c>XdmItem</c>, and is therefore an <c>XdmValue</c> in its own right, and may also participate
    /// as one item within a sequence value.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XdmNode</c> is implemented as a wrapper around an object
    /// of type <c>net.sf.saxon.NodeInfo</c>. Because this is a key interface
    /// within Saxon, it is exposed via this API, even though it is a Java
    /// interface that is not part of the API proper.</para>
    /// <para>The <c>XdmNode</c> interface exposes basic properties of the node, such
    /// as its name, its string value, and its typed value. Navigation to other nodes
    /// is supported through a single method, <c>EnumerateAxis</c>, which allows
    /// other nodes to be retrieved by following any of the XPath axes.</para>
    /// </remarks>

    [Serializable]
    public class XdmNode : XdmItem {

        /// <summary>
        /// Determine whether the item is an atomic value
        /// </summary>
        /// <returns>
        /// false (the item is not an atomic value)
        /// </returns>

        public override bool IsAtomic() {
            return false;
        }

        /// <summary>
        /// The name of the node, as a <c>QName</c>. Returns null in the case of unnamed nodes.
        /// </summary>

        public QName NodeName {
            get {
                NodeInfo node = (NodeInfo)value;
                String local = node.getLocalPart();
                if (local == "") {
                    return null;
                }
                String prefix = node.getPrefix();
                String uri = node.getURI();
                return new QName(prefix, uri, local);
            }
        }

        /// <summary>
        /// The kind of node, as an instance of <c>System.Xml.XmlNodeType</c>.
        /// </summary>
        /// <remarks>For a namespace node in the XDM model, the value XmlNodeType.None 
        /// is returned.
        /// </remarks>

        public XmlNodeType NodeKind {
            get {
                NodeInfo node = (NodeInfo)value;
                int kind = node.getNodeKind();
                switch (kind) {
                    case JType.DOCUMENT:
                        return XmlNodeType.Document;
                    case JType.ELEMENT:
                        return XmlNodeType.Element;
                    case JType.ATTRIBUTE:
                        return XmlNodeType.Attribute;
                    case JType.TEXT:
                        return XmlNodeType.Text;
                    case JType.COMMENT:
                        return XmlNodeType.Comment;
                    case JType.PROCESSING_INSTRUCTION:
                        return XmlNodeType.ProcessingInstruction;
                    case JType.NAMESPACE:
                        return XmlNodeType.None;
                    default:
                        throw new ArgumentException("Unknown node kind");
                }
            }
        }

        /// <summary>
        /// The typed value of the node, as an instance of <c>XdmValue</c>.
        /// </summary>
        /// <exception>
        /// A DynamicError is thrown if the node has no typed value, as will be the case for
        /// an element with element-only content.
        /// </exception>

        public XdmValue TypedValue {
            get { return XdmValue.Wrap(((NodeInfo)value).atomize()); }
        }

        /// <summary>
        /// The string value of the node.
        /// </summary>

        public String StringValue {
            get { return ((NodeInfo)value).getStringValue(); }
        }

        /// <summary>
        /// Get the parent of this node.
        /// </summary>
        /// <remarks>
        /// Returns either a document node, and element node, or null in the case where
        /// this node has no parent. 
        /// </remarks>

        public XdmNode Parent {
            get { return (XdmNode)XdmValue.Wrap(((NodeInfo)value).getParent()); }
        }

        /// <summary>
        /// Get a the string value of a named attribute of this element. 
        /// </summary>
        /// <remarks>
        /// Returns null if this node is not an element, or if this element has no
        /// attribute with the specified name.
        /// </remarks>
        
        public String GetAttributeValue(QName name) {
            int fp = ((NodeInfo)value).getConfiguration().getNamePool().allocate(
                name.Prefix, name.Uri, name.LocalName);
            return ((NodeInfo)value).getAttributeValue(fp);
        }

        /// <summary>
        /// Get an enumerator that supplies all the nodes on one of the XPath
        /// axes, starting with this node.
        /// </summary>
        /// <param name="axis">
        /// The axis to be navigated, for example <c>XdmAxis.Child</c> for the child axis.
        /// </param>
        /// <remarks>
        /// The nodes are returned in axis order: that is, document order for a forwards
        /// axis, reverse document order for a reverse axis.
        /// </remarks>

        public IEnumerator EnumerateAxis(XdmAxis axis) {
            return new SequenceEnumerator(((NodeInfo)value).iterateAxis(GetAxisNumber(axis)));
        }

        /// <summary>
        /// Get an enumerator that selects all the nodes on one of the XPath
        /// axes, provided they have a given name. The nodes selected are those of the principal
        /// node kind (elements for most axes, attributes for the attribute axis, namespace nodes
        /// for the namespace axis) whose name matches the name given in the second argument.
        /// </summary>
        /// <param name="axis">
        /// The axis to be navigated, for example <c>XdmAxis.Child</c> for the child axis.
        /// </param>
        /// <param name="nodeName">
        /// The name of the required nodes, for example <c>new QName("", "item")</c> to select
        /// nodes with local name "item", in no namespace.
        /// </param>
        /// <remarks>
        /// The nodes are returned in axis order: that is, document order for a forwards
        /// axis, reverse document order for a reverse axis.
        /// </remarks>

        public IEnumerator EnumerateAxis(XdmAxis axis, QName nodeName) {
            int kind;
            switch (axis) {
                case XdmAxis.Attribute:
                    kind = net.sf.saxon.type.Type.ATTRIBUTE;
                    break;
                case XdmAxis.Namespace:
                    kind = net.sf.saxon.type.Type.NAMESPACE;
                    break;
                default:
                    kind = net.sf.saxon.type.Type.ELEMENT;
                    break;
            }
            NamePool pool = ((NodeInfo)value).getConfiguration().getNamePool();
            int nameCode = pool.allocate("", nodeName.Uri, nodeName.LocalName);
            NameTest test = new NameTest(kind, nameCode, pool);
            return new SequenceEnumerator(((NodeInfo)value).iterateAxis(GetAxisNumber(axis), test));
        }

        private static byte GetAxisNumber(XdmAxis axis) {
            switch (axis) {
                case XdmAxis.Ancestor: return Axis.ANCESTOR;
                case XdmAxis.AncestorOrSelf: return Axis.ANCESTOR_OR_SELF;
                case XdmAxis.Attribute: return Axis.ATTRIBUTE;
                case XdmAxis.Child: return Axis.CHILD;
                case XdmAxis.Descendant: return Axis.DESCENDANT;
                case XdmAxis.DescendantOrSelf: return Axis.DESCENDANT_OR_SELF;
                case XdmAxis.Following: return Axis.FOLLOWING;
                case XdmAxis.FollowingSibling: return Axis.FOLLOWING_SIBLING;
                case XdmAxis.Namespace: return Axis.NAMESPACE;
                case XdmAxis.Parent: return Axis.PARENT;
                case XdmAxis.Preceding: return Axis.PRECEDING;
                case XdmAxis.PrecedingSibling: return Axis.PRECEDING_SIBLING;
                case XdmAxis.Self: return Axis.SELF;
            }
            return 0;
        }

        /// <summary>
        /// The Base URI of the node.
        /// </summary>

        public Uri BaseUri {
            get { return new Uri(((NodeInfo)value).getBaseURI()); }
        }

        /// <summary>
        /// Send the node (that is, the subtree rooted at this node) to an <c>XmlTextWriter</c>
        /// </summary>
        /// <remarks>
        /// Note that a <c>XmlTextWriter</c> can only handle a well-formed XML document. This method
        /// will therefore signal an exception if the node is a document node with no children, or with
        /// more than one element child.
        /// </remarks>

        public void WriteTo(XmlTextWriter writer) {
            NodeInfo node = ((NodeInfo)value);
            DotNetReceiver receiver = new DotNetReceiver(writer);
            receiver.setPipelineConfiguration(node.getConfiguration().makePipelineConfiguration());
            receiver.open();
            node.copy(receiver, 2, false, 0);
            receiver.close();
        }

        /// <summary>
        /// Return a serialization of this node as lexical XML
        /// </summary>
        /// <remarks>
        /// <para>In the case of an element node, the result will be a well-formed
        /// XML document serialized as defined in the W3C XSLT/XQuery serialization specification,
        /// using options method="xml", indent="yes", omit-xml-declaration="yes".</para>
        /// <para>In the case of a document node, the result will be a well-formed
        /// XML document provided that the document node contains exactly one element child,
        /// and no text node children. In other cases it will be a well-formed external
        /// general parsed entity.</para>
        /// <para>In the case of an attribute node, the output is a string in the form
        /// <c>name="value"</c>. The name will use the original namespace prefix.</para>
        /// <para>Other nodes, such as text nodes, comments, and processing instructions, are
        /// represented as they would appear in lexical XML.</para>
        /// </remarks>

        public String OuterXml {
            get {
                NodeInfo node = ((NodeInfo)value);

                if (node.getNodeKind() == JType.ATTRIBUTE) {
                    String val = node.getStringValue().Replace("\"", "&quot;");
                    val = val.Replace("<", "&lt;");
                    val = val.Replace("&", "&amp;");
                    return node.getDisplayName() + "=\"" + val + '"';
                }

                Serializer serializer = new Serializer();
                serializer.SetOutputProperty(Serializer.METHOD, "xml");
                serializer.SetOutputProperty(Serializer.INDENT, "yes");
                serializer.SetOutputProperty(Serializer.OMIT_XML_DECLARATION, "yes");

                StringWriter sw = new StringWriter();
                serializer.SetOutputWriter(sw);
                node.copy(serializer.GetReceiver(node.getConfiguration()), 2, false, 0);
                return sw.ToString();
            }
        }


        /// <summary>
        /// Return a string representation of the node.
        /// </summary>
        /// <remarks>
        /// This currently returns the same as the <c>OuterXml</c> property.
        /// To get the string value as defined in XPath, use the <c>StringValue</c> property.
        /// </remarks>

        public override String ToString() {
            return OuterXml;
        }

        /// <summary>
        /// Escape hatch to the underlying class in the Java implementation
        /// </summary>

        public NodeInfo Implementation {
            get { return ((NodeInfo)value); }
        }


    }


    /// <summary inherits="XdmValue">
    /// The class <c>XdmEmptySequence</c> represents an empty sequence in the XDM Data Model.
    /// </summary>
    /// <remarks>
    /// <para>An empty sequence <i>may</i> also be represented by an <c>XdmValue</c> whose length
    /// happens to be zero. Applications should therefore not test to see whether an object
    /// is an instance of this class in order to decide whether it is empty.</para>
    /// <para>In interfaces that expect an <c>XdmItem</c>, an empty sequence is represented
    /// by a CLI <c>null</c> value.</para> 
    /// </remarks>

    [Serializable]
    public sealed class XdmEmptySequence : XdmValue {

        ///<summary>The singular instance of this class</summary>

        public static XdmEmptySequence INSTANCE = new XdmEmptySequence();

        private XdmEmptySequence() {
            this.value = null;
        }
    }


    /// <summary>
    /// The QName class represents an instance of xs:QName, as defined in the XPath 2.0
    /// data model. Internally, it has three components, a namespace URI, a local name, and
    /// a prefix. The prefix is intended to be used only when converting the value back to 
    /// a string.
    /// </summary>
    /// <remarks>
    /// Note that a QName is not itself an <c>XdmItem</c> in this model; however it can
    /// be wrapped in an XdmItem.
    /// </remarks>    

    [Serializable]
    public sealed class QName {

        private String prefix;
        private String uri;
        private String local;
        int hashcode = -1;      // evaluated lazily
        int fingerprint = -1;   // evaluated only if the QName is registered with the Processor
        private NamePool pool = null;

        private static String XS = NamespaceConstant.SCHEMA;

        /// <summary>QName constant for the name xs:string</summary>
        public static readonly QName XS_STRING = new QName(XS, "xs:string");

        /// <summary>QName constant for the name xs:integer</summary>
        public static readonly QName XS_INTEGER = new QName(XS, "xs:integer");

        /// <summary>QName constant for the name xs:double</summary>
        public static readonly QName XS_DOUBLE = new QName(XS, "xs:double");

        /// <summary>QName constant for the name xs:float</summary>
        public static readonly QName XS_FLOAT = new QName(XS, "xs:float");

        /// <summary>QName constant for the name xs:decimal</summary>
        public static readonly QName XS_DECIMAL = new QName(XS, "xs:decimal");

        /// <summary>QName constant for the name xs:boolean</summary>
        public static readonly QName XS_BOOLEAN = new QName(XS, "xs:boolean");

        /// <summary>QName constant for the name xs:anyURI</summary>
        public static readonly QName XS_ANYURI = new QName(XS, "xs:anyURI");

        /// <summary>QName constant for the name xs:QName</summary>
        public static readonly QName XS_QNAME = new QName(XS, "xs:QName");

        /// <summary>QName constant for the name xs:untypedAtomic</summary>
        public static readonly QName XS_UNTYPED_ATOMIC = new QName(XS, "xdt:untypedAtomic");

        /// <summary>QName constant for the name xs:untypedAtomic (for backwards compatibility)</summary>
        public static readonly QName XDT_UNTYPED_ATOMIC = new QName(XS, "xdt:untypedAtomic");

        /// <summary>
        /// Construct a QName using a namespace URI and a lexical representation.
        /// The lexical representation may be a local name on its own, or it may 
        /// be in the form <c>prefix:local-name</c>
        /// </summary>
        /// <remarks>
        /// This constructor does not check that the components of the QName are
        /// lexically valid.
        /// </remarks>
        /// <param name="uri">The namespace URI. Use either the string "" or null
        /// for names that are not in any namespace.
        /// </param>
        /// <param name="lexical">Either the local part of the name, or the prefix
        /// and local part in the format <c>prefix:local</c>
        /// </param>

        public QName(String uri, String lexical) {
            // TODO: check for validity
            this.uri = (uri == null ? "" : uri);
            int colon = lexical.IndexOf(':');
            if (colon < 0) {
                this.prefix = "";
                this.local = lexical;
            } else {
                this.prefix = lexical.Substring(0, colon);
                this.local = lexical.Substring(colon + 1);
            }
        }

        /// <summary>
        /// Construct a QName using a namespace prefix, a namespace URI, and a local name
        /// (in that order).
        /// </summary>
        /// <remarks>
        /// This constructor does not check that the components of the QName are
        /// lexically valid.
        /// </remarks>
        /// <param name="prefix">The prefix of the name. Use either the string ""
        /// or null for names that have no prefix (that is, they are in the default
        /// namespace)</param>
        /// <param name="uri">The namespace URI. Use either the string "" or null
        /// for names that are not in any namespace.
        /// </param>
        /// <param name="local">The local part of the name</param>

        public QName(String prefix, String uri, String local) {
            this.uri = (uri == null ? String.Empty : uri);
            this.local = local;
            this.prefix = (prefix == null ? String.Empty : prefix);
        }

        /// <summary>
        /// Construct a QName from a lexical QName, supplying an element node whose
        /// in-scope namespaces are to be used to resolve any prefix contained in the QName.
        /// </summary>
        /// <remarks>
        /// <para>This constructor checks that the components of the QName are
        /// lexically valid.</para>
        /// <para>If the lexical QName has no prefix, the name is considered to be in the
        /// default namespace, as defined by <c>xmlns="..."</c>.</para>
        /// <para>If the prefix of the lexical QName is not in scope, returns null.</para>
        /// </remarks>
        /// <param name="lexicalQName">The lexical QName, in the form <code>prefix:local</code>
        /// or simply <c>local</c>.</param>
        /// <param name="element">The element node whose in-scope namespaces are to be used
        /// to resolve the prefix part of the lexical QName.</param>
        /// <exception cref="ArgumentException">If the prefix of the lexical QName is not in scope</exception>
        /// <exception cref="ArgumentException">If the lexical QName is invalid 
        /// (for example, if it contains invalid characters)</exception>
        /// 

        public QName(String lexicalQName, XdmNode element) {
            try {
                NodeInfo node = (NodeInfo)element.value;
                NamePool pool = node.getConfiguration().getNamePool();
                int nc = pool.allocateLexicalQName(lexicalQName, true, new InscopeNamespaceResolver(node),
                    node.getConfiguration().getNameChecker());
                this.uri = pool.getURI(nc);
                this.local = pool.getLocalName(nc);
                this.prefix = pool.getPrefix(nc);
            } catch (net.sf.saxon.trans.DynamicError err) {
                throw new ArgumentException(err.getMessage());
            }
        }

        /// <summary>
        /// Construct a <c>QName</c> from an <c>XmlQualifiedName</c> (as defined in the
        /// <c>System.Xml</c> package).
        /// </summary>
        /// <remarks>
        /// Note that an <c>XmlQualifiedName</c> does not contain any prefix, so the result
        /// will always have a prefix of ""
        /// </remarks>
        /// <param name="qualifiedName">The XmlQualifiedName</param>

        public QName(XmlQualifiedName qualifiedName) {
            this.uri = qualifiedName.Namespace;
            this.local = qualifiedName.Name;
            this.prefix = String.Empty;
        }

        //  internal constructor from a QNameValue

        internal QName(QNameValue q) {
            this.uri = q.getNamespaceURI();
            this.prefix = q.getPrefix();
            this.local = q.getLocalName();
        }

        /// <summary>
        /// Factory method to construct a QName from a string containing the expanded
        /// QName in Clark notation, that is, <c>{uri}local</c>
        /// </summary>
        /// <remarks>
        /// The prefix part of the <c>QName</c> will be set to an empty string.
        /// </remarks>
        /// <param name="expandedName">The URI in Clark notation: <c>{uri}local</c> if the
        /// name is in a namespace, or simply <c>local</c> if not.</param> 

        public static QName FromClarkName(String expandedName) {
            String namespaceURI;
            String localName;
            if (expandedName[0] == '{') {
                int closeBrace = expandedName.IndexOf('}');
                if (closeBrace < 0) {
                    throw new ArgumentException("No closing '}' in Clark name");
                }
                namespaceURI = expandedName.Substring(1, closeBrace - 1);
                if (closeBrace == expandedName.Length) {
                    throw new ArgumentException("Missing local part in Clark name");
                }
                localName = expandedName.Substring(closeBrace + 1);
            } else {
                namespaceURI = "";
                localName = expandedName;
            }

            return new QName("", namespaceURI, localName);
        }

        /// <summary>
        /// Register a QName with the <c>Processor</c>. This makes comparison faster
        /// when the QName is compared with others that are also registered with the <c>Processor</c>.
        /// </summary>
        /// <param name="processor">The Processor in which the name is to be registered.</param>

        public void Register(Processor processor) {
            pool = processor.config.getNamePool();
            fingerprint = pool.allocate(prefix, uri, local) & 0xfffff;
        }

        /// <summary>
        /// Validate the QName against the XML 1.0 or XML 1.1 rules for valid names.
        /// </summary>
        /// <param name="processor">The Processor in which the name is to be validated.
        /// This determines whether the XML 1.0 or XML 1.1 rules for forming names are used.</param>
        /// <returns>true if the name is valid, false if not</returns>

        public bool IsValid(Processor processor) {
            NameChecker nc = processor.config.getNameChecker();
            if (prefix != String.Empty) {
                if (!nc.isValidNCName(prefix)) {
                    return false;
                }
            }
            if (!nc.isValidNCName(local)) {
                return false;
            }
            return true;
        }

        /// <summary>The prefix of the QName. This plays no role in operations such as comparison
        /// of QNames for equality, but is retained (as specified in XPath) so that a string representation
        /// can be reconstructed.
        /// </summary>
        /// <remarks>
        /// Returns the zero-length string in the case of a QName that has no prefix.
        /// </remarks>

        public String Prefix {
            get { return prefix; }
        }

        /// <summary>The namespace URI of the QName. Returns "" (the zero-length string) if the
        /// QName is not in a namespace.
        /// </summary>

        public String Uri {
            get { return uri; }
        }

        /// <summary>The local part of the QName</summary>

        public String LocalName {
            get { return local; }
        }

        /// <summary>The expanded name, as a string using the notation devised by James Clark.
        /// If the name is in a namespace, the resulting string takes the form <c>{uri}local</c>.
        /// Otherwise, the value is the local part of the name.
        /// </summary>

        public String ClarkName {
            get {
                if (uri == "") {
                    return local;
                } else {
                    return "{" + uri + "}" + local;
                }
            }
        }

        /// <summary>
        /// Convert the value to a string. The resulting string is the lexical form of the QName,
        /// using the original prefix if there was one.
        /// </summary>

        public override String ToString() {
            if (prefix == "") {
                return local;
            } else {
                return prefix + ":" + uri;
            }
        }

        /// <summary>
        /// Get a hash code for the QName, to support equality matching. This supports the
        /// semantics of equality, which considers only the namespace URI and local name, and
        /// not the prefix.
        /// </summary>
        /// <remarks>
        /// The algorithm for allocating a hash code does not depend on registering the QName 
        /// with the <c>Processor</c>.
        /// </remarks>

        public override int GetHashCode() {
            if (hashcode == -1) {
                hashcode = ClarkName.GetHashCode();
            }
            return hashcode;
        }

        /// <summary>
        /// Test whether two QNames are equal. This supports the
        /// semantics of equality, which considers only the namespace URI and local name, and
        /// not the prefix.
        /// </summary>
        /// <remarks>
        /// The result of the function does not depend on registering the QName 
        /// with the <c>Processor</c>, but is computed more quickly if the QNames have
        /// both been registered
        /// </remarks>

        public override bool Equals(Object other) {
            if (!(other is QName)) {
                return false;
            }
            if (pool != null && pool == ((QName)other).pool) {
                return fingerprint == ((QName)other).fingerprint;
            }
            if (GetHashCode() != ((QName)other).GetHashCode()) {
                return false;
            }
            return ClarkName == ((QName)other).ClarkName;
            //TODO: avoid computing ClarkName more than once
        }

        /// <summary>
        /// Convert the value to an <c>XmlQualifiedName</c> (as defined in the
        /// <c>System.Xml</c> package)
        /// </summary>
        /// <remarks>
        /// Note that this loses the prefix.
        /// </remarks>

        public XmlQualifiedName ToXmlQualifiedName() {
            return new XmlQualifiedName(local, uri);
        }

        /// <summary>
        /// Convert to a net.sf.saxon.value.QNameValue
        /// </summary>

        internal QNameValue ToQNameValue() {
            return new QNameValue(prefix, uri, local, null);
        }


    }

    /// <summary>
    /// This class represents an enumeration of the values in an XPath
    /// sequence. It implements the IEnumerator interface, and the objects
    /// returned are always instances of <c>XPathItem</c>
    /// </summary>
    /// <remarks>
    /// Because the underlying value can be evaluated lazily, it is possible
    /// for exceptions to occur as the sequence is being read.
    /// </remarks>

    [Serializable]
    public class SequenceEnumerator : IEnumerator {

        private SequenceIterator iter;

        internal SequenceEnumerator(SequenceIterator iter) {
            this.iter = iter;
        }

        /// <summary>Return the current item in the sequence</summary>
        /// <returns>An object which will always be an instance of <c>XdmItem</c></returns>

        public object Current {
            get { return XdmValue.Wrap(iter.current()); }
        }

        /// <summary>Move to the next item in the sequence</summary>
        /// <returns>true if there are more items in the sequence</returns>

        public bool MoveNext() {
            return (iter.next() != null);
        }

        /// <summary>Reset the enumeration so that the next call of
        /// <c>MoveNext</c> will position the enumeration at the
        /// first item in the sequence</summary>

        public void Reset() {
            iter = iter.getAnother();
        }
    }

    /// <summary>
    /// Enumeration identifying the thirteen XPath axes
    /// </summary>

    public enum XdmAxis {
        /// <summary>The XPath ancestor axis</summary> 
        Ancestor,
        /// <summary>The XPath ancestor-or-self axis</summary> 
        AncestorOrSelf,
        /// <summary>The XPath attribute axis</summary> 
        Attribute,
        /// <summary>The XPath child axis</summary> 
        Child,
        /// <summary>The XPath descendant axis</summary> 
        Descendant,
        /// <summary>The XPath descandant-or-self axis</summary> 
        DescendantOrSelf,
        /// <summary>The XPath following axis</summary> 
        Following,
        /// <summary>The XPath following-sibling axis</summary> 
        FollowingSibling,
        /// <summary>The XPath namespace axis</summary> 
        Namespace,
        /// <summary>The XPath parent axis</summary> 
        Parent,
        /// <summary>The XPath preceding axis</summary> 
        Preceding,
        /// <summary>The XPath preceding-sibling axis</summary> 
        PrecedingSibling,
        /// <summary>The XPath self axis</summary> 
        Self
    }

    internal class EmptyEnumerator : IEnumerator {

        public static EmptyEnumerator INSTANCE = new EmptyEnumerator();

        private EmptyEnumerator() { }

        public void Reset() { }

        public object Current {
            get { throw new InvalidOperationException("Collection is empty."); }
        }

        public bool MoveNext() {
            return false;
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