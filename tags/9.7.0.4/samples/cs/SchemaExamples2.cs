using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;





/**
 * Some examples to show how the Saxon Schema API should be used. These examples
 * require use of Saxon-SA. In this example we make use of the InvalidityHandler to generate an XML formatted
 * report of the validation errors
 */
public class SchemaExamples2
{

    /**
     * Class is not instantiated, so give it a private constructor
     */
    private SchemaExamples2()
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

        validator.SetInvalidityHandler(new InvalidityReportGenerator(strm));
        XdmDestination psvi = new XdmDestination();
        validator.SetDestination(psvi);

        try {
            validator.Run();
        } catch (Exception) {
            Console.WriteLine("Instance validation failed with " + validator.ErrorList.Count + " errors");
            /*foreach (StaticError error in validator.ErrorList) {
                Console.WriteLine("At line " + error.LineNumber + ": " + error.Message);
            } */
        }


        // Run a query on the result to check that it has type annotations

        XQueryCompiler xq = saxon.NewXQueryCompiler();
        XQueryEvaluator xv = xq.Compile("data((//PRICE)[1]) instance of xs:decimal").Load();
        xv.ContextItem = psvi.XdmNode;
        Console.WriteLine("Price is decimal? " + xv.EvaluateSingle().ToString());

    }

}

	public class InvalidityReportGenerator : IInvalidityHandler {
		private String systemId;
		private XmlWriter writer;
		private int warningCount = 0;
		private int errorCount = 0;
		private XmlWriterSettings settings = new XmlWriterSettings();
		private String xsdVersion = "1.0";
		private bool started = false;
		private String schemaName = null;


		public InvalidityReportGenerator(StreamWriter sw){
            writer = XmlWriter.Create (sw, settings);

		}
		public InvalidityReportGenerator(StreamWriter sw, XmlWriterSettings settings){
            this.settings = settings;
            InvalidityReportGenerator(sw);

		}

		public void setOutStream(StreamWriter sw){
			writer = XmlWriter.Create (sw, settings);

		}

		public void startReporting (String systemId){
			this.systemId = systemId;
			started = true;

			writer.WriteStartDocument ();
			writer.WriteStartElement ("validation-report");
			if (systemId != null) {
				writer.WriteAttributeString ("system-id", systemId);
			}


		}

		public void setSchemaName(String name) {
			schemaName = name;
		}

		public String XsdVersion {

			set {
				this.xsdVersion = value;
			}
			get{
				return this.xsdVersion;
			}


		}

		public void reportInvalidity (StaticError failure){
			errorCount++;
			writer.WriteStartElement("error");
			if (failure.LineNumber != -1) {
				writer.WriteAttributeString("line", failure.LineNumber.ToString());
			}
			if (failure.ColumnNumber > -1) {
				writer.WriteAttributeString("column", failure.ColumnNumber.ToString());
			}
			if (failure.UnderlyingException is net.sf.saxon.type.ValidationException) {
				net.sf.saxon.type.ValidationException valException = (net.sf.saxon.type.ValidationException)failure.UnderlyingException;


				string pathStr = valException.getPath ();
				if (pathStr != null) {
					writer.WriteAttributeString ("path", pathStr);
				}
				if (valException.getSystemId() != null && !valException.getSystemId().Equals (systemId)) {
					writer.WriteAttributeString ("system-id", valException.getSystemId());
				}

				writer.WriteAttributeString ("xsd-part", valException.getConstraintSchemaPart().ToString());
				//            if (failure.getConstraintClauseNumber() != null) {
				//                writer.writeAttribute("clause", failure.getConstraintClauseNumber());
				//            }
				if (valException.getConstraintName () != null) {
					writer.WriteAttributeString ("constraint", valException.getConstraintReference ());
				}
				if (valException.getContextPath () != null) {
					writer.WriteAttributeString ("context-path", valException.getContextPath ().toString ());
				}
			}
			writer.WriteChars(failure.Message.ToCharArray(), 0, failure.Message.Length);
			writer.WriteEndElement();
		}


		public XdmValue endReporting(){
			createMetaData ();
			if (!started) {
				throw new StaticError(new TransformerException("reporting not started"));
			}
			writer.WriteEndElement ();
			writer.WriteEndDocument ();
			writer.Close ();
			return null;
		}

		public void createMetaData() {
				writer.WriteStartElement("meta-data");
				writer.WriteStartElement("validator");
				writer.WriteAttributeString("name", net.sf.saxon.Version.getProductName()); /* + "-" + getConfiguration().getEditionCode()); */
				writer.WriteAttributeString("version", net.sf.saxon.Version.getProductVersion());
				writer.WriteEndElement(); //</validator>
				writer.WriteStartElement("results");
				writer.WriteAttributeString("errors", "" + errorCount);
				writer.WriteAttributeString("warnings", "" + warningCount);
				writer.WriteEndElement(); //</results>
				writer.WriteStartElement("schema");
				if (schemaName != null) {
					writer.WriteAttributeString("file", schemaName);
				}
				writer.WriteAttributeString("xsd-version", xsdVersion);
				writer.WriteEndElement(); //</schema>
				writer.WriteStartElement("run");
				writer.WriteAttributeString("at", net.sf.saxon.value.DateTimeValue.getCurrentDateTime(null).getStringValue());
				writer.WriteEndElement(); //</run>
				writer.WriteEndElement(); //</meta-data>

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