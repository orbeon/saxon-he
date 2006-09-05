using System;
using System.IO;
using System.Xml;
using System.Collections;
using JConfiguration = net.sf.saxon.Configuration;
using JReceiver = net.sf.saxon.@event.Receiver;
using JProperties = java.util.Properties;
using JOutputStream = java.io.OutputStream;
using JWriter = java.io.Writer;
using JFileOutputStream = java.io.FileOutputStream;
using JDynamicError = net.sf.saxon.trans.DynamicError;
using JResult = javax.xml.transform.Result;
using JStreamResult = javax.xml.transform.stream.StreamResult;
using JTinyBuilder = net.sf.saxon.tinytree.TinyBuilder;
using net.sf.saxon.@event;
using net.sf.saxon.om;
using net.sf.saxon.value;
using net.sf.saxon.query;
using net.sf.saxon.dotnet;


namespace Saxon.Api {



    /// <summary>
    /// An abstract destination for the results of a query or transformation
    /// </summary>
    /// <remarks>
    /// <para>Note to implementors: To implement a new kind of destination, you need
    /// to supply a method <c>getResult</c> which returns an implementation of
    /// the JAXP <c>Result</c> interface. Optionally, if the destination
    /// performs serialization, you can also implement <c>getOutputProperties</c>,
    /// which returns the properties used for serialization.
    /// </para>
    /// </remarks>


    public abstract class XmlDestination {

        /// <summary>
        /// Get a <c>Result</c> to which the XML document can be sent as a series
        /// of events.
        /// </summary>
        /// <remarks>
        /// This must be an implementation of the JAXP <c>Result</c> interface that is
        /// recognized by Saxon.
        /// </remarks>

        public abstract JResult GetResult();

        /// <summary>
        /// Get a set of <c>Properties</c> representing the parameters to the serializer.
        /// The default implementation returns an empty set of properties.
        /// </summary>

        public virtual JProperties GetOutputProperties() {
            return new JProperties();
        }

        /// <summary>
        /// Close the Destination, releasing any resources that need to be released.
        /// </summary>
        /// <remarks>
        /// This method is called by the system on completion of a query or transformation.
        /// Some kinds of Destination may need to close an output stream, others might
        /// not need to do anything. The default implementation does nothing.
        /// </remarks>

        public virtual void Close() {
        }


    }

    /// <summary>
    /// A <c>Serializer</c> takes a tree representation of XML and turns
    /// it into lexical XML markup.
    /// </summary>
    /// <remarks>
    /// Note that this is serialization in the sense of the W3C XSLT and XQuery specifications.
    /// Unlike the class <c>System.Xml.Serialization.XmlSerializer</c>, this object does not
    /// serialize arbitrary CLI objects.
    /// </remarks>

    public class Serializer : XmlDestination {

        private JProperties props = new JProperties();
        private JOutputStream outputStream = null;
        private JWriter writer = null;
        private bool mustClose = true;


        /// <summary>QName identifying the serialization parameter "method". If the method
        /// is a user-defined method, then it is given as a QName in Clark notation, that is
        /// "{uri}local".</summary>

        public static readonly QName METHOD =
            new QName("", "method");

        /// <summary>QName identifying the serialization parameter "byte-order-mark"</summary>

        public static readonly QName BYTE_ORDER_MARK =
            new QName("", "byte-order-mark");

        /// <summary>QName identifying the serialization parameter "cdata-section-elements".
        /// The value of this parameter is given as a space-separated list of expanded QNames in
        /// Clark notation, that is "{uri}local".</summary>

        public static readonly QName CDATA_SECTION_ELEMENTS =
            new QName("", "cdata-section-elements");

        /// <summary>QName identifying the serialization parameter "doctype-public"</summary>

        public static readonly QName DOCTYPE_PUBLIC =
            new QName("", "doctype-public");

        /// <summary>QName identifying the serialization parameter "doctype-system"</summary>

        public static readonly QName DOCTYPE_SYSTEM =
            new QName("", "doctype-system");

        /// <summary>QName identifying the serialization parameter "encoding"</summary>

        public static readonly QName ENCODING =
            new QName("", "encoding");

        /// <summary>QName identifying the serialization parameter "escape-uri-attributes".
        /// The value is the string "yes" or "no".</summary>

        public static readonly QName ESCAPE_URI_ATTRIBUTES =
            new QName("", "escape-uri-attributes");

        /// <summary>QName identifying the serialization parameter "include-content-type".
        /// The value is the string "yes" or "no".</summary>

        public static readonly QName INCLUDE_CONTENT_TYPE =
            new QName("", "include-content-type");

        /// <summary>QName identifying the serialization parameter "indent".
        /// The value is the string "yes" or "no".</summary>

        public static readonly QName INDENT =
            new QName("", "indent");

        /// <summary>QName identifying the serialization parameter "media-type".</summary>

