package net.sf.saxon.event;

import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.SchemaType;

import java.util.HashMap;
import java.util.Stack;


/**
  * This class sits in a receiver (push) pipeline and maintains the current path.
  * @author Michael H. Kay
  */


public class PathMaintainer extends ProxyReceiver {

    private static class PathElement {
        NodeName name;
        int index;
        public PathElement(NodeName name, int index) {
            this.name = name;
            this.index = index;
        }
    }

    private Stack<PathElement> path = new Stack<PathElement>();
    private Stack<HashMap<NodeName, Integer>> siblingCounters =
            new Stack<HashMap<NodeName, Integer>>();

    public PathMaintainer(/*@NotNull*/ Receiver next) {
        super(next);
        siblingCounters.push(new HashMap<NodeName, Integer>());
    }


    public void startElement (NodeName elemName, SchemaType type, int locationId, int properties) throws XPathException
    {
    	// System.err.println("startElement " + nameCode);
        nextReceiver.startElement(elemName, type, locationId, properties);
        HashMap<NodeName, Integer> counters = siblingCounters.peek();
        int index = 1;
        Integer preceding = counters.get(elemName);
        if (preceding != null) {
            index = preceding + 1;
            counters.put(elemName, index);
        } else {
            counters.put(elemName, 1);
        }
        path.push(new PathElement(elemName, index));
        siblingCounters.push(new HashMap<NodeName, Integer>());
    }

    /**
    * Handle an end-of-element event
    */

    public void endElement () throws XPathException {
        nextReceiver.endElement();
        siblingCounters.pop();
        path.pop();
    }

    /**
     * Get the path to the current location in the stream
     * @param useURIs set to true if namespace URIs are to appear in the path;
     * false if prefixes are to be used instead. The prefix will be the one
     * that is used in the source document, and is potentially ambiguous.
     * @return the path to the current location, as a string.
     */

    public String getPath(boolean useURIs) {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.MEDIUM);
        for (PathElement pe : path) {
            fsb.append('/');
            if (useURIs) {
                String uri = pe.name.getURI();
                if (uri.length()!=0) {
                    fsb.append('"');
                    fsb.append(uri);
                    fsb.append('"');
                }
            } else {
                String prefix = pe.name.getPrefix();
                if (prefix.length()!=0) {
                    fsb.append(prefix);
                    fsb.append(':');
                }
            }
            fsb.append(pe.name.getLocalPart());
            fsb.append('[');
            fsb.append(pe.index+"");
            fsb.append(']');
        }
        return fsb.toString();
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