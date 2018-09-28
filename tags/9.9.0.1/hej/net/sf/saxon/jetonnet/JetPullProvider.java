package net.sf.saxon.jetonnet;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.*;
import net.sf.saxon.option.cpp.SaxonCAPI;
import net.sf.saxon.pull.PullProvider;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.pull.UnparsedEntity;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.CompressedWhitespace;
import net.sf.saxon.tree.util.AttributeCollectionImpl;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.AtomicValue;

import javax.xml.transform.Source;
import java.util.List;

/**
 * Created by ond1 on 09/03/2018.
 */
public class JetPullProvider implements PullProvider {

    private PipelineConfiguration pipe;
    private long parserPtr;
    private int parserPtrInt;
    private Object parser;
    private String baseURI;
    private boolean isEmptyElement = false;
    private int current = START_OF_INPUT;
    private boolean expandDefaults = true;


    public JetPullProvider(Object parser, long parserPtr, String bURI) {
        if (SaxonCAPI.debug) {
            System.err.println("Java: Parser pointer long value="+parserPtr);
        }
        this.parser = parser;
        this.parserPtr = parserPtr;
        this.parserPtrInt = (int)parserPtr;
        baseURI = bURI;
    }

    public static XdmNode Build(Processor processor, Object parser, long parserPtr, String baseUri) throws XPathException {


        Configuration config = processor.getUnderlyingConfiguration();

        Source source;
        ParseOptions options = new ParseOptions(config.getParseOptions());

        source = new PullSource(new JetPullProvider(parser, parserPtr, baseUri));
        source.setSystemId(baseUri);


        NodeInfo doc = config.buildDocumentTree(source, options).getRootNode();
        return (XdmNode) XdmValue.wrap(doc);


    }


    public long getParserPointerValue(){
        return parserPtr;
    }

    public Object getPointerObject(){
        return parser;
    }

