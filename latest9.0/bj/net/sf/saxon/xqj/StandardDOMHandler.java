package net.sf.saxon.xqj;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.dom.NodeOverNodeInfo;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Sender;
import net.sf.saxon.javax.xml.xquery.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.pull.StaxBridge;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.*;
import org.w3c.dom.Node;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Implementation of XQCommonHandler that performs the mappings between Java and XPath
 * as defined in the XQJ specification. This is the handler that is used by default.
 * <p>
 * This handler implements the mappings defined in the XQJ specification. In addition,
 * it defines the following mappings, which are applied after those defined in XQJ:</p>
 *
 * <p>For fromObject:</p>
 * <ul>
 * <li>If the supplied object is an instance of javax.xml.transform.Source, a document
 * node is constructed from the source and the resulting node is returned as the Item</li>
 * <li>If the supplied object is an instance of javax.xml.stream.XMLStreamReader, a document
 * node is constructed from the XMLStreamReader and the resulting node is returned as the Item</li>*
 * </ul>
 */
public class StandardDOMHandler  {

    SaxonXQDataFactory dataFactory;
    Configuration config;

    public StandardDOMHandler(SaxonXQDataFactory factory) {
        dataFactory = factory;
        config = factory.getConfiguration();
    }

    public XQItem fromObject(Object obj) throws XQException {
        return new SaxonXQItem(convertToItem(obj), dataFactory);
    }

    //@SuppressWarnings({"ConstantConditions"})
    public Object toObject(XQItemAccessor item) throws XQException {
        if (item instanceof AtomicValue) {
            AtomicValue p = ((AtomicValue)item);
            int t = p.getItemType(config.getTypeHierarchy()).getPrimitiveType();
            switch (t) {
                case StandardNames.XS_ANY_URI:
                    return p.getStringValue();
                case StandardNames.XS_BASE64_BINARY:
                    return ((Base64BinaryValue)p).getBinaryValue();
                case StandardNames.XS_BOOLEAN:
                    return Boolean.valueOf(((BooleanValue)p).getBooleanValue());
                case StandardNames.XS_DATE:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DATE_TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DECIMAL:
                    return ((DecimalValue)p).getDecimalValue();
                case StandardNames.XS_DOUBLE:
                    return new Double(((DoubleValue)p).getDoubleValue());
                case StandardNames.XS_DURATION:
                    return new SaxonDuration((DurationValue)p);
                case StandardNames.XS_FLOAT:
                    return new Float(((FloatValue)p).getFloatValue());
                case StandardNames.XS_G_DAY:
                case StandardNames.XS_G_MONTH:
                case StandardNames.XS_G_MONTH_DAY:
                case StandardNames.XS_G_YEAR:
                case StandardNames.XS_G_YEAR_MONTH:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_HEX_BINARY:
                    return ((HexBinaryValue)p).getBinaryValue();
                case StandardNames.XS_INTEGER:
                    if (p instanceof BigIntegerValue) {
                        return ((BigIntegerValue)p).asBigInteger();
                    } else {
                        int sub = ((AtomicType)p.getItemType(null)).getFingerprint();
                        switch (sub) {
                            case StandardNames.XS_INTEGER:
                            case StandardNames.XS_NEGATIVE_INTEGER:
                            case StandardNames.XS_NON_NEGATIVE_INTEGER:
                            case StandardNames.XS_NON_POSITIVE_INTEGER:
                            case StandardNames.XS_POSITIVE_INTEGER:
                            case StandardNames.XS_UNSIGNED_LONG:
                                return BigInteger.valueOf(((Int64Value)p).longValue());
                            case StandardNames.XS_BYTE:
                                return new Byte((byte)((Int64Value)p).longValue());
                            case StandardNames.XS_INT:
                            case StandardNames.XS_UNSIGNED_SHORT:
                                return new Integer((int)((Int64Value)p).longValue());
                            case StandardNames.XS_LONG:
                            case StandardNames.XS_UNSIGNED_INT:
                                return new Long(((Int64Value)p).longValue());
                            case StandardNames.XS_SHORT:
                            case StandardNames.XS_UNSIGNED_BYTE:
                                return new Short((short)((Int64Value)p).longValue());
                            default:
                                throw new XQException("Unrecognized integer subtype " + sub);
                        }
                    }
                case StandardNames.XS_QNAME:
                    return ((QualifiedNameValue)p).makeQName(config);
                case StandardNames.XS_STRING:
                    return p.getStringValue();
                case StandardNames.XS_TIME:
                    return new SaxonXMLGregorianCalendar((CalendarValue)p);
                case StandardNames.XS_DAY_TIME_DURATION:
                    return new SaxonDuration((DurationValue)p);
                case StandardNames.XS_YEAR_MONTH_DURATION:
                    return new SaxonDuration((DurationValue)p);
                default:
                    throw new XQException("unsupported type");
            }
        } else {
            return NodeOverNodeInfo.wrap((NodeInfo)item);
        }
    }

