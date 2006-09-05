using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;


/**
 * Some examples to show how the Saxon XQuery API should be used
 */
public class XQueryExamples {

    /**
     * Class is not instantiated, so give it a private constructor
     */
    private XQueryExamples() {
    }

    /**
     * Method main. First argument is the name of the test to be run (defaults to "all").
     * Second argument is the samples directory: defaults to %HOME%/samples/
     */
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

        String inputFile = samplesDir + "data/books.xml";

        if (test == "all" || test == "toStreamResult") {
            Console.WriteLine("\n\n==== toStreamResult ====");

            try {
                ExampleToStreamResult();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toSingleton") {
            Console.WriteLine("\n\n==== toSingleton ====");

            try {
                ExampleToSingleton();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toDOM") {
            Console.WriteLine("\n\n==== toDOM ====");

            try {
                ExampleToDOM(inputFile);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toXDM") {
            Console.WriteLine("\n\n==== toXDM ====");

            try {
                ExampleToXDM(inputFile);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toSequence") {
            Console.WriteLine("\n\n==== toSequence ====");

            try {
                ExampleToSequence();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "fromXmlReader") {
            Console.WriteLine("\n\n==== fromXmlReader ====");

            try {
                ExampleFromXmlReader(inputFile);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toSerializedSequence") {
            Console.WriteLine("\n\n==== toSerializedSequence ====");

            try {
                ExampleToSerializedSequence(inputFile);
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toWrappedSequence") {
            Console.WriteLine("\n\n==== toWrappedSequence ====");

            try {
                ExampleToWrappedSequence();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "toHTMLFile") {
            Console.WriteLine("\n\n==== toHTMLFile ====");

            try {
                ExampleToHTMLFile();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "withParam") {
            Console.WriteLine("\n\n==== withParam ====");

            try {
                ExampleWithParam();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "multiModule") {
            Console.WriteLine("\n\n==== multiModule ====");

            try {
                ExampleMultiModule();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "pipeline") {
            Console.WriteLine("\n\n==== pipeline ====");

            try {
                ExamplePipeline();
            } catch (Exception ex) {
                handleException(ex);
            }
        }

        if (test == "all" || test == "extra") {
            Console.WriteLine("\n\n==== extra ====");

            try {
                ExampleExtra();
            } catch (Exception ex) {
                handleException(ex);
            }
        }


    }

    /**
     * Show a query producing a document as its result and serializing this
     * to a FileStream
     */

    public static void ExampleToStreamResult() {
        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();
        compiler.BaseUri = "http://www.saxonica.com/";
        compiler.DeclareNamespace("saxon", "http://saxon.sf.net/");
        XQueryExecutable exp = compiler.Compile("<saxon:example>{static-base-uri()}</saxon:example>");
        XQueryEvaluator eval = exp.Load();
        Serializer qout = new Serializer();
        qout.SetOutputProperty(Serializer.METHOD, "xml");
        qout.SetOutputProperty(Serializer.INDENT, "yes");
        qout.SetOutputProperty(Serializer.SAXON_INDENT_SPACES, "1");
        qout.SetOutputStream(new FileStream("testoutput.xml", FileMode.Create, FileAccess.Write));
        eval.Run(qout);
    }

    /**
     * Show a query producing a single atomic value as its result and returning the value
     * to the Java application
     */

    public static void ExampleToSingleton() {
        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("avg(for $i in 1 to 10 return $i * $i)");
        XQueryEvaluator eval = exp.Load();
        XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
        Console.WriteLine("Result type: " + result.Value.GetType());
        Console.WriteLine("Result value: " + (decimal)result.Value);
    }

    /**
     * Show a query taking a DOM as its input and producing a DOM as its output. 
     */

    public static void ExampleToDOM(String inputFileName) {
        Processor processor = new Processor();

        XmlDocument input = new XmlDocument();
        input.Load(inputFileName);
        XdmNode indoc = processor.NewDocumentBuilder().Build(new XmlNodeReader(input));

        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
        XQueryEvaluator eval = exp.Load();
        eval.ContextItem = indoc;
        DomDestination qout = new DomDestination();
        eval.Run(qout);
        XmlDocument outdoc = qout.XmlDocument;
        Console.WriteLine(outdoc.OuterXml);

    }

    /**
     * Show a query taking a Saxon tree as its input and producing a Saxon tree as its output. 
     */

    public static void ExampleToXDM(String inputFileName) {
        Processor processor = new Processor();

        DocumentBuilder loader = processor.NewDocumentBuilder();
        loader.BaseUri = new Uri(inputFileName);
        XdmNode indoc = loader.Build(
                new FileStream(inputFileName, FileMode.Open, FileAccess.Read));

        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("<doc>{reverse(/*/*)}</doc>");
        XQueryEvaluator eval = exp.Load();
        eval.ContextItem = indoc;
        XdmDestination qout = new XdmDestination();
        eval.Run(qout);
        XdmNode outdoc = qout.XdmNode;
        Console.WriteLine(outdoc.OuterXml);

    }

    /**
     * Show a query producing a sequence as its result and returning the sequence
     * to the Java application in the form of an iterator. For each item in the
     * result, its string value is output.
     */

    public static void ExampleToSequence() {
        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("for $i in 1 to 10 return $i * $i");
        XQueryEvaluator eval = exp.Load();
        XdmValue value = eval.Evaluate();
        IEnumerator e = value.GetEnumerator();
        while (e.MoveNext()) {
            XdmItem item = (XdmItem)e.Current;
            Console.WriteLine(item.ToString());
        }

    }

    /**
     * Show a query reading an input document using an XmlReader (the .NET XML parser)
     */

    public static void ExampleFromXmlReader(String inputFileName) {
        Processor processor = new Processor();

        XmlTextReader reader = new XmlTextReader(inputFileName,
            new FileStream(inputFileName, FileMode.Open, FileAccess.Read));
        reader.Normalization = true;

        // add a validating reader - not to perform validation, but to expand entity references
        XmlValidatingReader validator = new XmlValidatingReader(reader);
        validator.ValidationType = ValidationType.None;

        XdmNode doc = processor.NewDocumentBuilder().Build(validator);

        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("/");
        XQueryEvaluator eval = exp.Load();
        eval.ContextItem = doc;
        Serializer qout = new Serializer();
        qout.SetOutputProperty(Serializer.METHOD, "xml");
        qout.SetOutputProperty(Serializer.INDENT, "yes");
        qout.SetOutputStream(new FileStream("testoutput2.xml", FileMode.Create, FileAccess.Write));
        eval.Run(qout);

    }

    /**
     * Show a query producing a sequence as its result and returning the sequence
     * to the Java application in the form of an iterator. The sequence is then
     * output by serializing each item individually, with each item on a new line.
     */

    public static void ExampleToSerializedSequence(String inputFileName) {
        Processor processor = new Processor();

        XmlTextReader reader = new XmlTextReader(inputFileName,
            new FileStream(inputFileName, FileMode.Open, FileAccess.Read));
        reader.Normalization = true;

        // add a validating reader - not to perform validation, but to expand entity references
        XmlValidatingReader validator = new XmlValidatingReader(reader);
        validator.ValidationType = ValidationType.None;

        XdmNode doc = processor.NewDocumentBuilder().Build(reader);

        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile("//ISBN");
        XQueryEvaluator eval = exp.Load();
        eval.ContextItem = doc;

        foreach (XdmNode node in eval) {
            Console.WriteLine(node.OuterXml);
        }
    }

    /**
     * Show a query producing a sequence as its result and returning the sequence
     * to the Java application in the form of an iterator. The sequence is then
     * output by wrapping the items in a document, with wrapping elements indicating
     * the type of each item, and serializing the resulting document.
     */

    public static void ExampleToWrappedSequence() {
        //final Processor processor = new Processor();
        //final StaticQueryContext sqc = new StaticQueryContext(processor);
        //final XQueryExpression exp = sqc.compileQuery("<doc><chap><a>3</a></chap></doc>//a, <b>4</b>, attribute c {5}, 19");
        //Properties props = new Properties();
        //props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        //props.setProperty(OutputKeys.INDENT, "yes");

        //final DynamicQueryContext dynamicContext = new DynamicQueryContext(processor);
        //final SequenceIterator iter = exp.iterator(dynamicContext);
        //final DocumentInfo doc = QueryResult.wrap(iter, processor);
        //QueryResult.serialize(doc, new StreamResult(System.out), props, processor);
    }


    /**
     * Show how to run a query that is read from a file and that serializes its output
     * as HTML to another file. The input to the query (the initial value of the context
     * node) is supplied as the content of another file.
     */

    public static void ExampleToHTMLFile() {
        //final Processor processor = new Processor();
        //final StaticQueryContext sqc = new StaticQueryContext(processor);
        //final XQueryExpression exp = sqc.compileQuery(new FileReader("query/books-to-html.xq"));
        //final DynamicQueryContext dynamicContext = new DynamicQueryContext(processor);
        //dynamicContext.setContextItem(sqc.buildDocument(new StreamSource("data/books.xml")));
        //final Properties props = new Properties();
        //props.setProperty(OutputKeys.METHOD, "html");
        //props.setProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.01 Transitional//EN");
        //exp.run(dynamicContext, new StreamResult(new File("booklist.html")), props);
    }

    /**
     * Show a query that takes a parameter (external variable) as input.
     * The query produces a single atomic value as its result and returns the value
     * to the Java application. For the types of value that may be returned, and
     * their mapping to XPath data types, see {@link XPathEvaluator#Evaluate}
     */

    public static void ExampleWithParam() {
        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();
        compiler.DeclareNamespace("p", "http://saxon.sf.net/ns/p");
        XQueryExecutable exp = compiler.Compile(
                "declare variable $p:in as xs:integer external; $p:in * $p:in");
        XQueryEvaluator eval = exp.Load();
        eval.SetExternalVariable(new QName("http://saxon.sf.net/ns/p", "p:in"), new XdmAtomicValue(12));
        XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
        Console.WriteLine("Result type: " + result.Value.GetType());
        Console.WriteLine("Result value: " + (long)result.Value);

    }

    /**
     * Show a query consisting of two modules, using a QueryResolver to resolve
     * the "import module" declaration
     */

    public static void ExampleMultiModule() {

        String mod1 = "import module namespace m2 = 'http://www.example.com/module2';" +
                      "m2:square(3)";

        String mod2 = "module namespace m2 = 'http://www.example.com/module2';" +
                      "declare function m2:square($p) { $p * $p };";

        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();

        InlineModuleResolver resolver = new InlineModuleResolver();
        resolver.AddModule(new Uri("http://www.example.com/module2"), mod2);
        compiler.QueryResolver = resolver;
        XQueryExecutable exp = compiler.Compile(mod1);
        XQueryEvaluator eval = exp.Load();

        XdmAtomicValue result = (XdmAtomicValue)eval.EvaluateSingle();
        Console.WriteLine("Result type: " + result.Value.GetType());
        Console.WriteLine("Result value: " + (long)result.Value);

    }

    // A simple QueryResolver designed to show that the actual query
    // text can come from anywhere: in this case, the resolver maintains
    // a simple mapping of module URIs onto strings.

    public class InlineModuleResolver : IQueryResolver {

        private Hashtable modules = new Hashtable();

        public void AddModule(Uri moduleName, String moduleText) {
            modules.Add(moduleName, moduleText);
        }

        public Uri[] GetModules(String moduleUri, Uri baseUri, String[] locationHints) {
            Uri[] result = { new Uri(moduleUri) };
            return result;
        }

        public Object GetEntity(Uri absoluteUri) {
            return modules[absoluteUri];
        }
    }

    /**
     * Show how to run two queries in tandem. The second query is applied to the
     * results of the first.
     */

    public static void ExamplePipeline() {
        //final Processor processor = new Processor();

        // Compile the first query
        //final StaticQueryContext sqc1 = new StaticQueryContext(processor);
        //final XQueryExpression exp1 = sqc1.compileQuery("declare variable $in as xs:integer external;" +
        //        "document{ <a>{$in * $in}</a> }");

        // Compile the second query (each query should have its own static context)
        //final StaticQueryContext sqc2 = new StaticQueryContext(processor);
        //final XQueryExpression exp2 = sqc2.compileQuery("/a + 5");

        // Run the first query
        //final DynamicQueryContext dynamicContext = new DynamicQueryContext(processor);
        //dynamicContext.setParameter("in", new Long(3));
        //final NodeInfo doc = (NodeInfo)exp1.evaluateSingle(dynamicContext);

        // Run the second query
        //dynamicContext.clearParameters();
        //dynamicContext.setContextItem(doc);
        //final Object result = exp2.evaluateSingle(dynamicContext);
        //Console.WriteLine("3*3 + 5 = " + result);
        // The result is actually a java.lang.Double
    }

    /**
     * Show how to run two queries in tandem. The second query is applied to the
     * results of the first.
     */

    public static void ExampleExtra() {
        // place-holder to add user-defined examples for testing
        Processor processor = new Processor();
        XQueryCompiler compiler = processor.NewXQueryCompiler();
        XQueryExecutable exp = compiler.Compile(@"<out>{matches('ABC', '\p{IsBasicLatin}*')}</out>");
        XQueryEvaluator eval = exp.Load();
        DomDestination dest = new DomDestination();
        eval.Run(dest);
        Console.WriteLine(dest.XmlDocument.OuterXml);
    }




    /**
     * Handle an exception thrown while running one of the examples
     *
     * @param ex the exception
     */
    private static void handleException(Exception ex) {
        Console.WriteLine("EXCEPTION: " + ex);
        Console.WriteLine(ex.StackTrace);
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
// Contributor(s): none.
//