        public static readonly QName MEDIA_TYPE =
            new QName("", "media-type");

        /// <summary>QName identifying the serialization parameter "normalization-form"</summary>

        public static readonly QName NORMALIZATION_FORM =
            new QName("", "normalization-form");

        /// <summary>QName identifying the serialization parameter "omit-xml-declaration".
        /// The value is the string "yes" or "no".</summary>

        public static readonly QName OMIT_XML_DECLARATION =
            new QName("", "omi-xml-declaration");

        /// <summary>QName identifying the serialization parameter "standalone".
        /// The value is the string "yes" or "no" or "omit".</summary>

        public static readonly QName STANDALONE =
            new QName("", "standalone");

        /// <summary>QName identifying the serialization parameter "undeclare-prefixes".
        /// The value is the string "yes" or "no".</summary>

        public static readonly QName UNDECLARE_PREFIXES =
            new QName("", "undeclare-prefixes");

        /// <summary>QName identifying the serialization parameter "use-character-maps".
        /// This is available only with XSLT. The value of the parameter is a list of expanded QNames
        /// in Clark notation giving the names of character maps defined in the XSLT stylesheet.</summary>

        public static readonly QName USE_CHARACTER_MAPS =
            new QName("", "use-character-maps");

        /// <summary>QName identifying the serialization parameter "version"</summary>

        public static readonly QName VERSION =
            new QName("", "version");

        private const String SAXON = NamespaceConstant.SAXON;


        /// <summary>QName identifying the serialization parameter "saxon:character-representation"</summary>


        public static readonly QName SAXON_CHARACTER_REPRESENTATION =
            new QName(SAXON, "saxon:character-representation");

        /// <summary>QName identifying the serialization parameter "saxon:indent-spaces". The value
        /// is an integer (represented as a string) indicating the amount of indentation required.
        /// If specified, this parameter overrides indent="no".</summary>

        public static readonly QName SAXON_INDENT_SPACES =
            new QName(SAXON, "saxon:indent-spaces");

        /// <summary>QName identifying the serialization parameter "saxon:next-in-chain". This
        /// is available only with XSLT, and identifies the URI of a stylesheet that is to be used to
        /// process the results before passing them to their final destination.</summary>

        public static readonly QName NEXT_IN_CHAIN =
            new QName(SAXON, "saxon:next-in-chain");

        /// <summary>QName identifying the serialization parameter "require-well-formed". The
        /// value is the string "yes" or "no". If set to "yes", the output must be a well-formed
        /// document, or an error will be reported.</summary>

        public static readonly QName SAXON_REQUIRE_WELL_FORMED =
            new QName(SAXON, "saxon:require-well-formed");


        /// <summary>Create a Serializer</summary>

        public Serializer() {
        }

        /// <summary>Set a serialization property</summary>
        /// <remarks>In the case of XSLT, properties set within the serializer override
        /// any properties set in <c>xsl:output</c> declarations in the stylesheet.
        /// Similarly, with XQuery, they override any properties set in the Query
        /// prolog using <c>declare option saxon:output</c>.</remarks>
        /// <example>
        ///   <code>
        ///     Serializer qout = new Serializer();
        ///     qout.SetOutputProperty(Serializer.METHOD, "xml");
        ///     qout.SetOutputProperty(Serializer.INDENT, "yes");
        ///     qout.SetOutputProperty(Serializer.SAXON_INDENT_SPACES, "1");
        ///   </code>
        /// </example> 
        /// <param name="name">The name of the serialization property to be set</param>
        /// <param name="value">The value to be set for the serialization property. May be null
        /// to unset the property (that is, to set it back to the default value).</param>

        public void SetOutputProperty(QName name, String value) {
            props.setProperty(name.ClarkName, value);
        }

        /// <summary>Specify the destination of the serialized output, in the
        /// form of a file name</summary>
        /// <param name="filename">The name of the file to receive the serialized output</param>
        /// <exception>Throws a <c>DyamicError</c> if it is not possible to create an output
        /// stream to write to this file, for example, if the filename is in a directory
        /// that does not exist.</exception>

        public void SetOutputFile(String filename) {
            try {
                outputStream = new JFileOutputStream(filename);
                mustClose = true;
            } catch (java.io.IOException err) {
                JDynamicError e = new JDynamicError(err);
                throw new DynamicError(e);
            }
        }

        /// <summary>Specify the destination of the serialized output, in the
        /// form of a <c>Stream</c></summary>
        /// <param name="stream">The stream to which the output will be written.
        /// This must be a stream that allows writing.</param>

        public void SetOutputStream(Stream stream) {
            outputStream = new DotNetOutputStream(stream);
            mustClose = false;
        }

