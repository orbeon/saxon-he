using System;
using System.Collections;
using javax.xml.transform;
using javax.xml.transform.stream;
using XPathException = net.sf.saxon.trans.XPathException;


namespace Saxon.Api
{

	/// <summary>
	/// The StaticError class contains information about a static error detected during
	/// compilation of a stylesheet, query, or XPath expression.
	/// </summary>

	[Serializable]
	public class StaticError : Exception
	{

		private XPathException exception;
		internal bool isWarning;

		/// <summary>
		/// Create a new StaticError, wrapping a Saxon XPathException
		/// </summary>

		internal StaticError(TransformerException err)
		{
			if (err is XPathException)
			{
				this.exception = (XPathException)err;
			}
			else
			{
				this.exception = XPathException.makeXPathException(err);
			}
		}

		/// <summary>
		/// The error code, as a QName. May be null if no error code has been assigned
		/// </summary>

		public QName ErrorCode
		{
			get
			{
				if (exception.getErrorCodeLocalPart () != null) {
					return new QName ("err",
						exception.getErrorCodeNamespace (),
						exception.getErrorCodeLocalPart ());
				} else {
					return null;
				}
			}
		}

		/// <summary>
		/// Return the message associated with this error
		/// </summary>

		public override String Message
		{
			get
			{
				return exception.getMessage();
			}
		}


		/// <summary>
		/// Return the message associated with this error concatenated with the message from the causing exception
		/// </summary> 
		public String InnerMessage
		{
			get {

				return exception.getMessage() +": " + exception.getCause().Message;
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

		public String ModuleUri
		{
			get
			{
				if (exception.getLocator() == null)
				{
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

		public int LineNumber
		{
			get
			{
				SourceLocator loc = exception.getLocator();
				if (loc == null)
				{
					if (exception.getException() is TransformerException)
					{
						loc = ((TransformerException)exception.getException()).getLocator();
						if (loc != null)
						{
							return loc.getLineNumber();
						}
					}
					return -1;
				}
				return loc.getLineNumber();
			}
		}


		/// <summary>
		/// The line number locating the error within a query or stylesheet module
		/// </summary>
		/// <remarks>
		/// May be set to -1 if the location of the error is unknown
		/// </remarks>        

		public int ColoumnNumber
		{
			get
			{
				SourceLocator loc = exception.getLocator();
				if (loc == null)
				{
					if (exception.getException() is TransformerException)
					{
						loc = ((TransformerException)exception.getException()).getLocator();
						if (loc != null)
						{
							return loc.getColumnNumber();
						}
					}
					return -1;
				}
				return loc.getColumnNumber();
			}
		}

		/// <summary>
		/// Indicate whether this error is being reported as a warning condition. If so, applications
		/// may ignore the condition, though the results may not be as intended.
		/// </summary>

		public bool IsWarning
		{
			get
			{
				return isWarning;
			}
			set
			{
				isWarning = value;
			}
		}

		/// <summary>
		/// Indicate whether this condition is a type error.
		/// </summary>

		public bool IsTypeError
		{
			get
			{
				return exception.isTypeError();
			}
		}

		/// <summary>
		/// Return the underlying exception. This is unstable as this is an internal object
		/// </summary>
		/// <returns>XPathException</returns>
		public XPathException UnderlyingException
		{
			get
			{
				return exception;
			}
		}

		/// <summary>
		/// Return the error message.
		/// </summary>

		public override String ToString()
		{
			return exception.getMessage();
		}
	}

	/// <summary>
	/// The DynamicError class contains information about a dynamic error detected during
	/// execution of a stylesheet, query, or XPath expression.
	/// </summary>

	[Serializable]
	public class DynamicError : Exception
	{

		private XPathException exception;
		internal bool isWarning;

		/// <summary>
		/// Create a new DynamicError, specifying the error message
		/// </summary>
		/// <param name="message">The error message</param>

		public DynamicError(String message)
		{
			exception = new XPathException(message);
		}

		/// <summary>
		/// Create a new DynamicError, wrapping a Saxon XPathException
		/// </summary>
		/// <param name="err">The exception to be wrapped</param>

		internal DynamicError(TransformerException err)
		{
			if (err is XPathException)
			{
				this.exception = (XPathException)err;
			}
			else
			{
				this.exception = XPathException.makeXPathException(err);
			}
		}

		/// <summary>
		/// The error code, as a QName. May be null if no error code has been assigned
		/// </summary>

		public QName ErrorCode
		{
			get
			{
				return new QName("err",
					exception.getErrorCodeNamespace(),
					exception.getErrorCodeLocalPart());
			}
		}

		/// <summary>
		/// Return the message associated with this error
		/// </summary>

		public override String Message
		{
			get
			{
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

		public String ModuleUri
		{
			get
			{
				if (exception.getLocator() == null)
				{
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

		public int LineNumber
		{
			get
			{
				SourceLocator loc = exception.getLocator();
				if (loc == null)
				{
					if (exception.getException() is TransformerException)
					{
						loc = ((TransformerException)exception.getException()).getLocator();
						if (loc != null)
						{
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

		public bool IsWarning
		{
			get
			{
				return isWarning;
			}
			set
			{
				isWarning = value;
			}
		}

		/// <summary>
		/// Indicate whether this condition is a type error.
		/// </summary>

		public bool IsTypeError
		{
			get
			{
				return exception.isTypeError();
			}
		}

		/// <summary>
		/// Return the error message.
		/// </summary>

		public override String ToString()
		{
			return exception.getMessage();
		}



	}

	/// <summary>
	/// Error gatherer. This class To provide customized error handling
	/// <p>If an application does <em>not</em> register its own custom
	/// <para><code>ErrorListener</code>, the default <code>ErrorGatherer</code>
	/// is used which keeps track of all warnings and errors in a list.
	/// and does not throw any <code>Exception</code>s.
	/// Applications are <em>strongly</em> encouraged to register and use
	/// <code>ErrorListener</code>s that insure proper behavior for warnings and
	/// errors.</para>
	/// </summary>
	[Serializable]
	internal class ErrorGatherer : javax.xml.transform.ErrorListener
	{

		private IList errorList;


		/// <summary>
		/// Initializes a new instance of the <see cref="Saxon.Api.ErrorGatherer"/> class.
		/// </summary>
		/// <param name="errorList">Error list.</param>
		public ErrorGatherer(IList errorList)
		{
			this.errorList = errorList;
		}

		/// <summary>
		/// Warning the specified exception.
		/// </summary>
		/// <param name="exception">TransformerException.</param>
		public void warning(TransformerException exception)
		{
			StaticError se = new StaticError(exception);
			se.isWarning = true;
			//Console.WriteLine("(Adding warning " + exception.getMessage() + ")");
			errorList.Add(se);
		}

		/// <summary>
		/// Report a Transformer exception thrown.
		/// </summary>
		/// <param name="error">Error.</param>
		public void error(TransformerException error)
		{
			StaticError se = new StaticError(error);
			se.isWarning = false;
			//Console.WriteLine("(Adding error " + error.getMessage() + ")");
			errorList.Add(se);
		}

		/// <summary>
		/// Report a fatal exception thrown.
		/// </summary>
		/// <param name="error">TransformerException.</param>
		public void fatalError(TransformerException error)
		{
			StaticError se = new StaticError(error);
			se.isWarning = false;
			errorList.Add(se);
			//Console.WriteLine("(Adding fatal error " + error.getMessage() + ")");
		}


		/// <summary>
		/// Gets the error list.
		/// </summary>
		/// <returns>Returns the error list</returns>
		public IList ErrorList{
			get { return errorList;}
		}
	}




}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////