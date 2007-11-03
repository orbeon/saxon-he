using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;


/**
 * Some examples to show how the Saxon Schema API should be used. These examples
 * require use of Saxon-SA.
 */
public class SchemaExamples
{

    /**
     * Class is not instantiated, so give it a private constructor
     */
    private SchemaExamples()
    {
    }

    /**
     * Method main. First argument is the Saxon samples directory.
     */
    public static void Main(String[] argv)
    {

        String samplesDir;

        if (argv.Length > 0)
        {
            samplesDir = argv[0];
        }
        else
        {
            String home = Environment.GetEnvironmentVariable("SAXON_HOME");
            if (home == null)
            {
                Console.WriteLine("No input directory supplied, and SAXON_HOME is not set");
                return;
            }
            else
            {
                if (home.EndsWith("/") || home.EndsWith("\\"))
                {
                    samplesDir = home + "samples/";
                }
                else
                {
                    samplesDir = home + "/samples/";
                }
            }
        }

        UriBuilder ub = new UriBuilder();
        ub.Scheme = "file";
        ub.Host = "";
        ub.Path = samplesDir;
        Uri baseUri = ub.Uri;

        Console.WriteLine("Base URI: " + baseUri.ToString());

        // Create a schema-aware Processor

        Processor saxon = new Processor(true);

        // Load a schema

        SchemaManager manager = saxon.SchemaManager;
        manager.ErrorList = new ArrayList();
        Uri schemaUri = new Uri(baseUri, "data/books.xsd");

        try {
            manager.Compile(schemaUri);
        } catch (Exception) {
            Console.WriteLine("Schema compilation failed with " + manager.ErrorList.Count + " errors");
            foreach (StaticError error in manager.ErrorList) {
                Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
            }
            return;
        }


        // Use this to validate an instance document

        SchemaValidator validator = manager.NewSchemaValidator();
        Uri instanceUri = new Uri(baseUri, "data/books.xml");
        validator.SetSource(instanceUri);
        validator.ErrorList = new ArrayList();
        XdmDestination psvi = new XdmDestination();
        validator.SetDestination(psvi);

        try {
            validator.Run();
        } catch (Exception) {
            Console.WriteLine("Instance validation failed with " + validator.ErrorList.Count + " errors");
            foreach (StaticError error in validator.ErrorList) {
                Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
            }
        }


        // Run a query on the result to check that it has type annotations

        XQueryCompiler xq = saxon.NewXQueryCompiler();
        XQueryEvaluator xv = xq.Compile("data((//PRICE)[1]) instance of xs:decimal").Load();
        xv.ContextItem = psvi.XdmNode;
        Console.WriteLine("Price is decimal? " + xv.EvaluateSingle().ToString());

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