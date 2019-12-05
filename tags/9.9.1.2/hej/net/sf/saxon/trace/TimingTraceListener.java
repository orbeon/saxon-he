////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trace;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.StreamWriterToReceiver;
import net.sf.saxon.event.TransformerReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.lib.Logger;
import net.sf.saxon.lib.StandardLogger;
import net.sf.saxon.lib.TraceListener;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.XsltController;
import net.sf.saxon.value.StringValue;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

/**
 * A trace listener that records timing information for templates and functions, outputting this
 * information as an HTML report to a specified destination when the transformation completes.
 */

public class TimingTraceListener implements TraceListener {

    private int repeat = 0;

    private static class InstructionDetails {
        public InstructionInfo instruct;
        public long gross;
        public long net;
        public long count;
    }


    Logger out = new StandardLogger();
    private long t_total;
    /*@NotNull*/ private Stack<InstructionDetails> instructs = new Stack<>();
    /*@NotNull*/ HashMap<InstructionInfo, InstructionDetails> instructMap = new HashMap<>();
    /*@Nullable*/ protected Configuration config = null;

    private InstructionInfo instructStack[] = new InstructionInfo[1500];
    private int stackDepth = 0;
    private int lang = Configuration.XSLT;

    /**
     * Set the PrintStream to which the output will be written.
     *
     * @param stream the PrintStream to be used for output. By default, the output is written
     *               to System.err.
     */

    public void setOutputDestination(Logger stream) {
        out = stream;
    }

    /**
     * Called at start
     */

    public void open(/*@NotNull*/ Controller controller) {
        config = controller.getConfiguration();
        lang = controller.getExecutable().getHostLanguage();
        t_total = System.nanoTime();
    }

    /**
     * Called at end. This method builds the XML out and analyzed html output
     */

