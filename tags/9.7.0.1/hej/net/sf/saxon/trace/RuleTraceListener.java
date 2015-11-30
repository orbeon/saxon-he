// Copyright (c) 2015. Saxonica Limited. All rights reserved.

package net.sf.saxon.trace;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.Version;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.StreamWriterToReceiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.TemplateRule;
import net.sf.saxon.lib.Logger;
import net.sf.saxon.lib.StandardLogger;
import net.sf.saxon.lib.TraceListener2;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.*;
import net.sf.saxon.value.DateTimeValue;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A trace listener that is specifically for tracing the application and time costs of pattern matching.
 */
public class RuleTraceListener implements TraceListener2 {
    String sourceId;
    String xslId;

    public void setXslId(String xslId) {
        this.xslId = xslId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }


    private class RuleTrace {
        public List<Long> times;
        public List<Integer> type;

        RuleTrace() {
            times = new ArrayList<Long>(200);
        }

        void add(long duration, Item item) {
            add(duration);
        }

        void add(long duration) {
            times.add(duration);
        }
    }

    private class BuiltInRuleTrace extends RuleTrace {
        public List<Integer> type;

        BuiltInRuleTrace() {
            super();
            type = new ArrayList<Integer>();
        }

        void add(long duration, Item item) {
            add(duration);
            if (item instanceof NodeInfo) {
                type.add(((NodeInfo) item).getNodeKind());
            }
        }
    }

    /*
    This structure keeps records of the rule matching - the actual rule, how long it took to be found and
    the item type that was used (or even the item itself?)
     */
    HashMap<Mode, HashMap<Object, RuleTrace>> rules = new HashMap<Mode, HashMap<Object, RuleTrace>>();

    Logger out = new StandardLogger();

    /*@Nullable*/ private Configuration config = null;

    /**
     * Set the PrintStream to which the output will be written.
     *
     * @param stream the PrintStream to be used for output. By default, the output is written
     *               to System.err.
     * @throws XPathException
     * @throws XMLStreamException
     */

    public void setOutputDestination(Logger stream) {
        out = stream;
    }

    /**
     * Called at start
     */

    public void open(/*@NotNull*/ Controller controller) {
        config = controller.getConfiguration();
    }

    private long initialStart = 0;        // FOr estimating complete time

    private long startTime;

    private static String typeChars = "EATW  PCD   N";        //Element Attribute Text Whitespace .... etc.

    public void startRuleSearch() {
        startTime = System.nanoTime();
        if (initialStart == 0) {
            initialStart = startTime;
        }
    }

    /**
     * Called at the end of a rule search
     *  @param rule the rule (or possible built-in ruleset) that has been selected
     * @param mode the mode in operation
     * @param item
     */

    public void endRuleSearch(Object rule, Mode mode, Item item) {
        long duration = System.nanoTime() - startTime;
        HashMap<Object, RuleTrace> thisModeRules = rules.get(mode);
        if (thisModeRules == null) {
            thisModeRules = new HashMap<Object, RuleTrace>();
            rules.put(mode, thisModeRules);
        }
        RuleTrace details = thisModeRules.get(rule);
        if (details == null) {
            details = rule instanceof BuiltInRuleSet ? new BuiltInRuleTrace() : new RuleTrace();
            thisModeRules.put(rule, details);
        }
        details.add(duration, item);
    }

