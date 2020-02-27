#ifndef SAXON_API_EXCEPTION_H
#define SAXON_API_EXCEPTION_H

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2020 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

#include <iostream>
#include <exception>


/*! <code>SaxonApiException</code>. An exception thrown by the Saxon s9api API (Java). This is always a C++ wrapper for some other underlying exception in Java
 * <p/>
 */
class SaxonApiException: public std::exception {

public:

    /**
     * A default Constructor. Create a SaxonApiException
     */
     SaxonApiException();


         /**
          * A constructor. Create a SaxonApiException
          * @param message - The detail message string of this throwable.
          */
     explicit SaxonApiException(const char * message);

    /**
     * A Copy constructor. Create a SaxonApiException
     * @param ex - The exception object to copy
     */
	SaxonApiException(const SaxonApiException &ex);



    /** Returns a pointer to the (constant) error description.
        *  @return A pointer to a const char*. The underlying memory
        *          is in posession of the Exception object. Callers must
        *          not attempt to free the memory.
             */
    virtual const char * what ();

    /**
     * A constructor. Create a SaxonApiException
     * @param message - The detail message string of this throwable.
     * @param errorCode - The error code of the underlying exception thrown, if known
     * @param systemId - Get the URI of the module associated with the exception, if known.
     * @param lineNumber - The line number in the stylesheet related to cause of the exception
     */
	SaxonApiException(const char * message, const char * errorCode, const char * systemId, int linenumber);


    /**
     * A destructor.
     */
	virtual ~SaxonApiException() throw ();



    /**
     * Get the error code associated with the exception, if it exists
     * @return the associated error code, or null if no error code is available
     */
	const char * getErrorCode();


	int getLineNumber();


    /**
     * Returns the detail message string of the throwable, if there is one
     * @return the detail message string of this <tt>Throwable</tt> instance
     *         (which may be <tt>null</tt>).
     */
	const char * getMessage();

	/**
	* Get the URI of the module associated with the exception, if known.
	*/
	const char * getSystemId();




private:
	std::string message;
	int lineNumber;
	std::string errorCode;
	std::string systemId;
};

#endif /* SAXON_API_EXCEPTION_H */