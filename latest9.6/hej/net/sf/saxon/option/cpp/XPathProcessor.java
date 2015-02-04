package net.sf.saxon.option.cpp;

import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.s9api.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * * This class is to use with Saxon/C on C++
 */
public class XPathProcessor extends SaxonCAPI {

    private XPathCompiler compiler = null;
    private XPathSelector selector = null;
    private XdmItem contextItem = null;

    public XPathProcessor() {
        super();
        compiler = processor.newXPathCompiler();
    }

    public XPathProcessor(boolean l) {
        super(l);
        compiler = processor.newXPathCompiler();
    }

    public XPathProcessor(Processor proc, boolean l) {
        super(proc, l);
        compiler = processor.newXPathCompiler();
    }


    /**
     * Set whether XPath 1.0 backwards compatibility mode is to be used. In backwards compatibility
     * mode, more implicit type conversions are allowed in XPath expressions, for example it
     * is possible to compare a number with a string. The default is false (backwards compatibility
     * mode is off).
     *
     * @param option true if XPath 1.0 backwards compatibility is to be enabled, false if it is to
     *               be disabled.
     */

    public void setBackwardsCompatible(boolean option) {
        compiler.setBackwardsCompatible(option);
    }


    public void setBaseURI(String uriStr) throws SaxonApiException {
        URI uri = null;
        try {
            uri = new URI(uriStr);
            compiler.setBaseURI(uri);
        } catch (URISyntaxException e) {
            throw new SaxonApiException(e);
        }

    }



    public void setContextItem(XdmItem item) throws SaxonApiException {
            this.contextItem = item;
    }

    public void setProperties(String[] params, Object[] values) throws SaxonApiException {
        if(selector != null) {
            applyXPathProperties(this , "", processor, selector, params, values);
        } else {
            throw new SaxonApiException("XPathExecutable not created");
        }

    }

    public void reset(){
        compiler = null;
        selector = null;
    }



    public XdmValue evaluate(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {


       applyXPathProperties(this, cwd, processor, selector, params, values);


       return compiler.evaluate(xpathStr, contextItem);

    }


    public XdmItem evaluateSingle(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {



       applyXPathProperties(this, cwd, processor, selector, params, values);


       return compiler.evaluateSingle(xpathStr, contextItem);

    }

    public boolean effectiveBooleanValue(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {


       selector = compiler.compile(xpathStr).load();


       applyXPathProperties(this, cwd, processor, selector, params, values);
       if(contextItem != null){
        selector.setContextItem(contextItem);
       }
       return selector.effectiveBooleanValue();



    }




    public static void applyXPathProperties(SaxonCAPI api, String cwd, Processor processor, XPathSelector selector, String[] params, Object[] values) throws SaxonApiException {
        if (params != null) {
            String outputFilename = null;
            String initialTemplate = null;
            String initialMode = null;
            XdmItem item = null;
            String outfile = null;
            Source source = null;
            DocumentBuilder builder = processor.newDocumentBuilder();
            Map<Serializer.Property, String> propsList = new HashMap<Serializer.Property, String>();
            if (params.length != values.length) {
                throw new SaxonApiException("Length of params array not equal to the length of values array");
            }
            if (params.length != 0) {
                if (cwd != null && cwd.length() > 0) {
            if (!cwd.endsWith("/")) {
                cwd = cwd.concat("/");
            }
                }
                for (int i = 0; i < params.length; i++) {
                    if (params[i].startsWith("!")) {
                        String name = params[i].substring(1);
                        Serializer.Property prop = Serializer.Property.get(name);
                        if(prop == null) {
                            throw new SaxonApiException("Property name "+name+ " not found");
                        }
                        propsList.put(prop, (String) values[i]);
                    }   else if (params[i].equals("s")) {
                       source = api.resolveFileToSource(cwd, (String) values[i]);
                       selector.setContextItem(builder.build(source));
                    } else if (params[i].equals("item") || params[i].equals("node")) {
                        Object value = values[i];
                        if (value instanceof XdmItem) {
                            item = (XdmItem) value;
                            selector.setContextItem(item);
                        }
                    } else if(params[i].equals("resources")){
                        char separatorChar = '/';
                        if (SaxonCAPI.RESOURCES_DIR == null) {
                            String dir1 = (String)values[i];
                            if (!dir1.endsWith("/")) {
                               dir1 = dir1.concat("/");
                            }
                             if (File.separatorChar != '/') {
                               dir1.replace(separatorChar, File.separatorChar);
                                separatorChar = '\\';
                            }
                            SaxonCAPI.RESOURCES_DIR = dir1;
                        }

                    } else if (params[i].startsWith("param:")) {
                        String paramName = params[i].substring(6);
                        Object value = values[i];
                        XdmValue xdmValue;
                        if (value instanceof XdmValue) {
                            xdmValue = (XdmValue) value;
                            if (debug) {
                                System.err.println("XSLTTransformerForCpp: " + paramName);
                                System.err.println("XSLTTransformerForCpp: " + xdmValue.getUnderlyingValue().toString());
                                net.sf.saxon.type.ItemType suppliedItemType = SequenceTool.getItemType(xdmValue.getUnderlyingValue(), processor.getUnderlyingConfiguration().getTypeHierarchy());
                                System.err.println("XSLTTransformerForCpp: " + xdmValue.getUnderlyingValue());
                                System.err.println("XSLTTransformerForCpp Type: " + suppliedItemType.toString());
                            }


                            QName qname = QName.fromClarkName(paramName);
                            selector.setVariable(qname, xdmValue);
                        }
                    }
                }
            }
            if(api.serializer != null) {
                for(Map.Entry pairi : propsList.entrySet()){
                        api.serializer.setOutputProperty((Serializer.Property)pairi.getKey(), (String) pairi.getValue());
                }
            }
        }

    }


    public static void main(String [] arg) throws SaxonApiException{
        XPathProcessor xpath = new XPathProcessor(false);

         Processor p = xpath.getProcessor();
            DocumentBuilder b = p.newDocumentBuilder();
            XdmNode foo = b.build(new StreamSource(new StringReader("<foo><bar/></foo>")));
        xpath.setContextItem(foo);
        XdmValue value = xpath.evaluateSingle("", "//*", null, null);

        System.out.println(value.toString());

    }





}