        /// <summary>Specify the destination of the serialized output, in the
        /// form of a <c>TextWriter</c></summary>
        /// <remarks>Note that when writing to a <c>TextWriter</c>, character encoding is
        /// the responsibility of the <c>TextWriter</c>, not the <c>Serializer</c>. This
        /// means that the encoding requested in the output properties is ignored; it also
        /// means that characters that cannot be represented in the target encoding will
        /// use whatever fallback representation the <c>TextWriter</c> defines, rather than
        /// being represented as XML character references.</remarks>
        /// <param name="textWriter">The stream to which the output will be written.
        /// This must be a stream that allows writing.</param>

        public void SetOutputWriter(TextWriter textWriter) {
            writer = new DotNetWriter(textWriter);
            mustClose = false;
        }

        internal JReceiver GetReceiver(JConfiguration config) {
            return config.getSerializerFactory().getReceiver(
                    GetResult(),
                    config.makePipelineConfiguration(),
                    GetOutputProperties());

        }


        /// <summary inherit="yes"/>

        public override JResult GetResult() {
            if (outputStream != null) {
                return new JStreamResult(outputStream);
            } else if (writer != null) {
                return new JStreamResult(writer);
            } else {
                return new JStreamResult(new DotNetWriter(Console.Out));
            }
        }

        /// <summary inherit="yes"/>

        public override JProperties GetOutputProperties() {
            return props;
        }

        /// <summary inherit="yes"/>

        public override void Close() {
            if (mustClose) {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (writer != null) {
                    writer.close();
                }
            }
        }
    }

    /// <summary>
    /// A <c>DomDestination</c> represents an XmlDocument that is constructed to hold the
    /// output of a query or transformation.
    /// </summary>
    /// <remarks>
    /// No data needs to be supplied to the DomDestination object. The query or transformation
    /// populates an XmlDocument, which may then be retrieved as the value of the <c>XmlDocument</c>
    /// property
    /// </remarks>

    public class DomDestination : XmlDestination {

        internal DotNetDomBuilder builder;

        /// <summary>Construct a DomDestination</summary>

        public DomDestination() {
            builder = new DotNetDomBuilder();
        }

        /// <summary>After construction, retrieve the constructed document node</summary>

        public XmlDocument XmlDocument {
            get { return builder.getDocumentNode(); }
        }

        /// <summary inherit="yes"/>

        public override JResult GetResult() {
            return builder;
        }
    }

    /// <summary>
    /// A <c>TextWriterDestination</c> is an implementation of <c>XmlDestination</c> that wraps
    /// an instance of <c>XmlTextWriter</c>.
    /// </summary>
    /// <remarks>
    /// Note that when a <c>TextWriterDestination</c> is used to process the output of a stylesheet
    /// or query, the output format depends only on the way the underlying <c>TextWriter</c>
    /// is configured; serialization parameters present in the stylesheet or query are ignored.
    /// </remarks>

    public class TextWriterDestination : XmlDestination {

        internal XmlTextWriter writer;

        /// <summary>Construct a TextWriterDestination</summary>
        /// <param name="writer">The <c>XmlTextWriter</c> that is to be notified of the events
        /// representing the XML document.</param>

        public TextWriterDestination(XmlTextWriter writer) {
            this.writer = writer;
        }

        /// <summary inherit="yes"/>

        public override JResult GetResult() {
            DotNetReceiver dnr = new DotNetReceiver(writer);
            return dnr;
            //net.sf.saxon.@event.TracingFilter filter = new net.sf.saxon.@event.TracingFilter();
            //filter.setUnderlyingReceiver(dnr);
            //return filter;
        }
    }


    /// <summary>
    /// An <c>XdmDestination</c> represents an XdmNode that is constructed to hold the
    /// output of a query or transformation: that is, a tree using Saxon's implementation of the
    /// XDM data model
    /// </summary>
    /// <remarks>
    /// <para>No data needs to be supplied to the XdmDestination object. The query or transformation
    /// populates an XmlNode, which may then be retrieved as the value of the <c>XmlNode</c>
    /// property.</para>
    /// <para>An XdmDestination can be reused to hold the results of a second transformation only
    /// if the <c>reset</c> method is first called to reset its state.</para>
    /// </remarks>

    public class XdmDestination : XmlDestination {

        internal JTinyBuilder builder;

        /// <summary>Construct a DomDestination</summary>

        public XdmDestination() {
            builder = new JTinyBuilder();
        }

        /// <summary>Reset the state of the XdmDestination so that it can be used to hold
        /// the result of another transformation.</summary>

        public void Reset() {
            builder = new JTinyBuilder();
        }

        /// <summary>After construction, retrieve the constructed document node</summary>

        public XdmNode XdmNode {
            get {
                return (XdmNode)XdmValue.Wrap(builder.getCurrentRoot());
            }
        }

        /// <summary inherit="yes"/>

        public override JResult GetResult() {
            return builder;
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