    public Item convertToItem(Object value) throws XQException {
        try {
            if (value instanceof Boolean) {
                return BooleanValue.get(((Boolean)value).booleanValue());
            } else if (value instanceof byte[]) {
                return new HexBinaryValue((byte[])value);
            } else if (value instanceof Byte) {
                return new Int64Value(((Byte)value).byteValue(), BuiltInAtomicType.BYTE, false);
            } else if (value instanceof Float) {
                return new FloatValue(((Float)value).floatValue());
            } else if (value instanceof Double) {
                return new DoubleValue(((Double)value).doubleValue());
            } else if (value instanceof Integer) {
                return new Int64Value(((Integer)value).intValue(), BuiltInAtomicType.INT, false);
            } else if (value instanceof Long) {
                return new Int64Value(((Long)value).longValue(), BuiltInAtomicType.LONG, false);
            } else if (value instanceof Short) {
                return new Int64Value(((Short)value).shortValue(), BuiltInAtomicType.SHORT, false);
            } else if (value instanceof String) {
                return new UntypedAtomicValue((String)value);
            } else if (value instanceof BigDecimal) {
                return new DecimalValue((BigDecimal)value);
            } else if (value instanceof BigInteger) {
                return new BigIntegerValue((BigInteger)value);
            } else if (value instanceof Duration) {
                // this is simpler and safer (but perhaps slower) than extracting all the components
                return DurationValue.makeDuration(value.toString()).asAtomic();
            } else if (value instanceof XMLGregorianCalendar) {
                XMLGregorianCalendar g = (XMLGregorianCalendar)value;
                QName gtype = g.getXMLSchemaType();
                if (gtype.equals(DatatypeConstants.DATETIME)) {
                    return DateTimeValue.makeDateTimeValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.DATE)) {
                    return DateValue.makeDateValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.TIME)) {
                    return TimeValue.makeTimeValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GYEAR)) {
                    return GYearValue.makeGYearValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GYEARMONTH)) {
                    return GYearMonthValue.makeGYearMonthValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GMONTH)) {
                    return GMonthValue.makeGMonthValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GMONTHDAY)) {
                    return GMonthDayValue.makeGMonthDayValue(gtype.toString()).asAtomic();
                } else if (gtype.equals(DatatypeConstants.GDAY)) {
                    return GDayValue.makeGDayValue(gtype.toString()).asAtomic();
                } else {
                    throw new AssertionError("Unknown Gregorian date type");
                }
            } else if (value instanceof QName) {
                QName q = (QName)value;
                return new QNameValue(q.getPrefix(), q.getNamespaceURI(), q.getLocalPart(),
                        BuiltInAtomicType.QNAME, null);
            } else if (value instanceof Node) {
                return (Item)(new DOMObjectModel().convertObjectToXPathValue(value, config));
            } else if (value instanceof Source) {
                // Saxon extension to the XQJ specification
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send((Source)value, b);
                return b.getCurrentRoot();
            } else if (value instanceof XMLStreamReader) {
                // Saxon extension to the XQJ specification
                StaxBridge bridge = new StaxBridge();
                bridge.setXMLStreamReader((XMLStreamReader)value);
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send(new PullSource(bridge), b);
                return b.getCurrentRoot();
            } else {
                throw new XPathException("Java object cannot be converted to an XQuery value");
            }
        } catch (XPathException e) {
            XQException xqe = new XQException(e.getMessage());
            xqe.initCause(e);
            throw xqe;
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
// Contributor(s):
//