////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.serialize;

import net.sf.saxon.expr.instruct.ResultDocument;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.InscopeNamespaceResolver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.Type;

import javax.xml.transform.SourceLocator;
import java.util.Properties;

/**
 * This class handles a set of serialization parameters provided in the form of an XDM instance
 * as specified in the Serialization 3.0 definition, section 3.1
 */
public class SerializationParamsHandler {

    public static final String NAMESPACE = NamespaceConstant.OUTPUT;
    Properties properties;
    SourceLocator locator;

    /**
     * Set the location of the instruction to be used for error message reporting
     * @param locator the location for error reporting
     */

    public void setLocator(SourceLocator locator) {
        this.locator = locator;
    }

    /**
     * Set the serialization parameters in the form of an XDM instance
     *
     * @param node either the serialization-parameters element node, or a document node having
     *             this element as its only child
     * @throws XPathException if incorrect serialization parameters are found
     */

    public void setSerializationParams(NodeInfo node) throws XPathException {
        properties = new Properties();
        if (node.getNodeKind() == Type.DOCUMENT) {
            node = Navigator.getOutermostElement((DocumentInfo) node);
        }
        if (node.getNodeKind() != Type.ELEMENT) {
            throw new XPathException("Serialization params: node must be a document or element node");
        }
        if (!node.getLocalPart().equals("serialization-parameters")) {
            throw new XPathException("Serialization params: element name must be 'serialization-parameters");
        }
        if (!node.getURI().equals(NAMESPACE)) {
            throw new XPathException("Serialization params: element namespace must be " + NAMESPACE);
        }
        AxisIterator kids = node.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
            String lname = child.getLocalPart();
            String uri = child.getURI();
            if (NamespaceConstant.OUTPUT.equals(uri)) {
                uri = "";
            }
            if ("".equals(uri) && lname.equals("use-character-maps")) {
                //TODO: FIXME
                throw new XPathException("The 'use-character-maps' property is not available.", "SEPM0016");
            }
            String value = child.getAttributeValue("", "value");
            if (value == null) {
                XPathException err = new XPathException("In the serialization parameters, element " +
                        Err.depict(child) + " has no @value attribute", "SEPM0017");
                err.setLocator(locator);
                throw err;
            }
            try {
                ResultDocument.setSerializationProperty(properties, uri, lname, value,
                        new InscopeNamespaceResolver(child), false, node.getConfiguration());
            } catch (XPathException err) {
                if ("XQST0109".equals(err.getErrorCodeLocalPart())) {
                    if ("".equals(uri)) {
                        XPathException e2 = new XPathException("Unknown serialization parameters " +
                                Err.depict(child), "SEPM0017");
                        e2.setLocator(locator);
                        throw e2;
                    }
                    // Unknown serialization parameter - no action, ignore the error
                    // TODO: check the final specification
                } else {
                    throw err;
                }
            }

        }
    }

    public Properties getSerializationProperties() {
        return properties;
    }
}

