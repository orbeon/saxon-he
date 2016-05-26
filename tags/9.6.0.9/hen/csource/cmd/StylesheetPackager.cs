using System;
using System.Text;
using com.saxonica;

namespace Saxon.cmd
{

    /// <summary>
    /// This class provides the command line interface for the .NET executable
    /// </summary>
    /// 
    class DotNetStylesheetPackager : com.saxonica.ptree.StylesheetPackager
    {

        /// <summary>
        /// Report incorrect usage of the command line.
        /// </summary>
       private static void badUsage(string message) {
            if (!"".Equals(message))
            {
                Console.WriteLine(message);
            }
            Console.WriteLine("Usage: see http://saxonica.com/documentation/html/!using-xsl/packaged-xslt.html");
            Console.WriteLine("StylesheetPackager [stylesheet file name] [zip file name]");
        
        }

        /// <summary>
        /// Private constructor, ensuring the class can only be used via its "main" method.
        /// </summary>
        /// 
        private DotNetStylesheetPackager()
        { 
        }



        /// <summary>
        /// Entry point for use from the .NET command line
        /// <param name="args">command line arguments</param>
        /// </summary>
        /// Throws java.lang.Exception
        static void Main(string[] args)
        {

            if (args.Length != 2)
            {
                DotNetStylesheetPackager.badUsage("");
            } else {
                    DotNetStylesheetPackager packager = new DotNetStylesheetPackager();
                    packager.archive(args[0], args[1]);
            }
        }
    }
}
