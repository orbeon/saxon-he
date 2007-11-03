using System;
//using System.Collections.Generic;
using System.Text;

namespace SampleExtensions {

    /// <summary>
    /// This class contains some example methods that can be invoked from XSLT as
    /// extension functions or from XQuery as external functions. For examples of calls
    /// on these functions, see the XQueryExamples and XsltExamples programs respectively.
    /// </summary>
     
    public class SampleExtensions {

        /// <summary>
        /// Add two numbers
        /// </summary>

        public static double add(double one, double two) {
            return one + two;
        }

        /// <summary>
        /// Get the average of an array of numbers
        /// </summary>

        public static double average(double[] numbers) {
            double total = 0.0e0;
            foreach (double d in numbers) {
                total += d;
            }
            return total / numbers.Length;
        }

        /// <summary>
        /// Get the current host language from the Saxon context
        /// </summary>

        public static string hostLanguage(net.sf.saxon.expr.XPathContext context) {
            int lang = context.getController().getExecutable().getHostLanguage();
            if (lang == net.sf.saxon.Configuration.XQUERY) {
                return "XQuery";
            } else if (lang == net.sf.saxon.Configuration.XSLT) {
                return "XSLT";
            } else if (lang == net.sf.saxon.Configuration.XPATH) {
                return "XPath";
            } else {
                return "unknown";
            }
        }

    }
}