    @Override
    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        final Configuration config = pipe.getConfiguration();
        expandDefaults = config.isExpandAttributeDefaults();
    }

    @Override
    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    // public native void next(Object parser);

    public native boolean Read(Object parseri);

    public native int get_NodeType(long parserPtri);

    public native boolean get_IsEmptyElement(long parserPtri);

    public native boolean get_EOF(long parserPtri);

    public native int get_Depth(long parserPtri);

    public native void parser_close(long parserPtri);

    public native String[] get_Attributes(long parserPtri, boolean expandDefault);

    public native String get_Value(long parserPtri);

    public native String get_NodeName(long parserPtr);

    @Override
    public int next() throws XPathException {

        if (SaxonCAPI.debug) {
            System.err.println("Java: next(), current = " + current + " empty: " + isEmptyElement);
        }
        if (current == START_OF_INPUT) {
            current = START_DOCUMENT;
            return current;
        } else if (current == END_DOCUMENT || current == END_OF_INPUT) {
            current = END_OF_INPUT;
            return current;
        } else if (current == START_ELEMENT && isEmptyElement) {
            current = END_ELEMENT;
            return current;
        }

        do {

            Read(parser);

            //noinspection ConstantIfStatement
            if (false) {
                XPathException de = new XPathException("Error reported by XML parser: dummy");
                throw de;

            }
            //noinspection ConstantIfStatement
            if (false) {
                XPathException de = new XPathException("Validation error reported by XML parser: ");

                throw de;

            }

            int intype = get_NodeType(parserPtr);
            isEmptyElement = get_IsEmptyElement(parserPtr);
            if (SaxonCAPI.debug) {
                System.err.println("Java: Next event: " + intype + " at depth " + get_Depth(parserPtr) + " empty: " + isEmptyElement + "," + get_IsEmptyElement(parserPtr));
            }
            if (get_EOF(parserPtr)) {
                current = END_DOCUMENT;
                return current;
            }
            if (intype == JetXmlNodeType.EntityReference) {
                //parser.ResolveEntity();
                current = -1;
            } else {
                current = mapInputKindToOutputKind(intype);
                if (current == TEXT && get_Depth(parserPtr) == 0) {
                    current = -1;
                }
            }
        } while (current == -1);
        return current;
    }


    /**
     * Map the numbers used to identify events in the .NET XMLReader interface to the numbers used
     * by the Saxon PullProvider interface
     *
     * @param in the XMLReader event number
     * @return the Saxon PullProvider event number
     */

    private int mapInputKindToOutputKind(int in) {
        // Note: we are losing unparsedEntities - see test expr02. Apparently unparsed entities are not
        // available via an XMLValidatingReader. We would have to build a DOM to get them, and that's too high
        // a price to pay.
        switch (in) {
            case JetXmlNodeType.Attribute:
                return PullProvider.ATTRIBUTE;
            case JetXmlNodeType.CDATA:
                return PullProvider.TEXT;
            case JetXmlNodeType.Comment:
                return PullProvider.COMMENT;
            case JetXmlNodeType.Document:
                return PullProvider.START_DOCUMENT;
            case JetXmlNodeType.DocumentFragment:
                return PullProvider.START_DOCUMENT;
            case JetXmlNodeType.Element:
                return PullProvider.START_ELEMENT;
            case JetXmlNodeType.EndElement:
                return PullProvider.END_ELEMENT;
            case JetXmlNodeType.ProcessingInstruction:
                return PullProvider.PROCESSING_INSTRUCTION;
            case JetXmlNodeType.SignificantWhitespace:
                //System.err.println("Significant whitespace");
                return PullProvider.TEXT;
            case JetXmlNodeType.Text:
                return PullProvider.TEXT;
            case JetXmlNodeType.Whitespace:
                //System.err.println("Plain whitespace");
                return PullProvider.TEXT;
            //return -1;
            default:
                return -1;
        }
    }

    @Override
    public int current() {
        return current;
    }

    /**
     * Return the system identifier for the current document event.
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     * if none is available.
     */
    public String getSystemId() {
        String base = null;//parser.get_BaseURI();
        if (base == null || base.isEmpty()) {
            return baseURI;
        } else {
            return base;
        }
    }

    @Override
    public AttributeCollection getAttributes() throws XPathException {

        String[] attributes = get_Attributes(parserPtr, expandDefaults);

        if (attributes != null && attributes.length > 0) {
            int attrLen = attributes.length;
            AttributeCollectionImpl atts = new AttributeCollectionImpl(pipe.getConfiguration());
            for (int i = 0; i < attrLen; i++) {
                String result = attributes[i];
                int openingBracket = result.indexOf("{");
                int closingBracket = result.indexOf("}");
                int valueIndex = result.indexOf("=");
                String prefix = result.substring(0, openingBracket);
                String namespaceURI = result.substring(result.indexOf("{") + 1, closingBracket);
                String localName = result.substring(closingBracket + 1, valueIndex);
                String value = result.substring(valueIndex + 1);
                if (SaxonCAPI.debug) {
                    System.err.println("Java: getNodeName prefix= " + prefix + ", namespace=" + namespaceURI + ", localName=" + localName + " value=" + value);
                }
                NodeName nc = new FingerprintedQName(prefix, namespaceURI, localName);
                // .NET does not report the attribute type (even if it's an ID...)
                atts.addAttribute(nc, BuiltInAtomicType.UNTYPED_ATOMIC, value, ExplicitLocation.UNKNOWN_LOCATION, 0);
            }
            return atts;
        } else {
            return AttributeCollectionImpl.EMPTY_ATTRIBUTE_COLLECTION;
        }

    }

    @Override
    public NamespaceBinding[] getNamespaceDeclarations() throws XPathException {
        return new NamespaceBinding[0];
    }

    @Override
    public int skipToMatchingEnd() throws XPathException {
        return 0;
    }

    @Override
    public void close() {
        parser_close(parserPtr);
    }


    @Override
    public NodeName getNodeName() {
        String result = get_NodeName(parserPtr);
        int openingBracket = result.indexOf("{");
        int closingBracket = result.indexOf("}");
        String prefix = result.substring(0, openingBracket);
        String namespaceURI = result.substring(result.indexOf("{") + 1, closingBracket);
        String localName = result.substring(closingBracket + 1);
        if (SaxonCAPI.debug) {
            System.err.println("Java: getNodeName prefix= " + prefix + ", namespace=" + namespaceURI + ", localName=" + localName);
        }
        return new FingerprintedQName(prefix, namespaceURI, localName);
    }

    @Override
    public CharSequence getStringValue() throws XPathException {
        if (current == TEXT) {
            return CompressedWhitespace.compress(get_Value(parserPtr));
        } else {
            return get_Value(parserPtr);
        }
    }

    @Override
    public SchemaType getSchemaType() {
        return Untyped.getInstance();
    }

    @Override
    public AtomicValue getAtomicValue() {
        return null;
    }

    @Override
    public Location getSourceLocator() {
        return null;
    }

    @Override
    public List<UnparsedEntity> getUnparsedEntities() {
        return null;
    }
}
