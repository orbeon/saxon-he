////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

/**
 * This class is a filter that passes all Receiver events through unchanged,
 * except that it changes namecodes to allow for the source and the destination
 * using different NamePools. This is necessary when a stylesheet has been constructed
 * as a general document (e.g. as the result of a transformation) and is passed to
 * newTemplates() to be compiled as a stylesheet.
 *
 * <p>The type annotations of nodes passed through this filter must be built-in types
 * in the XSD namespace, because user-defined types belong to a specific Configuration
 * and cannot readily be transferred. In practice the class is used only for untyped trees.</p>
 */


public class NamePoolConverter extends ProxyReceiver {

    NamePool oldPool;
    NamePool newPool;

    /**
     * Constructor
     *
     * @param next    the next receiver in the pipeline
     * @param oldPool the old namepool
     * @param newPool th new namepool
     */

    public NamePoolConverter(Receiver next, NamePool oldPool, NamePool newPool) {
        super(next);
        this.oldPool = oldPool;
        this.newPool = newPool;
    }

    /**
     * Output element start tag
     */

    public void startElement(NodeName name, SchemaType type, Location location, int properties) throws XPathException {
        checkType(type);
        int fp = newPool.allocateFingerprint(name.getURI(), name.getLocalPart());
        nextReceiver.startElement(new CodedName(fp, name.getPrefix(), newPool), type, location, properties);
    }

    /**
     * Handle an attribute
     */

    public void attribute(NodeName name, SimpleType type, CharSequence value, Location location, int properties)
            throws XPathException {
        checkType(type);
        int fp = newPool.allocateFingerprint(name.getURI(), name.getLocalPart());
        nextReceiver.attribute(new CodedName(fp, name.getPrefix(), newPool), type, value, location, properties);
    }

    /**
     * Check that the schema type of a node is a built-in type
     * @param type the type to be checked
     * @throws UnsupportedOperationException if the type is a user-defined type
     */

    private void checkType(SchemaType type) {
        if ((type.getFingerprint() & NamePool.USER_DEFINED_MASK) != 0) {
            throw new UnsupportedOperationException("Cannot convert a user-typed node to a different name pool");
        }
    }

}

