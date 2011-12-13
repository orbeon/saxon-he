package net.sf.saxon.style;

import net.sf.saxon.Controller;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.StartTagBuffer;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ExpressionLocation;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DateTimeValue;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import java.util.Stack;

/**
 * This is a filter inserted into the input pipeline for processing stylesheet modules, whose
 * task is to evaluate use-when expressions and discard those parts of the stylesheet module
 * for which the use-when attribute evaluates to false.
 */

public class UseWhenFilter extends ProxyReceiver {

    private StartTagBuffer startTag;
    private int useWhenCode;
    private int xslUseWhenCode;
    private int defaultNamespaceCode;
    private int depthOfHole = 0;
    private boolean emptyStylesheetElement = false;
    private Stack defaultNamespaceStack = new Stack();
    private DateTimeValue currentDateTime = DateTimeValue.getCurrentDateTime(null);

    /**
     * Create a UseWhenFilter
     * @param next the next receiver in the pipeline
     */

    public UseWhenFilter(Receiver next) {
        super(next);
    }

    /**
     * Set the start tag buffer
     * @param startTag a preceding filter on the pipeline that buffers the attributes of a start tag
     */

    public void setStartTagBuffer(StartTagBuffer startTag) {
        this.startTag = startTag;
    }

    /**
     * Start of document
     */

    public void open() throws XPathException {
        useWhenCode = getNamePool().allocate("", "", "use-when") & 0xfffff;
        xslUseWhenCode = getNamePool().allocate("xsl", NamespaceConstant.XSLT, "use-when");
        defaultNamespaceCode = getNamePool().allocate("", "", "xpath-default-namespace");
        nextReceiver.open();
    }

    /**
     * Notify the start of an element.
     *
     * @param elemName    the name of the element.
     * @param typeCode    integer code identifying the element's type within the name pool.
     * @param properties  bit-significant properties of the element node
     */

    public void startElement(NodeName elemName, SchemaType typeCode, int locationId, int properties) throws XPathException {
        defaultNamespaceStack.push(startTag.getAttribute(defaultNamespaceCode));
        if (emptyStylesheetElement) {
            depthOfHole++;
            return;
        }
        if (depthOfHole == 0) {
            String useWhen;
            if (elemName.isInNamespace(NamespaceConstant.XSLT)) {
                useWhen = startTag.getAttribute(useWhenCode);
            } else {
                useWhen = startTag.getAttribute(xslUseWhenCode);
            }
            if (useWhen != null) {
                LocationProvider lp = getPipelineConfiguration().getLocationProvider();
                try {
                    boolean b = evaluateUseWhen(useWhen, lp.getLineNumber(locationId));
                    if (!b) {
                        int fp = elemName.allocateNameCode(getNamePool()) & NamePool.FP_MASK;
                        if (fp == StandardNames.XSL_STYLESHEET || fp == StandardNames.XSL_TRANSFORM) {
                            emptyStylesheetElement = true;
                        } else {
                            depthOfHole = 1;
                            return;
                        }
                    }
                } catch (XPathException e) {
                    XPathException err = new XPathException("Error in use-when expression. " + e.getMessage());
                    ExpressionLocation loc = new ExpressionLocation();
                    loc.setSystemId(lp.getSystemId(locationId));
                    loc.setLineNumber(lp.getLineNumber(locationId));
                    err.setLocator(loc);
                    err.setErrorCodeQName(e.getErrorCodeQName());
                    try {
                        getPipelineConfiguration().getErrorListener().fatalError(err);
                    } catch (TransformerException tex) {
                        throw XPathException.makeXPathException(tex);
                    }
                    err.setHasBeenReported(true);
                    throw err;
                }
            }
            nextReceiver.startElement(elemName, typeCode, locationId, properties);
        } else {
            depthOfHole++;
        }
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceBinding
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(NamespaceBinding namespaceBinding, int properties) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.namespace(namespaceBinding, properties);
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     *
     *
     * @param attName   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.attribute(attName, typeCode, value, locationId, properties);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.startContent();
        }
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        defaultNamespaceStack.pop();
        if (depthOfHole > 0) {
            depthOfHole--;
        } else {
            nextReceiver.endElement();
        }
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (depthOfHole == 0) {
            nextReceiver.characters(chars, locationId, properties);
        }
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) {
        // these are ignored in a stylesheet
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        // these are ignored in a stylesheet
    }

    /**
     * Evaluate a use-when attribute
     * @param expression the expression to be evaluated
     * @param locationId identifies the location of the expression in case error need to be reported
     * @return the effective boolean value of the result of evaluating the expression
     */

    public boolean evaluateUseWhen(String expression, int locationId) throws XPathException {
        UseWhenStaticContext staticContext = new UseWhenStaticContext(getConfiguration(), startTag);
        LocationProvider lp = getPipelineConfiguration().getLocationProvider();
        // TODO: The following doesn't take account of xml:base attributes
        staticContext.setBaseURI(lp.getSystemId(locationId));
        staticContext.setDefaultElementNamespace(NamespaceConstant.NULL);
        for (int i=defaultNamespaceStack.size()-1; i>=0; i--) {
            String uri = (String)defaultNamespaceStack.get(i);
            if (uri != null) {
                staticContext.setDefaultElementNamespace(uri);
                break;
            }
        }
        Expression expr = ExpressionTool.make(expression, staticContext,
                staticContext, 0, Token.EOF, lp.getLineNumber(locationId), null);
        expr.setContainer(staticContext);
        ItemType contextItemType = Type.ITEM_TYPE;
        ExpressionVisitor.ContextItemType cit = new ExpressionVisitor.ContextItemType(contextItemType, true);
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext, staticContext.getExecutable());
        expr = visitor.typeCheck(expr, cit);
        SlotManager stackFrameMap = getPipelineConfiguration().getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(expr, stackFrameMap.getNumberOfVariables(), stackFrameMap);
        Controller controller = new Controller(getConfiguration());
        controller.setURIResolver(new URIPreventer());
        controller.setCurrentDateTime(currentDateTime);
                // this is to ensure that all use-when expressions in a module use the same date and time
        XPathContext dynamicContext = controller.newXPathContext();
        dynamicContext = dynamicContext.newCleanContext();
        ((XPathContextMajor)dynamicContext).openStackFrame(stackFrameMap);
        return expr.effectiveBooleanValue(dynamicContext);
    }

    /**
     * Define a URIResolver that disallows all URIs
     */

    private static class URIPreventer implements URIResolver {
        /**
         * Called by the processor when it encounters
         * an xsl:include, xsl:import, or document() function.
         *
         * @param href An href attribute, which may be relative or absolute.
         * @param base The base URI against which the first argument will be made
         *             absolute if the absolute URI is required.
         * @return A Source object, or null if the href cannot be resolved,
         *         and the processor should try to resolve the URI itself.
         * @throws javax.xml.transform.TransformerException
         *          if an error occurs when trying to
         *          resolve the URI.
         */
        /*@NotNull*/ public Source resolve(String href, String base) throws TransformerException {
            throw new TransformerException("No external documents are available within an [xsl]use-when expression");
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//