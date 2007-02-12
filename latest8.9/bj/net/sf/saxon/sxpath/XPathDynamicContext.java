package net.sf.saxon.sxpath;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;

import javax.xml.transform.Source;

/**
 * This object represents the dynamic XPath execution context for use in the free-standing Saxon XPath API.
 * The dynamic context holds the context item and the values of external variables used by the XPath expression.
 *
 * <p>This object is always created via the method
 * {@link net.sf.saxon.sxpath.XPathExpression#createDynamicContext(net.sf.saxon.om.Item)}</p>
 */
public class XPathDynamicContext {

    private XPathContextMajor contextObject;

    protected XPathDynamicContext(XPathContextMajor contextObject) {
        this.contextObject = contextObject;
    }

    /**
     * Set the context item to a node derived from a supplied Source object. This may be
     * any implementation of the Source interface recognized by Saxon. Note that the
     * Saxon {@link NodeInfo} interface, representing a node in a tree, is one such
     * implementation; others include {@link javax.xml.transform.stream.StreamSource},
     * {@link javax.xml.transform.sax.SAXSource}, and {@link javax.xml.transform.dom.DOMSource}
     *
     * @param source The source object representing the node that will be used as the context item
     * @throws XPathException if a failure occurs reading or parsing a Source object to build an input tree
     */

    public void setContextNode(Source source) throws XPathException {
        NodeInfo origin;
        if (source instanceof NodeInfo) {
            origin = (NodeInfo)source;
        } else {
            origin = contextObject.getConfiguration().buildDocument(source);
        }
        setContextItem(origin);
    }

    /**
     * Set the context item for evaluation of the XPath Expression
     * @param item the context item
     */

    public void setContextItem(Item item) {
        UnfailingIterator iter = SingletonIterator.makeIterator(item);
        iter.next();
        contextObject.setCurrentIterator(iter);
    }

    /**
     * Get the context item
     * @return the context item if there is one, or null otherwise
     */

    public Item getContextItem() {
        return contextObject.getContextItem();
    }

    /**
     * Set the value of an external variable used within the XPath expression
     * @param variable the object representing the variable, as returned by the
     * {@link net.sf.saxon.sxpath.XPathEvaluator#declareVariable(String, String)} method.
     * Note that setting the value of a variable does not modify the {@link XPathVariable}
     * object itself, which means that this method is thread-safe.
     * @param value The value of the variable.
     */

    public void setVariable(XPathVariable variable, ValueRepresentation value) {
        contextObject.setLocalVariable(variable.getLocalSlotNumber(), value);
    }

    /**
     * For system use: get the wrapped XPathContext object
     * @return the XPathContext object
     */

    protected XPathContext getXPathContextObject() {
        return contextObject;
    }

    /**
     * Check that all external variables have been given a value
     */

    protected void checkExternalVariables(SlotManager stackFrameMap, int numberOfExternals) throws XPathException {
        ValueRepresentation[] stack = contextObject.getStackFrame().getStackFrameValues();
        for (int i=0; i<numberOfExternals; i++) {
            if (stack[i] == null) {
                int fp = ((Integer)stackFrameMap.getVariableMap().get(i)).intValue();
                String name = contextObject.getConfiguration().getNamePool().getDisplayName(fp);
                throw new DynamicError("No value has been supplied for variable $" + name);
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

