package net.sf.saxon.trace;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StandardNames;

import java.io.PrintStream;

/**
* A Simple trace listener that writes messages to System.err or to another designated PrintStream
*/

public class TimedTraceListener implements TraceListener {

    PrintStream out = System.err;

    /**
     * Set the PrintStream to which the output will be written.
     * @param stream the PrintStream to be used for output. By default, the output is written
     * to System.err.
     */

    public void setOutputStream(PrintStream stream) {
        out = stream;
    }

    /**
    * Called at start
    */

    public void open() {
        out.println("<trace time=\"" + System.currentTimeMillis() + "\">");
    }

    /**
    * Called at end
    */

    public void close() {
        out.println("<end time=\"" + System.currentTimeMillis()
    	       + "\"/></trace>");
    }

    /**
    * Called when an instruction in the stylesheet gets processed
    */

    public void enter(InstructionInfo instruction, XPathContext context) {
        int loc = instruction.getConstructType();
        if (loc == StandardNames.XSL_FUNCTION || loc == StandardNames.XSL_TEMPLATE) {
            String tag = "<";
            tag += (loc==StandardNames.XSL_FUNCTION ? "function" : "template");
            String name = null;
            if (instruction.getObjectName() != null) {
                name = instruction.getObjectName().getDisplayName();
            } else if (instruction.getProperty("name") != null) {
                name = instruction.getProperty("name").toString();
            }
            if (name != null) {
                tag += " name=\"" + name + "\"";
            }
            if (instruction.getProperty("match") != null) {
                tag += " match=\"" + instruction.getProperty("match") + "\"";
            }
            String file = instruction.getSystemId();
            if (file != null) {
                if (file.length()>15) {
                    file="*" + file.substring(file.length()-14);
                }
                tag += " file=\"" + file + "\"";
            }
            tag += " line=\"" + instruction.getLineNumber() + "\"";
            tag += " time=\"" + System.currentTimeMillis() + "\"";
            tag += ">";
            out.println(tag);
        }
    }

    /**
    * Called after an instruction of the stylesheet got processed
    */

  	public void leave(InstructionInfo instruction) {
        int loc = instruction.getConstructType();
        if (loc == StandardNames.XSL_FUNCTION || loc == StandardNames.XSL_TEMPLATE) {
            String tag = "<end time=\"" + System.currentTimeMillis() + "\"/></";
            tag += (loc==StandardNames.XSL_FUNCTION ? "function>" : "template>");
            out.println(tag);
        }
  	}

    /**
    * Called when an item becomes current
    */

    public void startCurrentItem(Item item) {}

    /**
    * Called after a node of the source tree got processed
    */

    public void endCurrentItem(Item item) {}

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//