    public void close() {
        t_total = System.nanoTime() - t_total;
        repeat++;
        try {
            PreparedStylesheet sheet = this.getStyleSheet();
            XsltController controller = sheet.newController();

            SerializationProperties props = new SerializationProperties();
            props.setProperty("method", "html");
            props.setProperty("indent", "yes");
            controller.setTraceListener(null);
            TransformerReceiver tr = new TransformerReceiver(controller);
            controller.initializeController(new GlobalParameterSet());
            tr.open();
            Receiver result = config.getSerializerFactory().getReceiver(out.asStreamResult(), props, controller.makePipelineConfiguration());
            tr.setDestination(result);
            XMLStreamWriter writer = new StreamWriterToReceiver(tr);
            writer.writeStartDocument();

            writer.writeStartElement("trace");
            writer.writeAttribute("t-total", Double.toString((double) t_total / 1000000));
            for (InstructionDetails ins : instructMap.values()) {
                writer.writeStartElement("fn");
                String name = "UNKNOWN";
                if (ins.instruct.getObjectName() != null) {
                    name = ins.instruct.getObjectName().getDisplayName();
                    writer.writeAttribute("name", name);
                } else {
                    if (ins.instruct.getProperty("name") != null) {
                        name = ins.instruct.getProperty("name").toString();
                        writer.writeAttribute("name", name);
                    }
                }
                if (ins.instruct.getProperty("match") != null) {
                    name = ins.instruct.getProperty("match").toString();
                    writer.writeAttribute("match", name);
                }
                if (ins.instruct.getProperty("mode") != null) {
                    name = ins.instruct.getProperty("mode").toString();
                    writer.writeAttribute("mode", name);
                }
                writer.writeAttribute("construct", ins.instruct.getConstructType() == StandardNames.XSL_FUNCTION ? "function" : ins.instruct.getConstructType() == StandardNames.XSL_VARIABLE ? "variable" : "template");
                String file = ins.instruct.getSystemId();
                if (file != null) {
                    writer.writeAttribute("file", file);
                }
                writer.writeAttribute("count", Long.toString(ins.count/ repeat)) ;
                writer.writeAttribute("t-sum-net", Double.toString((double) ins.net / repeat/ 1000000));
                writer.writeAttribute("t-avg-net", Double.toString(ins.net / (double) ins.count / 1000000));
                writer.writeAttribute("t-sum", Double.toString((double) ins.gross / repeat/ 1000000));
                writer.writeAttribute("t-avg", Double.toString(ins.gross / (double) ins.count / 1000000));
                writer.writeAttribute("line", Long.toString(ins.instruct.getLineNumber()));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        } catch (TransformerException e) {
            System.err.println("Unable to transform timing profile information: " + e.getMessage());
        } catch (XMLStreamException e) {
            System.err.println("Unable to generate timing profile information: " + e.getMessage());
        }
    }

    /**
     * Called when an instruction in the stylesheet gets processed
     */

    public void enter(/*@NotNull*/ InstructionInfo instruction, XPathContext context) {
        int loc = instruction.getConstructType();
        if (loc == StandardNames.XSL_FUNCTION || loc == StandardNames.XSL_TEMPLATE || loc == StandardNames.XSL_VARIABLE) {
            long start = System.nanoTime();
            InstructionDetails instructDetails = new InstructionDetails();
            instructDetails.instruct = instruction;
            instructDetails.gross = start;
            instructs.add(instructDetails);

            instructStack[stackDepth++] = instruction;
        }
    }

    /**
     * Called after an instruction of the stylesheet got processed
     */

    public void leave(/*@NotNull*/ InstructionInfo instruction) {
        int loc = instruction.getConstructType();
        if (loc == StandardNames.XSL_FUNCTION || loc == StandardNames.XSL_TEMPLATE || loc == StandardNames.XSL_VARIABLE) {
            InstructionDetails instruct = instructs.peek();
            long duration = System.nanoTime() - instruct.gross;
            instruct.net = duration - instruct.net;
            instruct.gross = duration;
            InstructionDetails foundInstructDetails = instructMap.get(instruction);
            if (foundInstructDetails == null) {
                instruct.count = 1;
                instructMap.put(instruction, instruct);
                stackDepth--;
            } else {
                foundInstructDetails.count = foundInstructDetails.count + 1;
                boolean inStack = false;
                for (int i = 0; i < stackDepth - 1; i++) {
                    if (instructStack[i] == instruction) {
                        inStack = true;
                        break;
                    }
                }
                stackDepth--;
                if (!inStack) {
                    foundInstructDetails.gross = foundInstructDetails.gross + instruct.gross;
                }
                foundInstructDetails.net = foundInstructDetails.net + instruct.net;
            }
            instructs.pop();
            if (!instructs.isEmpty()) {
                InstructionDetails parentInstruct = instructs.peek();
                parentInstruct.net = parentInstruct.net + duration;
            }
        }
    }

    /**
     * Called when an item becomes current
     */

    public void startCurrentItem(Item item) {
    }

    /**
     * Called after a node of the source tree got processed
     */

    public void endCurrentItem(Item item) {
    }

    /**
     * Prepare Stylesheet to render the analyzed XML data out.
     * This method can be overridden in a subclass to produce the output in a different format.
     */
    /*@NotNull*/
    protected PreparedStylesheet getStyleSheet() throws XPathException {
        InputStream in = getStylesheetInputStream();
        StreamSource ss = new StreamSource(in, "profile.xsl");
        CompilerInfo info = config.getDefaultXsltCompilerInfo();
        info.setParameter(new StructuredQName("", "", "lang"),
                          new StringValue(this.lang == Configuration.XSLT ? "XSLT" : "XQuery"));
        return Compilation.compileSingletonPackage(config, info, ss);
    }

    /**
     * Get an input stream containing the stylesheet used for formatting results
     * @return the input stream
     */

    protected InputStream getStylesheetInputStream() {
        List<String> messages = new ArrayList<>();
        List<ClassLoader> classLoaders = new ArrayList<>();
        return Configuration.locateResource("profile.xsl", messages, classLoaders);
    }

}

