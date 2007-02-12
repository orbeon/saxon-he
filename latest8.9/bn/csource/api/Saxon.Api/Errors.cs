using System;
using System.Collections;
using javax.xml.transform;
using javax.xml.transform.stream;
using JStaticError = net.sf.saxon.trans.StaticError;
using JDynamicError = net.sf.saxon.trans.DynamicError;
using XPathException = net.sf.saxon.trans.XPathException;


namespace Saxon.Api {

    /// <summary>
    /// The StaticError class contains information about a static error detected during
    /// compilation of a stylesheet, query, or XPath expression.
    /// </summary>

    [Serializable]
    public class StaticError : Exception {

        private XPathException exception;
        internal bool isWarning;

        /// <summary>
        /// Create a new StaticError, wrapping a Saxon XPathException
        /// </summary>

        internal StaticError(TransformerException err) {
            if (err is XPathException) {
                this.exception = (XPathException)err;
            } else {
                this.exception = JStaticError.makeStaticError(err);
            }
        }

        /// <summary>
        /// The error code, as a QName. May be null if no error code has been assigned
        /// </summary>

        public QName ErrorCode {
            get {
                return new QName("err",
                        exception.getErrorCodeNamespace(),
                        exception.getErrorCodeLocalPart());
            }
        }

        /// <summary>
        /// Return the message associated with this error
        /// </summary>

        public override String Message {
            get {
                return exception.getMessage();
            }
        }

        /// <summary>
        /// The URI of the query or stylesheet module in which the error was detected
        /// (as a string)
        /// </summary>
        /// <remarks>
        /// May be null if the location of the error is unknown, or if the error is not
        /// localized to a specific module, or if the module in question has no known URI
        /// (for example, if it was supplied as an anonymous Stream)
        /// </remarks>

        public String ModuleUri {
            get {
                if (exception.getLocator() == null) {
                    return null;
                }
                return exception.getLocator().getSystemId();
            }
        }

        /// <summary>
        /// The line number locating the error within a query or stylesheet module
        /// </summary>
        /// <remarks>
        /// May be set to -1 if the location of the error is unknown
        /// </remarks>        

        public int LineNumber {
            get {
                SourceLocator loc = exception.getLocator();
                if (loc == null) {
                    if (exception.getException() is TransformerException) {
                        loc = ((TransformerException)exception.getException()).getLocator();
                        if (loc != null) {
                            return loc.getLineNumber();
                        }
                    }
                    return -1;
                }
                return loc.getLineNumber();
            }
        }

        /// <summary>
        /// Indicate whether this error is being reported as a warning condition. If so, applications
        /// may ignore the condition, though the results may not be as intended.
        /// </summary>

        public bool IsWarning {
            get {
                return isWarning;
            }
            set {
                isWarning = value;
            }
        }

        /// <summary>
        /// Indicate whether this condition is a type error.
        /// </summary>

        public bool IsTypeError {
            get {
                return exception.isTypeError();
            }
        }

        /// <summary>
        /// Return the error message.
        /// </summary>

        public override String ToString() {
            return exception.getMessage();
        }
    }

    /// <summary>
    /// The DynamicError class contains information about a dynamic error detected during
    /// execution of a stylesheet, query, or XPath expression.
    /// </summary>

    [Serializable]
    public class DynamicError : Exception {

        private XPathException exception;
        internal bool isWarning;

        /// <summary>
        /// Create a new DynamicError, specifying the error message
        /// </summary>

        public DynamicError(String message) {
            exception = new JDynamicError(message);
        }

        /// <summary>
        /// Create a new DynamicError, wrapping a Saxon XPathException
        /// </summary>

        internal DynamicError(TransformerException err) {
            if (err is XPathException) {
                this.exception = (XPathException)err;
            } else {
                this.exception = JDynamicError.makeDynamicError(err);
            }
        }

        /// <summary>
        /// The error code, as a QName. May be null if no error code has been assigned
        /// </summary>

        public QName ErrorCode {
            get {
                return new QName("err",
                        exception.getErrorCodeNamespace(),
                        exception.getErrorCodeLocalPart());
            }
        }

        /// <summary>
        /// Return the message associated with this error
        /// </summary>

        public override String Message {
            get {
                return exception.getMessage();
            }
        }

        /// <summary>
        /// The URI of the query or stylesheet module in which the error was detected
        /// (as a string)
        /// </summary>
        /// <remarks>
        /// May be null if the location of the error is unknown, or if the error is not
        /// localized to a specific module, or if the module in question has no known URI
        /// (for example, if it was supplied as an anonymous Stream)
        /// </remarks>

        public String ModuleUri {
            get {
                if (exception.getLocator() == null) {
                    return null;
                }
                return exception.getLocator().getSystemId();
            }
        }

        /// <summary>
        /// The line number locating the error within a query or stylesheet module
        /// </summary>
        /// <remarks>
        /// May be set to -1 if the location of the error is unknown
        /// </remarks>        

        public int LineNumber {
            get {
                SourceLocator loc = exception.getLocator();
                if (loc == null) {
                    if (exception.getException() is TransformerException) {
                        loc = ((TransformerException)exception.getException()).getLocator();
                        if (loc != null) {
                            return loc.getLineNumber();
                        }
                    }
                    return -1;
                }
                return loc.getLineNumber();
            }
        }

        /// <summary>
        /// Indicate whether this error is being reported as a warning condition. If so, applications
        /// may ignore the condition, though the results may not be as intended.
        /// </summary>

        public bool IsWarning {
            get {
                return isWarning;
            }
        }

        /// <summary>
        /// Indicate whether this condition is a type error.
        /// </summary>

        public bool IsTypeError {
            get {
                return exception.isTypeError();
            }
        }

        /// <summary>
        /// Return the error message.
        /// </summary>

        public override String ToString() {
            return exception.getMessage();
        }



    }

    [Serializable]
    internal class ErrorGatherer : javax.xml.transform.ErrorListener {

        private IList errorList;

        public ErrorGatherer(IList errorList) {
            this.errorList = errorList;
        }

        public void warning(TransformerException exception) {
            StaticError se = new StaticError(exception);
            se.isWarning = true;
            //Console.WriteLine("(Adding warning " + exception.getMessage() + ")");
            errorList.Add(se);
        }

        public void error(TransformerException error) {
            StaticError se = new StaticError(error);
            se.isWarning = false;
            //Console.WriteLine("(Adding error " + error.getMessage() + ")");
            errorList.Add(se);
        }

        public void fatalError(TransformerException error) {
            StaticError se = new StaticError(error);
            se.isWarning = false;
            errorList.Add(se);
            //Console.WriteLine("(Adding fatal error " + error.getMessage() + ")");
            throw error;
        }
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