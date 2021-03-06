////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;

import java.util.Arrays;

import static net.sf.saxon.event.RegularSequenceChecker.State.*;

/**
 * This class is used for generating complex content, that is, the content of an
 * element or document node. It enforces the rules on the order of events within
 * complex content (attributes and namespaces must come first), and it implements
 * part of the namespace fixup rules, in particular, it ensures that there is a
 * namespace node for the namespace used in the element name and in each attribute
 * name.
 * <p>
 * The same ComplexContentOutputter may be used for generating an entire XML
 * document; it is not necessary to create a new outputter for each element node.
 * <p>
 * From Saxon 9.9, the ComplexContentOutputter does not combine top-level events.
 * Unless nested within a startDocument/endDocument or startElement/endElement pair,
 * items such as atomic values, text nodes, attribute nodes, maps and arrays are
 * passed through unchanged to the output. It is typically the responsibility
 * of the Destination object to decide how to combine top-level events (whether
 * to build a single document, whether to insert item separators, etc).
 *
 *
 */

public final class ComplexContentOutputter extends SequenceReceiver {

    private Receiver nextReceiver;
    // the next receiver in the output pipeline

    private NodeName pendingStartTag = null;
    private int level = -1;
    // records the number of startDocument or startElement events
    // that have not yet been closed. Note that startDocument and startElement
    // events may be arbitrarily nested; startDocument and endDocument
    // are ignored unless they occur at the outermost level, except that they
    // still change the level number
    private boolean[] currentLevelIsDocument = new boolean[20];
    private InScopeNamespaces[] copyNamespacesStack = new InScopeNamespaces[20];
    private Boolean elementIsInNullNamespace;
    private NodeName[] pendingAttCode = new NodeName[20];
    private SimpleType[] pendingAttType = new SimpleType[20];
    private String[] pendingAttValue = new String[20];
    private Location[] pendingAttLocation = new Location[20];
    private int[] pendingAttProp = new int[20];
    private int pendingAttListSize = 0;

    private NamespaceBinding[] pendingNSList = new NamespaceBinding[20];
    private int pendingNSListSize = 0;

    private SchemaType currentSimpleType = null;  // any other value means we are currently writing an
    // element of a particular simple type

    private int startElementProperties;
    private Location startElementLocationId = ExplicitLocation.UNKNOWN_LOCATION;
    private boolean declaresDefaultNamespace;
    private int hostLanguage = Configuration.XSLT;

    private RegularSequenceChecker.State state = Initial;
    /**
     * Create a ComplexContentOutputter
     * @param next the next receiver in the pipeline
     */

    public ComplexContentOutputter(Receiver next) {
        super(next.getPipelineConfiguration());
        setReceiver(next);
        setHostLanguage(next.getPipelineConfiguration().getHostLanguage());
    }

    /**
     * Static factory method to create an push pipeline containing a ComplexContentOutputter
     * @param receiver the destination to which the constructed complex content will be written
     * @param options options for validating the output stream; may be null
     */

    public static Receiver makeComplexContentReceiver(Receiver receiver, ParseOptions options) {
//        System.err.println("CHANGE OUTPUT DESTINATION new=" + receiver);

        String systemId = receiver.getSystemId();
        boolean validate = options != null && options.getSchemaValidationMode() != Validation.PRESERVE;

        if (receiver instanceof ComplexContentOutputter && !validate) {
            return receiver;
        }

        // add a validator to the pipeline if required

        if (validate) {
            Configuration config = receiver.getPipelineConfiguration().getConfiguration();
            receiver = config.getDocumentValidator(receiver, systemId, options, null);
        }

        // add a filter to remove duplicate namespaces
        NamespaceReducer ne = new NamespaceReducer(receiver);
        ne.setSystemId(receiver.getSystemId());
        receiver = ne;

        receiver = new ComplexContentOutputter(ne);

        receiver.setSystemId(systemId);
        return receiver;
    }

    public void setPipelineConfiguration(/*@NotNull*/ PipelineConfiguration pipe) {
        if (pipelineConfiguration != pipe) {
            pipelineConfiguration = pipe;
            if (nextReceiver != null) {
                nextReceiver.setPipelineConfiguration(pipe);
            }
        }
    }

