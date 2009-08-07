package net.sf.saxon.event;

import net.sf.saxon.om.NodeInfo;

/**
 * A Receiver that can be inserted into an event pipeline to copy location information.
 * The class acts as a LocationProvider, so it supports getSystemId() and getLineNumber() methods;
 * the location returned can vary for each node, and is set by the class generating the events.
 * The class is used when it is necessary to copy a subtree along with its location information;
 * for example, when copying an inline schema within a stylesheet to a separate schema document.
 *
 * <p><i>Note: prior to 9.2, the LocationCopier was a ProxyReceiver that passed all events on the
 * pipeline unchanged. It no longer does this, instead it is found as the LocationProvider on a
 * pipeline, but does not itself see the pipeline events.</i></p>
 */

public class LocationCopier implements CopyInformee, SourceLocationProvider {

    private String systemId;
    private int lineNumber;
    private boolean wholeDocument;

    public LocationCopier(boolean wholeDocument) {
        this.wholeDocument = wholeDocument;
    }

    /**
     * Provide information about the node being copied. This method is called immediately before
     * the startElement call for the element node in question.
     *
     * @param element the node being copied, which must be an element node
     */

    public int notifyElementNode(NodeInfo element) {
        systemId = (wholeDocument ? element.getSystemId() : element.getBaseURI());
            // The logic behind this is that if we are copying the whole document, we will be copying all
            // the relevant xml:base attributes; so retaining the systemId values is sufficient to enable
            // the base URIs of the nodes to be preserved. But if we only copy an element (for example
            // an xsl:import-schema element - see test schema091 - then its base URI might be affected
            // by xml:base attributes that aren't being copied. Ideally we would have two separate properties,
            // but XDM doesn't work that way.
        lineNumber = element.getLineNumber();
        return 0;
    }

    public String getSystemId(long locationId) {
        return systemId;
    }

    public int getLineNumber(long locationId) {
        return lineNumber;
    }

    public int getColumnNumber(long locationId) {
        return -1;
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
// Contributor(s): None
//
