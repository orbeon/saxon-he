using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;



public class XsltExamples {


    public static void Main(String[] argv) {

        String test = "all";

        if (argv.Length > 0) {
            test = argv[0];
        }

        String samplesDir;

        if (argv.Length > 1) {
            samplesDir = argv[1];
        } else {
            String home = Environment.GetEnvironmentVariable("SAXON_HOME");
            if (home == null) {
                Console.WriteLine("No input directory supplied, and SAXON_HOME is not set");
                return;
            } else {
                if (home.EndsWith("/") || home.EndsWith("\\")) {
                    samplesDir = home + "samples/";
                } else {
                    samplesDir = home + "/samples/";
                }
            }
        }

        String dir = samplesDir + "trax/";

        String foo_xml = dir + "xml/foo.xml";
        String foo_xsl = dir + "xsl/foo.xsl";
        String baz_xml = dir + "xml/baz.xml";
        String baz_xsl = dir + "xsl/baz.xsl";
        String foo2_xsl = dir + "xsl/foo2.xsl";
        String foo3_xsl = dir + "xsl/foo3.xsl";
        String text_xsl = dir + "xsl/text.xsl";
        String embedded_xml = dir + "xml/embedded.xml";
        String multidoc_xsl = dir + "xsl/multidoc.xsl";
        String valid_xml = dir + "xml/valid.xml";
        String lookup_xsl = dir + "xsl/lookup.xsl";


        if (test == "all" || test == "ExampleSimple1") {
            Console.WriteLine("\n\n==== ExampleSimple1 ====");

            try {
                ExampleSimple1(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleSimple2") {
            Console.WriteLine("\n\n==== ExampleSimple2 ====");

            try {
                ExampleSimple2(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }


        if (test == "all" || test == "ExampleFromReader") {
            Console.WriteLine("\n\n==== ExampleFromReader ====");

            try {
                ExampleFromReader(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleUseTemplatesObj") {
            Console.WriteLine("\n\n==== ExampleUseTemplatesObj ====");

            try {
                ExampleUseTemplatesObj(foo_xml, baz_xml,
                                       foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleSAXResult") {
            Console.WriteLine("\n\n==== ExampleSAXResult ====");

            try {
                ExampleSAXResult(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleXMLReader") {
            Console.WriteLine("\n\n==== ExampleXMLReader ====");

            try {
                ExampleXMLReader(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleXMLFilter") {
            Console.WriteLine("\n\n==== ExampleXMLFilter ====");

            try {
                ExampleXMLFilter(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleXMLFilterChain") {
            Console.WriteLine("\n\n==== ExampleXMLFilterChain ====");

            try {
                ExampleXMLFilterChain(foo_xml, foo_xsl,
                                      foo2_xsl, foo3_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleSaxonToSaxon") {
            Console.WriteLine("\n\n==== ExampleSaxonToSaxon ====");

            try {
                ExampleSaxonToSaxon(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleSaxonToSaxonNonRoot") {
            Console.WriteLine("\n\n==== ExampleSaxonToSaxonNonRoot ====");

            try {
                ExampleSaxonToSaxonNonRoot(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleDOMtoDOM") {
            Console.WriteLine("\n\n==== ExampleDOMtoDOM (using wrapper) ====");

            try {
                ExampleDOMtoDOM(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleParam") {
            Console.WriteLine("\n\n==== ExampleParam ====");

            try {
                ExampleParam(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleTransformerReuse") {
            Console.WriteLine("\n\n==== ExampleTransformerReuse ====");

            try {
                ExampleTransformerReuse(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleOutputProperties") {
            Console.WriteLine("\n\n==== ExampleOutputProperties ====");

            try {
                ExampleOutputProperties(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleUseAssociated") {
            Console.WriteLine("\n\n==== ExampleUseAssociated ====");

            try {
                ExampleUseAssociated(foo_xml);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleUseEmbedded") {
            Console.WriteLine("\n\n==== ExampleUseEmbedded ====");

            try {
                ExampleUseAssociated(embedded_xml);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleDisplayingErrors") {
            Console.WriteLine("\n\n==== ExampleDisplayingErrors ====");

            try {
                ExampleDisplayingErrors();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleCapturingErrors") {
            Console.WriteLine("\n\n==== ExampleCapturingErrors ====");

            try {
                ExampleCapturingErrors();
            } catch (Exception ex) {
                handleException(ex);
            }
        }


        if (test == "all" || test == "ExampleUsingURIResolver") {
            Console.WriteLine("\n\n==== ExampleUsingURIResolver ====");

            try {
                ExampleUsingURIResolver(foo_xml, text_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleUsingDTD") {
            Console.WriteLine("\n\n==== ExampleUsingDTD ====");

            try {
                ExampleUsingDTD(valid_xml, lookup_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleMultipleOutput") {
            Console.WriteLine("\n\n==== ExampleMultipleOutput ====");

            try {
                ExampleMultipleOutput(foo_xml, multidoc_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExampleUsingResultDocumentHandler") {
            Console.WriteLine("\n\n==== ExampleUsingResultDocumentHandler ====");

            try {
                ExampleUsingResultDocumentHandler(foo_xml, multidoc_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "ExtraTestCase") {
            Console.WriteLine("\n\n==== ExtraTestCase ====");

            try {
                ExtraTestCase(foo_xml, foo_xsl);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        Console.WriteLine("\n==== done! ====");
    }

    /**
     * Show the simplest possible transformation from URI of source document
     * to output stream.
     */
    public static void ExampleSimple1(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a transformer for the stylesheet.
        XsltTransformer transformer = processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        // Create a serializer
        Serializer serializer = new Serializer();
        serializer.SetOutputWriter(Console.Out);

        // Transform the source XML to System.out.
        transformer.Run(serializer);
    }


    /**
     * Show the simplest possible transformation from File
     * to a File.
     */
    public static void ExampleSimple2(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a transformer for the stylesheet.
        XsltTransformer transformer = processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        // Create a serializer
        Serializer serializer = new Serializer();
        serializer.SetOutputStream(new FileStream("ExampleSimple2.out", FileMode.Create, FileAccess.Write));

        // Transform the source XML to System.out.
        transformer.Run(serializer);

        Console.WriteLine("\nOutput written to ExampleSimple2.out\n");
    }


    /**
     * Show simple transformation from reader to output stream.  In general
     * this use case is discouraged, since the XML encoding can not be
     * processed.
     */
    public static void ExampleFromReader(String sourceUri, String xsltUri) {

        /*        // Create a transform factory instance.
                TransformerFactory tfactory = TransformerFactory.newInstance();

                // Note that in this case the XML encoding can not be processed!
                Reader       xslReader = new BufferedReader(new FileReader(xsltUri));
                StreamSource xslSource = new StreamSource(xslReader);

                // Note that if we don't do this, relative URLs can not be resolved correctly!
                xslSource.setSystemId(xsltUri);

                // Create a transformer for the stylesheet.
                Transformer transformer = tfactory.newTransformer(xslSource);

                // Note that in this case the XML encoding can not be processed!
                Reader       xmlReader = new BufferedReader(new FileReader(sourceUri));
                StreamSource xmlSource = new StreamSource(xmlReader);

                // The following line would be needed if the source document contained
                // a relative URL
                // xmlSource.setSystemId(sourceUri);

                // Transform the source XML to System.out.
                transformer.transform(xmlSource, new StreamResult(System.out));*/
    }

    /**
     * Perform a transformation using a compiled stylesheet (a Templates object)
     */
    public static void ExampleUseTemplatesObj(
            String sourceUri1, String sourceUri2, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Create a compiled stylesheet
        XsltExecutable templates = processor.NewXsltCompiler().Compile(new Uri(xsltUri));

        // Note: we could actually use the same XSltTransformer in this case.
        // But in principle, the two transformations could be done in parallel in separate threads.

        // Do the first transformation
        Console.WriteLine("\n\n----- transform of " + sourceUri1 + " -----");
        XsltTransformer transformer1 = templates.Load();
        transformer1.InitialContextNode = processor.NewDocumentBuilder().Build(new Uri(sourceUri1));
        transformer1.Run(new Serializer());     // default destination is Console.Out

        // Do the second transformation
        Console.WriteLine("\n\n----- transform of " + sourceUri2 + " -----");
        XsltTransformer transformer2 = templates.Load();
        transformer2.InitialContextNode = processor.NewDocumentBuilder().Build(new Uri(sourceUri2));
        transformer2.Run(new Serializer());     // default destination is Console.Out        

    }


    /**
     * Send output to a user-specified ContentHandler.
     */

    public static void ExampleSAXResult(
            String sourceUri, String xsltUri) {

        /*       TransformerFactory tfactory = TransformerFactory.newInstance();

               // Does this factory support SAX features?
               if (tfactory.getFeature(SAXResult.FEATURE)) {

                   // Get a transformer in the normal way:
                   Transformer transformer =
                       tfactory.newTransformer(new StreamSource(xsltUri));

                   // Get the source as a StreamSource
                   Reader       xmlReader = new BufferedReader(new FileReader(sourceUri));
                   StreamSource xmlSource = new StreamSource(xmlReader);

                   // Set the result handling to be a serialization to System.out.
                   Result result = new SAXResult(new ExampleContentHandler());

                   // Do the transformation
                   transformer.transform(xmlSource, result);

               } else {
                   Console.WriteLine(
                       "Can't do ExampleSAXResult because tfactory is not a SAXTransformerFactory");
               }*/
    }


    /**
     * Show the Transformer as a SAX2 XMLReader.  An XMLFilter obtained
     * from newXMLFilter should act as a transforming XMLReader if setParent is not
     * called.  Internally, an XMLReader is created as the parent for the XMLFilter.
     */
    public static void ExampleXMLReader(String sourceUri, String xsltUri) {

        /*   TransformerFactory tfactory = TransformerFactory.newInstance();

           if (tfactory.getFeature(SAXSource.FEATURE)) {
               XMLReader reader =
                   ((SAXTransformerFactory) tfactory)
                       .newXMLFilter(new StreamSource(new File(xsltUri)));

               reader.setContentHandler(new ExampleContentHandler());
               reader.parse(new InputSource(new File(sourceUri).toURL().toString()));
           } else {
               Console.WriteLine("tfactory does not support SAX features!");
           }*/
    }

    /**
     * Show the Transformer as a simple XMLFilter.  This is pretty similar
     * to ExampleXMLReader, except that here the parent XMLReader is created
     * by the caller, instead of automatically within the XMLFilter.  This
     * gives the caller more direct control over the parent reader.
     */
    public static void ExampleXMLFilter(String sourceUri, String xsltUri) {

        /*      TransformerFactory tfactory = TransformerFactory.newInstance();
              XMLReader reader   = makeXMLReader();


              // The transformer will use a SAX parser as it's reader.

              try {
                  reader.setFeature(
                      "http://xml.org/sax/features/namespace-prefixes", true);
              } catch (SAXException se) {
                  System.err.println("SAX Parser doesn't report namespace prefixes!");
                  throw se;
              }

              XMLFilter filter =
                  ((SAXTransformerFactory) tfactory)
                      .newXMLFilter(new StreamSource(new File(xsltUri)));

              filter.setParent(reader);
              filter.setContentHandler(new ExampleContentHandler());

              // Now, when you call transformer.parse, it will set itself as
              // the content handler for the parser object (it's "parent"), and
              // will then call the parse method on the parser.
              filter.parse(new InputSource(new File(sourceUri).toURL().toString()));*/
    }

    /**
     * This Example shows how to chain events from one Transformer
     * to another transformer, using the Transformer as a
     * SAX2 XMLFilter/XMLReader.
     */
    public static void ExampleXMLFilterChain(
            String sourceUri, String xsltUri_1, String xsltUri_2, String xsltUri_3) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a compiler
        XsltCompiler compiler = processor.NewXsltCompiler();

        // Compile all three stylesheets
        XsltTransformer transformer1 = compiler.Compile(new Uri(xsltUri_1)).Load();
        XsltTransformer transformer2 = compiler.Compile(new Uri(xsltUri_2)).Load();
        XsltTransformer transformer3 = compiler.Compile(new Uri(xsltUri_3)).Load();

        // Now run them in series
        transformer1.InitialContextNode = input;
        XdmDestination results1 = new XdmDestination();
        transformer1.Run(results1);
        Console.WriteLine("After phase 1:");
        Console.WriteLine(results1.XdmNode.OuterXml);

        transformer2.InitialContextNode = results1.XdmNode;
        XdmDestination results2 = new XdmDestination();
        transformer2.Run(results2);
        Console.WriteLine("After phase 2:");
        Console.WriteLine(results2.XdmNode.OuterXml);

        transformer3.InitialContextNode = results2.XdmNode;
        //TextWriterDestination results3 = new TextWriterDestination(new XmlTextWriter(Console.Out));
        XdmDestination results3 = new XdmDestination();
        transformer3.Run(results3);
        Console.WriteLine("After phase 3:");
        Console.WriteLine(results3.XdmNode.OuterXml);

    }

    /**
     * Show how to transform a Saxon tree into another Saxon tree.
     */
    public static void ExampleSaxonToSaxon(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a compiler
        XsltCompiler compiler = processor.NewXsltCompiler();

        // Compile the stylesheet
        XsltTransformer transformer = compiler.Compile(new Uri(xsltUri)).Load();

        // Run the transformation
        transformer.InitialContextNode = input;
        XdmDestination result = new XdmDestination();
        transformer.Run(result);

        // Serialize the result so we can see that it worked
        StringWriter sw = new StringWriter();
        result.XdmNode.WriteTo(new XmlTextWriter(sw));
        Console.WriteLine(sw.ToString());

        // Note: we don't do 
        //   result.XdmNode.WriteTo(new XmlTextWriter(Console.Out));
        // because that results in the Console.out stream being closed, 
        // with subsequent attempts to write to it being rejected.

    }

    /**
     * Show how to transform a tree starting at a node other than the root.
     */
    public static void ExampleSaxonToSaxonNonRoot(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Navigate to the first grandchild
        XPathSelector eval = processor.NewXPathCompiler().Compile("/*/*[1]").Load();
        eval.ContextItem = input;
        input = (XdmNode)eval.EvaluateSingle();

        // Create an XSLT compiler
        XsltCompiler compiler = processor.NewXsltCompiler();

        // Compile the stylesheet
        XsltTransformer transformer = compiler.Compile(new Uri(xsltUri)).Load();

        // Run the transformation
        transformer.InitialContextNode = input;
        XdmDestination result = new XdmDestination();
        transformer.Run(result);

        // Serialize the result so we can see that it worked
        Console.WriteLine(result.XdmNode.OuterXml);

    }


    /**
     * Show how to transform a DOM tree into another DOM tree.
     * This uses the System.Xml parser to parse an XML file into a
     * DOM, and create an output DOM. In this Example, Saxon uses a
     * third-party DOM as both input and output.
     */
    public static void ExampleDOMtoDOM(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document (in practice, it would already exist as a DOM)
        XmlDocument doc = new XmlDocument();
        doc.Load(new XmlTextReader(sourceUri));
        XdmNode input = processor.NewDocumentBuilder().Wrap(doc);

        // Create a compiler
        XsltCompiler compiler = processor.NewXsltCompiler();

        // Compile the stylesheet
        XsltTransformer transformer = compiler.Compile(new Uri(xsltUri)).Load();

        // Run the transformation
        transformer.InitialContextNode = input;
        DomDestination result = new DomDestination();
        transformer.Run(result);

        // Serialize the result so we can see that it worked
        Console.WriteLine(result.XmlDocument.OuterXml);
    }


    /**
     * This shows how to set a parameter for use by the stylesheet. Use
     * two transformers to show that different parameters may be set
     * on different transformers.
     */
    public static void ExampleParam(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Compile the stylesheet
        XsltExecutable exec = processor.NewXsltCompiler().Compile(new Uri(xsltUri));

        // Create two transformers with different parameters
        XsltTransformer transformer1 = exec.Load();
        XsltTransformer transformer2 = exec.Load();

        transformer1.SetParameter(new QName("", "", "a-param"), new XdmAtomicValue("hello to you!"));
        transformer2.SetParameter(new QName("", "", "a-param"), new XdmAtomicValue("goodbye to you!"));

        // Now run them both
        transformer1.InitialContextNode = input;
        XdmDestination results1 = new XdmDestination();
        transformer1.Run(results1);

        transformer2.InitialContextNode = input;
        XdmDestination results2 = new XdmDestination();
        transformer2.Run(results2);

        Console.WriteLine("1: " + results1.XdmNode.StringValue);
        Console.WriteLine("2: " + results2.XdmNode.StringValue);
    }

    /**
     * Show the that a transformer can be reused, and show resetting
     * a parameter on the transformer. 
     */
    public static void ExampleTransformerReuse(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Compile the stylesheet
        XsltExecutable exec = processor.NewXsltCompiler().Compile(new Uri(xsltUri));

        // Create a transformer 
        XsltTransformer transformer = exec.Load();

        // Run it once        
        transformer.SetParameter(new QName("", "", "a-param"), new XdmAtomicValue("hello to you!"));
        transformer.InitialContextNode = input;
        XdmDestination results = new XdmDestination();
        transformer.Run(results);
        Console.WriteLine("1: " + results.XdmNode.StringValue);

        // Run it again        
        transformer.SetParameter(new QName("", "", "a-param"), new XdmAtomicValue("hello to me!"));
        transformer.InitialContextNode = input;
        results.Reset();
        transformer.Run(results);
        Console.WriteLine("2: " + results.XdmNode.StringValue);

    }

    /**
     * Show how to override output properties.
     */
    public static void ExampleOutputProperties(String sourceUri, String xsltUri) {

        /*      TransformerFactory tfactory  = TransformerFactory.newInstance();
              Templates          templates =
                  tfactory.newTemplates(new StreamSource(new File(xsltUri)));
              Properties         oprops    = templates.getOutputProperties();
              oprops.put(OutputKeys.INDENT, "yes");

              Transformer transformer = templates.newTransformer();

              transformer.setOutputProperties(oprops);
              transformer.transform(new StreamSource(new File(sourceUri)),
                                    new StreamResult(System.out));  */
    }

    /**
     * Show how to get stylesheets that are associated with a given
     * xml document via the xml-stylesheet PI (see http://www.w3.org/TR/xml-stylesheet/).
     */
    public static void ExampleUseAssociated(String sourceUri) {

        // Create a Processor instance.
        Processor processor = new Processor();
        XsltExecutable exec;

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));
        Console.WriteLine("=============== source document ===============");
        Console.WriteLine(input.OuterXml);
        Console.WriteLine("=========== end of source document ============");

        // Navigate to the xml-stylesheet processing instruction having the pseudo-attribute type=text/xsl;
        // then extract the value of the href pseudo-attribute if present

        String path = @"/processing-instruction(xml-stylesheet)[matches(.,'type\s*=\s*[''""]text/xsl[''""]')]" +
                @"/replace(., '.*?href\s*=\s*[''""](.*?)[''""].*', '$1')";

        XPathSelector eval = processor.NewXPathCompiler().Compile(path).Load();
        eval.ContextItem = input;
        XdmAtomicValue hrefval = (XdmAtomicValue)eval.EvaluateSingle();
        Console.WriteLine("evaluated");
        String href = hrefval.ToString();

        if (href == null || href == "") {
            Console.WriteLine("No suitable xml-stylesheet processing instruction found");
            return;

        } else if (href[0] == '#') {

            // The stylesheet is embedded in the source document and identified by a URI of the form "#id"

            Console.WriteLine("Locating embedded stylesheet with href = " + href);
            String idpath = "id('" + href.Substring(1) + "')";
            eval = processor.NewXPathCompiler().Compile(idpath).Load();
            eval.ContextItem = input;
            XdmNode node = (XdmNode)eval.EvaluateSingle();
            if (node == null) {
                Console.WriteLine("No element found with ID " + href.Substring(1));
                return;
            }
            exec = processor.NewXsltCompiler().Compile(node);

        } else {

            // The stylesheet is in an external document

            Console.WriteLine("Locating stylesheet at uri = " + new Uri(input.BaseUri, href));

            // Fetch and compile the referenced stylesheet
            exec = processor.NewXsltCompiler().Compile(new Uri(input.BaseUri, href.ToString()));
        }

        // Create a transformer 
        XsltTransformer transformer = exec.Load();

        // Run it       
        transformer.SetParameter(new QName("", "", "a-param"), new XdmAtomicValue("hello to you!"));
        transformer.InitialContextNode = input;
        XdmDestination results = new XdmDestination();
        transformer.Run(results);
        Console.WriteLine("1: " + results.XdmNode.StringValue);

    }

    /**
     * Show a stylesheet compilation in which errors are displayed to the console
     */

    public static void ExampleDisplayingErrors() {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Create the XSLT Compiler
        XsltCompiler compiler = processor.NewXsltCompiler();


        // Define a stylesheet containing errors
        String stylesheet =
            "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
            "<xsl:template name='eee:template'>\n" +
            "  <xsl:value-of select='32'/>\n" +
            "</xsl:template>\n" +
            "<xsl:template name='main'>\n" +
            "  <xsl:value-of select='$var'/>\n" +
            "</xsl:template>\n" +
            "</xsl:stylesheet>";


        // Attempt to compile the stylesheet and display the errors
        try {
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            compiler.Compile(new XmlTextReader(new StringReader(stylesheet)));
            Console.WriteLine("Stylesheet compilation succeeded");
        } catch (Exception) {
            Console.WriteLine("Stylesheet compilation failed");
        }

    }

    /**
     * Show a stylesheet compilation in which errors are captured for processing by
     * the calling application.
     */

    public static void ExampleCapturingErrors() {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Create the XSLT Compiler
        XsltCompiler compiler = processor.NewXsltCompiler();

        // Create a list to hold the error information
        compiler.ErrorList = new ArrayList();

        // Define a stylesheet containing errors
        String stylesheet =
            "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>\n" +
            "<xsl:template name='fff:template'>\n" +
            "  <xsl:value-of select='32'/>\n" +
            "</xsl:template>\n" +
            "<xsl:template name='main'>\n" +
            "  <xsl:value-of select='$var'/>\n" +
            "</xsl:template>\n" +
            "</xsl:stylesheet>";


        // Attempt to compile the stylesheet and display the errors
        try {
            compiler.BaseUri = new Uri("http://localhost/stylesheet");
            compiler.Compile(new XmlTextReader(new StringReader(stylesheet)));
            Console.WriteLine("Stylesheet compilation succeeded");
        } catch (Exception) {
            Console.WriteLine("Stylesheet compilation failed with " + compiler.ErrorList.Count + " errors");
            foreach (StaticError error in compiler.ErrorList) {
                Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
            }
        }

    }

    /**
     * Show a transformation using a user-written URI Resolver.
     */

    public static void ExampleUsingURIResolver(String sourceUri, String xsltUri) {

        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a transformer for the stylesheet.
        XsltTransformer transformer = processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        // Set the user-written XmlResolver
        transformer.InputXmlResolver = new UserXmlResolver();

        // Create a serializer
        Serializer serializer = new Serializer();
        serializer.SetOutputWriter(Console.Out);

        // Transform the source XML to System.out.
        transformer.Run(serializer);

    }


    /**
    * A sample XmlResolver. This handles a URI ending with ".txt". It loads the
    * text file identified by the URI, assuming it is in ISO-8859-1 encoding,
    * and wraps it in a containing doc element.
    * If the URI doesn't end with ".txt", it delegates processing
    * to the standard XmlResolver.
    */

    public class UserXmlResolver : XmlUrlResolver {

        public override object GetEntity(Uri absoluteUri, String role, Type ofObjectToReturn) {
            Stream input = (Stream)base.GetEntity(absoluteUri, role, ofObjectToReturn);
            if (absoluteUri.ToString().EndsWith(".txt")) {
                StreamReader reader = new StreamReader(input);
                String content = reader.ReadToEnd();
                content = content.Replace("&", "&amp;");
                content = content.Replace("<", "&lt;");
                content = content.Replace(">", "&gt;");
                content = "<doc>" + content + "</doc>";
                return new StringReader(content);
            } else {
                return input;
            }
        }
    }

    /**
     * Show a transformation using multiple result documents.
     */

    public static void ExampleMultipleOutput(String sourceUri, String xsltUri) {
        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a transformer for the stylesheet.
        XsltTransformer transformer = processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        // Create a serializer
        Serializer serializer = new Serializer();
        serializer.SetOutputWriter(Console.Out);

        // Transform the source XML to System.out.
        transformer.Run(serializer);


    }

    /**
     * Show a transformation using a DTD-based validation and the id() function.
     */

    public static void ExampleUsingDTD(String sourceUri, String xsltUri) {

        // Create a Processor instance
        Processor processor = new Processor();

        // Load the source document
        DocumentBuilder db = processor.NewDocumentBuilder();
        db.DtdValidation = true;
        XdmNode input = db.Build(new Uri(sourceUri));

        //Create a transformer for the stylesheet
        XsltTransformer transformer =
            processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        //Set the destination
        XdmDestination results = new XdmDestination();
        // Create a serializer
        //Serializer results = new Serializer();
        //results.SetOutputWriter(Console.Out);


        // Transform the XML
        transformer.Run(results);

        Console.WriteLine(results.XdmNode.ToString());

    }


    /**
     * Show a transformation using a user-written result document handler. This example
     * captures each of the result documents in a DOM, and creates a Hashtable that indexes
     * the DOM trees according to their absolute URI. On completion, it writes all the DOMs
     * to the standard output.
     */

    public static void ExampleUsingResultDocumentHandler(String sourceUri, String xsltUri) {
        // Create a Processor instance.
        Processor processor = new Processor();

        // Load the source document
        XdmNode input = processor.NewDocumentBuilder().Build(new Uri(sourceUri));

        // Create a transformer for the stylesheet.
        XsltTransformer transformer = processor.NewXsltCompiler().Compile(new Uri(xsltUri)).Load();

        // Set the root node of the source document to be the initial context node
        transformer.InitialContextNode = input;

        // Create a serializer
        Serializer serializer = new Serializer();
        serializer.SetOutputWriter(Console.Out);

        // Establish the result document handler
        Hashtable results = new Hashtable();
        transformer.ResultDocumentHandler = new ExampleResultDocumentHandler(results);

        // Transform the source XML to System.out.
        transformer.Run(serializer);

        // Process the captured DOM results
        foreach (DictionaryEntry entry in results) {
            string uri = (string)entry.Key;
            Console.WriteLine("\nResult File " + uri);
            DomDestination dom = (DomDestination)results[uri];
            Console.Write(dom.XmlDocument.OuterXml);
        }
    }

    public class ExampleResultDocumentHandler : IResultDocumentHandler {

        private Hashtable results;
        
        public ExampleResultDocumentHandler(Hashtable table) {
            this.results = table;
        }

        public XmlDestination HandleResultDocument(string href, Uri baseUri) {
            DomDestination destination = new DomDestination();
            results[href] = destination;
            return destination;
        }

    }

    /**
     * Add your own test case here
     */

    public static void ExtraTestCase(String sourceURI, String xsltUri) {
        //
    }



    private static void handleException(Exception ex) {

        Console.WriteLine("EXCEPTION: " + ex);
        Console.WriteLine(ex.StackTrace);


    }


}