    @Override
    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        nextReceiver.setSystemId(systemId);
    }

    /**
     * Set the host language
     *
     * @param language the host language, for example {@link Configuration#XQUERY}
     */

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }


    /**
     * Set the receiver (to handle the next stage in the pipeline) directly
     *
     * @param receiver the receiver to handle the next stage in the pipeline
     */

    public void setReceiver(Receiver receiver) {
        this.nextReceiver = receiver;
    }

    /**
     * Get the next receiver in the processing pipeline
     *
     * @return the receiver which this ComplexContentOutputter writes to
     */

    public Receiver getReceiver() {
        return nextReceiver;
    }


    /**
     * Start the output process
     */

    public void open() throws XPathException {
        nextReceiver.open();
        previousAtomic = false;
        state = Open;
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        level++;
        if (level == 0) {
            nextReceiver.startDocument(properties);
        } else if (state == StartTag) {
            startContent();
        }
        previousAtomic = false;
        if (currentLevelIsDocument.length < level + 1) {
            currentLevelIsDocument = Arrays.copyOf(currentLevelIsDocument, level * 2);
        }
        currentLevelIsDocument[level] = true;
        state = Content;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (level == 0) {
            nextReceiver.endDocument();
        }
        previousAtomic = false;
        level--;
        state = level < 0 ? Open : Content;
    }

    /**
     * Notify an unparsed entity URI.
     *
     * @param name     The name of the unparsed entity
     * @param systemID The system identifier of the unparsed entity
     * @param publicID The public identifier of the unparsed entity
     */
    @Override
    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
        nextReceiver.setUnparsedEntity(name, systemID, publicID);
    }

    /**
     * Produce text content output. <BR>
     * Special characters are escaped using XML/HTML conventions if the output format
     * requires it.
     *
     * @param s The String to be output
     * @param locationId the location of the node in the source, or of the instruction that created it
     * @throws XPathException for any failure
     */

    public void characters(CharSequence s, Location locationId, int properties) throws XPathException {
        if (level >= 0) {
            previousAtomic = false;
            if (s == null) {
                return;
            }
            int len = s.length();
            if (len == 0) {
                return;
            }
            if (state == StartTag) {
                startContent();
            }
        }
        nextReceiver.characters(s, locationId, properties);
    }

    /**
     * Output an element start tag. <br>
     * The actual output of the tag is deferred until all attributes have been output
     * using attribute().
     *
     * @param elemName The element name
     * @param location the location of the element node (or the instruction that created it)
     */

    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
        //System.err.println("CCO " + this + "StartElement " + nameCode);
        level++;
        if (state == StartTag) {
            startContent();
        }
        startElementProperties = properties;
        startElementLocationId = location.saveLocation();
        pendingAttListSize = 0;
        pendingNSListSize = 0;
        pendingStartTag = elemName;
        //pendingStartTagDepth = 1;
        elementIsInNullNamespace = null; // meaning not yet computed
        declaresDefaultNamespace = false;
        currentSimpleType = typeCode;
        previousAtomic = false;
        if (currentLevelIsDocument.length < level + 1) {
            currentLevelIsDocument = Arrays.copyOf(currentLevelIsDocument, level * 2);
        }
        if (copyNamespacesStack.length < level + 1) {
            copyNamespacesStack = Arrays.copyOf(copyNamespacesStack, level * 2);
        }
        currentLevelIsDocument[level] = false;
        state = StartTag;
    }


    /**
     * Output one or more namespace declarations. <br>
     * This is added to a list of pending namespaces for the current start tag.
     * If there is already another declaration of the same prefix, this one is
     * ignored, unless the REJECT_DUPLICATES flag is set, in which case this is an error.
     * Note that unlike SAX2 startPrefixMapping(), this call is made AFTER writing the start tag.
     *
     * @param nsBindings The namespace bindings
     * @throws XPathException if there is no start tag to write to (created using writeStartTag),
     *                        or if character content has been written since the start tag was written.
     */

    public void namespace(NamespaceBindingSet nsBindings, int properties)
            throws XPathException {
        // Optimization for recursive shallow-copy added in 9.8 - see bug 3011.
        if (nsBindings instanceof InScopeNamespaces) {
            copyNamespacesStack[level] = (InScopeNamespaces)nsBindings;
            if (level > 0 && copyNamespacesStack[level - 1] != null &&
                    copyNamespacesStack[level - 1].getElement().equals(((InScopeNamespaces)nsBindings).getElement())) {
                // Ignore these namespaces if they are the same as the namespaces for the parent element
                return;
            }
        }

        // System.err.println("Write namespace prefix=" + (nscode>>16) + " uri=" + (nscode&0xffff));
        for (NamespaceBinding ns : nsBindings) {
            if (state != StartTag) {
                throw NoOpenStartTagException.makeNoOpenStartTagException(
                        Type.NAMESPACE,
                        ns.getPrefix(),
                        hostLanguage,
                        currentLevelIsDocument[level],
                    startElementLocationId);
            }

            // elimination of namespaces already present on an outer element of the
            // result tree is done by the NamespaceReducer.

            // Handle declarations whose prefix is duplicated for this element.

            boolean rejectDuplicates = (properties & ReceiverOptions.REJECT_DUPLICATES) != 0;

            for (int i = 0; i < pendingNSListSize; i++) {
                if (ns.getPrefix().equals(pendingNSList[i].getPrefix())) {
                    if (ns.getURI().equals(pendingNSList[i].getURI())) {
                        return; // duplicate namespace, no action needed
                    }
                    if (pendingNSList[i].isDefaultUndeclaration() || ns.isDefaultUndeclaration()) {
                        // xmlns="" overridden by xmlns="abc"
                        pendingNSList[i] = ns;
                    } else if (rejectDuplicates) {
                        String prefix = ns.getPrefix();
                        String uri1 = ns.getURI();
                        String uri2 = pendingNSList[i].getURI();
                        XPathException err = new XPathException("Cannot create two namespace nodes with the same prefix mapped to different URIs (prefix=" +
                                (prefix.isEmpty() ? "\"\"" : prefix) + ", URI=" +
                                (uri1.isEmpty() ? "\"\"" : uri1) + ", URI=" +
                                (uri2.isEmpty() ? "\"\"" : uri2) + ")");
                        err.setErrorCode(hostLanguage == Configuration.XSLT ? "XTDE0430" : "XQDY0102");
                        throw err;
                    } else {
                        // same prefix, do a quick exit
                        return;
                    }
                }
            }

            // It is an error to output a namespace node for the default namespace if the element
            // itself is in the null namespace, as the resulting element could not be serialized

            if (ns.getPrefix().isEmpty() && !ns.getURI().isEmpty()) {
                declaresDefaultNamespace = true;
                if (elementIsInNullNamespace == null) {
                    elementIsInNullNamespace = pendingStartTag.hasURI("");
                }
                if (elementIsInNullNamespace) {
                    XPathException err = new XPathException("Cannot output a namespace node for the default namespace when the element is in no namespace");
                    err.setErrorCode(hostLanguage == Configuration.XSLT ? "XTDE0440" : "XQDY0102");
                    throw err;
                }
            }

            // if it's not a duplicate namespace, add it to the list for this start tag

            if (pendingNSListSize + 1 > pendingNSList.length) {
                pendingNSList = Arrays.copyOf(pendingNSList, pendingNSListSize * 2);
            }
            pendingNSList[pendingNSListSize++] = ns;
            previousAtomic = false;
        }
    }


    /**
     * Output an attribute value. <br>
     * This is added to a list of pending attributes for the current start tag, overwriting
     * any previous attribute with the same name. <br>
     * This method should NOT be used to output namespace declarations.<br>
     *
     * @param attName    The name of the attribute
     * @param value      The value of the attribute
     * @param locationId the location of the node in the source, or of the instruction that created it
     *@param properties Bit fields containing properties of the attribute to be written  @throws XPathException if there is no start tag to write to (created using writeStartTag),
     *                        or if character content has been written since the start tag was written.
     */

    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, Location locationId, int properties) throws XPathException {
        //System.err.println("Write attribute " + attName + "=" + value + " to Outputter " + this);
        if (level >= 0 && state != StartTag) {
            // The complexity here is in identifying the right error message and error code

            XPathException err = NoOpenStartTagException.makeNoOpenStartTagException(
                    Type.ATTRIBUTE,
                    attName.getDisplayName(),
                    hostLanguage,
                    currentLevelIsDocument[level],
                startElementLocationId);
            err.setLocator(locationId);
            throw err;
        }

        // if this is a duplicate attribute, overwrite the original in XSLT; throw an error in XQuery.
        // No check needed if the NOT_A_DUPLICATE property is set (typically, during a deep copy operation)

        if (level >= 0 && ((properties & ReceiverOptions.NOT_A_DUPLICATE)==0)) {
            for (int a = 0; a < pendingAttListSize; a++) {
                if (pendingAttCode[a].equals(attName)) {
                    if (hostLanguage == Configuration.XSLT) {
                        pendingAttType[a] = typeCode;
                        pendingAttValue[a] = value.toString();
                        // we have to copy the CharSequence, because some kinds of CharSequence are mutable.
                        pendingAttLocation[a] = locationId;
                        pendingAttProp[a] = properties;
                        return;
                    } else {
                        XPathException err = new XPathException("Cannot create an element having two attributes with the same name: " +
                                Err.wrap(attName.getDisplayName(), Err.ATTRIBUTE));
                        err.setErrorCode("XQDY0025");
                        throw err;
                    }
                }
            }
        }

        // for top-level attributes (attributes whose parent element is not being copied),
        // check that the type annotation is not namespace-sensitive (because the namespace context might
        // be different, and we don't do namespace fixup for prefixes in content: see bug 4151

        if (level == 0 && !typeCode.equals(BuiltInAtomicType.UNTYPED_ATOMIC) /**/ && currentLevelIsDocument[0] /**/) {
            // commenting-out in line above done MHK 22 Jul 2011 to pass test Constr-cont-nsmode-8
            // reverted 2011-07-27 to pass tests in qischema family
            if (typeCode.isNamespaceSensitive()) {
                XPathException err = new XPathException("Cannot copy attributes whose type is namespace-sensitive (QName or NOTATION): " +
                        Err.wrap(attName.getDisplayName(), Err.ATTRIBUTE));
                err.setErrorCode(hostLanguage == Configuration.XSLT ? "XTTE0950" : "XQTY0086");
                throw err;
            }
        }

        // push top-level attribute nodes down the pipeline
        if (level < 0) {
            nextReceiver.attribute(attName, typeCode, value, locationId, properties);
        }

        // otherwise, add this one to the list

        if (pendingAttListSize >= pendingAttCode.length) {
            pendingAttCode = Arrays.copyOf(pendingAttCode, pendingAttListSize * 2);
            pendingAttType = Arrays.copyOf(pendingAttType, pendingAttListSize * 2);
            pendingAttValue = Arrays.copyOf(pendingAttValue, pendingAttListSize * 2);
            pendingAttLocation = Arrays.copyOf(pendingAttLocation, pendingAttListSize * 2);
            pendingAttProp = Arrays.copyOf(pendingAttProp, pendingAttListSize * 2);
        }

        pendingAttCode[pendingAttListSize] = attName;
        pendingAttType[pendingAttListSize] = typeCode;
        pendingAttValue[pendingAttListSize] = value.toString();
        pendingAttLocation[pendingAttListSize] = locationId;
        pendingAttProp[pendingAttListSize] = properties;
        pendingAttListSize++;
        previousAtomic = false;
    }

    /**
     * Check that the prefix for an element or attribute is acceptable, allocating a substitute
     * prefix if not. The prefix is acceptable unless a namespace declaration has been
     * written that assignes this prefix to a different namespace URI. This method
     * also checks that the element or attribute namespace has been declared, and declares it
     * if not.
     *
     * @param nodeName the proposed name, including proposed prefix
     * @param seq      sequence number, used for generating a substitute prefix when necessary.
     *                 The value 0 is used for element names; values greater than 0 are used
     *                 for attribute names.
     * @return a nameCode to use in place of the proposed nameCode (or the original nameCode
     *         if no change is needed)
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs writing the new
     *          namespace node
     */

    private NodeName checkProposedPrefix(NodeName nodeName, int seq) throws XPathException {
        NamespaceBinding binding = nodeName.getNamespaceBinding();
        String nsprefix = binding.getPrefix();

        for (int i = 0; i < pendingNSListSize; i++) {
            if (nsprefix.equals(pendingNSList[i].getPrefix())) {
                // same prefix
                if (binding.getURI().equals(pendingNSList[i].getURI())) {
                    // same URI
                    return nodeName;    // all is well
                } else {
                    String prefix = getSubstitutePrefix(binding, seq);
                    NodeName newName = new FingerprintedQName(prefix, nodeName.getURI(), nodeName.getLocalPart());
                    namespace(newName.getNamespaceBinding(), 0);
                    return newName;
                }
            }
        }
        // no declaration of this prefix: declare it now
        if (seq > 0 && nsprefix.isEmpty()) {
            // This is an attribute and the prefix is "" - need to invent a prefix
            // See bug 3068 and unit test ParserTest/testXercesSchemaDefaultedAttributes
            String prefix = getSubstitutePrefix(binding, seq);
            NodeName newName = new FingerprintedQName(prefix, nodeName.getURI(), nodeName.getLocalPart());
            namespace(newName.getNamespaceBinding(), 0);
            return newName;
        }
        namespace(binding, 0);
        return nodeName;
    }

    /**
     * It is possible for a single output element to use the same prefix to refer to different
     * namespaces. In this case we have to generate an alternative prefix for uniqueness. The
     * one we generate is based on the sequential position of the element/attribute: this is
     * designed to ensure both uniqueness (with a high probability) and repeatability
     *
     * @param nscode the proposed namespace code
     * @param seq    sequence number for use in the substitute prefix
     * @return a prefix to use in place of the one originally proposed
     */

    private String getSubstitutePrefix(NamespaceBinding nscode, int seq) {
        if (nscode.getURI().equals(NamespaceConstant.XML)) {
            return "xml";
        }
        return nscode.getPrefix() + '_' + seq;
    }

    /**
     * Output an element end tag.
     */

    public void endElement() throws XPathException {
        //System.err.println("Write end tag " + this + " : " + name);
        if (state == StartTag) {
            startContent();
        } else {
            //pendingStartTagDepth = -2;
            pendingStartTag = null;
        }

        // write the end tag

        nextReceiver.endElement();
        level--;
        previousAtomic = false;
        state = level < 0 ? Open : Content;
    }

    /**
     * Write a comment
     */

    public void comment(CharSequence comment, Location locationId, int properties) throws XPathException {
        if (level >= 0) {
            if (state == StartTag) {
                startContent();
            }
            previousAtomic = false;
        }
        nextReceiver.comment(comment, locationId, properties);
    }

    /**
     * Write a processing instruction
     */

    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        if (level >= 0) {
            if (state == StartTag) {
                startContent();
            }
            previousAtomic = false;
        }
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     *
     * @param item           the item to be appended
     * @param locationId     the location of the calling instruction, for diagnostics
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
     *                       need to be copied. Values are {@link ReceiverOptions#ALL_NAMESPACES},
     *                       {@link ReceiverOptions#LOCAL_NAMESPACES}; the default (0) means
     *                       no namespaces
     */

    public void append(Item item, Location locationId, int copyNamespaces) throws XPathException {
        // Decompose the item into a sequence of node events if we're within a start/end element/document
        // pair. Otherwise, send the item down the pipeline unchanged: it's the job of the Destination
        // to deal with it (inserting item separators if appropriate)
        if (level >= 0) {
            decompose(item, locationId, copyNamespaces);
        } else {
            nextReceiver.append(item, locationId, copyNamespaces);
        }
    }

    /**
     * Close the output
     */

    public void close() throws XPathException {
        // System.err.println("Close " + this + " using emitter " + emitter.getClass());
        nextReceiver.close();
        previousAtomic = false;
        state = Final;
    }

    /**
     * Flush out a pending start tag
     */

    public void startContent() throws XPathException {

        if (state != StartTag) {
            // this can happen if the method is called from outside,
            // e.g. from a SequenceOutputter earlier in the pipeline
            return;
        }

        NodeName elcode = checkProposedPrefix(pendingStartTag, 0);
        int props = startElementProperties | ReceiverOptions.NAMESPACE_OK;
        nextReceiver.startElement(elcode, currentSimpleType, startElementLocationId, props);

        for (int a = 0; a < pendingAttListSize; a++) {
            NodeName attcode = pendingAttCode[a];
            if (!attcode.hasURI("")) {    // non-null prefix
                attcode = checkProposedPrefix(attcode, a + 1);
                pendingAttCode[a] = attcode;
            }
        }

        for (int n = 0; n < pendingNSListSize; n++) {
            nextReceiver.namespace(pendingNSList[n], 0);
        }

        for (int a = 0; a < pendingAttListSize; a++) {
            nextReceiver.attribute(pendingAttCode[a],
                    pendingAttType[a],
                    pendingAttValue[a],
                    pendingAttLocation[a],
                    pendingAttProp[a]);
        }

        nextReceiver.startContent();

        pendingAttListSize = 0;
        pendingNSListSize = 0;
        //pendingStartTagDepth = -1;
        previousAtomic = false;
        state = Content;
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     *
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return nextReceiver.usesTypeAnnotations();
    }

    public boolean isReadyForGrafting() {
        Receiver r2 = getReceiver();
        if (r2 instanceof NamespaceReducer) {
            if (!((NamespaceReducer) r2).isDisinheritingNamespaces()) {
                Receiver r3 = ((NamespaceReducer) r2).getNextReceiver();
                return r3 instanceof TinyBuilder &&
                        ((state == StartTag &&
                                  (startElementProperties & ReceiverOptions.DISINHERIT_NAMESPACES) == 0)
                                 || ((TinyBuilder) r3).isPositionedAtElement());
            }
        }
        return false;
    }

    public void graftElementNode(TinyElementImpl elementNode, int copyOptions) throws XPathException {
        NamespaceReducer r2 = (NamespaceReducer)getReceiver();
        TinyBuilder target = (TinyBuilder)r2.getNextReceiver();
        beforeBulkCopy();
        boolean copyNamespaces = CopyOptions.includes(copyOptions, CopyOptions.ALL_NAMESPACES);
        target.graft(elementNode, copyNamespaces);
        afterBulkCopy();
    }

    public boolean isReadyForBulkCopy() {
        Receiver r2 = getReceiver();
        if (r2 instanceof NamespaceReducer) {
            if (!((NamespaceReducer) r2).isDisinheritingNamespaces()) {
                Receiver r3 = ((NamespaceReducer) r2).getNextReceiver();
                return r3 instanceof TinyBuilder &&
                        ((state == StartTag &&
                                  (startElementProperties & ReceiverOptions.DISINHERIT_NAMESPACES) == 0)
                                 || ((TinyBuilder) r3).isPositionedAtElement());
            }
        }
        return false;
    }

    public void bulkCopyElementNode(TinyElementImpl elementNode, int copyOptions) throws XPathException {
        NamespaceReducer r2 = (NamespaceReducer) getReceiver();
        TinyBuilder target = (TinyBuilder) r2.getNextReceiver();
        beforeBulkCopy();
        boolean copyNamespaces = CopyOptions.includes(copyOptions, CopyOptions.ALL_NAMESPACES);
        target.bulkCopy(elementNode, copyNamespaces);
        afterBulkCopy();
    }

    private void beforeBulkCopy() throws XPathException {
        level++;
        if (state == StartTag) {
            startContent();
        }
    }

    private void afterBulkCopy() {
        level--;
        previousAtomic = false;
    }
}