    public void close() {
        long overallTime = System.nanoTime() - initialStart;

        try {
            Receiver rec = null;
            rec = ExpressionPresenter.defaultDestination(config, out);
            XMLStreamWriter writer = new StreamWriterToReceiver(rec);
            writer.writeStartDocument();
            writer.writeStartElement("statsRTL");
            writer.writeAttribute("total.time", "" + overallTime);
            writer.writeAttribute("dateTime", DateTimeValue.getCurrentDateTime(null).getPrimitiveStringValue().toString());
            if(sourceId != null) {
                writer.writeAttribute("source",sourceId);
            }
            if(xslId != null) {
                writer.writeAttribute("xsl",xslId);
            }
            writer.writeStartElement("config");
            {
                String label = config.getLabel();
                if (label != null) {
                    writer.writeAttribute("label", label);
                }
                writer.writeAttribute("edition", config.getEditionCode());
                writer.writeAttribute("version", Version.getProductVersion());
                config.getDefaultXsltCompilerInfo().getPatternOptimization().write(writer);
            }
            writer.writeEndElement();
            for (Mode mode : rules.keySet()) {
                writer.writeStartElement("mode");
                writer.writeAttribute("name", "" + mode.getModeName().getDisplayName().replace("saxon:_defaultMode", "#default"));
                HashMap<Object, RuleTrace> thisModeRules = rules.get(mode);
                writer.writeAttribute("count", "" + thisModeRules.size());
                for (Object r : thisModeRules.keySet()) {
                    writer.writeStartElement("rule");
                    RuleTrace trace = thisModeRules.get(r);
                    List<Long> times = trace.times;
                    Collections.sort(times);
                    int size = times.size();
                    writer.writeAttribute("count", "" + size);
                    long sum = 0, min = Long.MAX_VALUE, max = 0, var = 0;
                    for (Long ti : times) {
                        sum += ti.longValue();
                        min = Math.min(min, ti.longValue());
                        max = Math.max(min, ti.longValue());
                    }
                    long mean = sum / size;
                    for (Long ti : times) {
                        long diff = mean - ti.longValue();
                        var += diff * diff;
                    }
                    var = var / size;
                    long lowerDecile = times.get(size / 10);
                    long upperDecile = times.get(9 * size / 10);
                    long upper20th = times.get(19 * size / 20);

                    writer.writeAttribute("total", "" + sum);
                    writer.writeAttribute("min", "" + min);
                    writer.writeAttribute("l10", "" + lowerDecile);
                    writer.writeAttribute("avg", "" + mean);
                    writer.writeAttribute("std", "" + (long) Math.sqrt(var));
                    writer.writeAttribute("med", "" + times.get(size / 2));
                    writer.writeAttribute("h10", "" + upperDecile);
                    writer.writeAttribute("h20", "" + upper20th);
                    writer.writeAttribute("max", "" + max);
                    if (r instanceof Rule) {
                        Rule rule = (Rule) r;
                        TemplateRule t = (TemplateRule) rule.getAction();
                        String f = t.getSystemId();
                        writer.writeAttribute("loc", f + "#" + t.getLineNumber());
                        writer.writeAttribute("seq", "" + rule.getSequence());
                        writer.writeAttribute("rank", "" + rule.getRank());
                    } else if (r instanceof BuiltInRuleSet) {
                        writer.writeAttribute("built-in", mode.getCodeForBuiltInRuleSet((BuiltInRuleSet) r));
                        String res = "";
                        int hist[] = new int[14];
                        for (Integer b : ((BuiltInRuleTrace) trace).type) {
                            hist[b - 1]++;
                        }
                        for (int i = 0; i < hist.length; i++) {
                            if (hist[i] > 0) {
                                res += " " + typeChars.charAt(i) + ":" + hist[i];
                            }
                        }
                        writer.writeAttribute("types", res.substring(1));
                    }
                    int nBins = 10;

                    if (size > 300) {
                        int hist[] = new int[nBins + 2];
                        long d = (upper20th - min) / nBins;
                        for (Long t : times) {
                            int bin = (int) ((t.longValue() - min) / d);
                            if (t < upper20th) {
                                hist[bin]++;
                            }
                        }
                        String res = "";
                        for (int b : hist) {
                            res += " " + b;
                        }
                        writer.writeAttribute("hist", res.substring(1));
                    }
                    writer.writeEndElement();
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
        } catch (XPathException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    public void enter(/*@NotNull*/ InstructionInfo instruction, XPathContext context) {
    }

    public void leave(/*@NotNull*/ InstructionInfo instruction) {
    }

    public void startCurrentItem(Item currentItem) {
    }

    public void endCurrentItem(Item currentItem) {
    }
}
