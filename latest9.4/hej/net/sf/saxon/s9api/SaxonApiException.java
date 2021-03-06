package net.sf.saxon.s9api;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;

/**
 * An exception thrown by the Saxon s9api API. This is always a wrapper for some other underlying exception
 */
public class SaxonApiException extends Exception {

    /**
     * Create a SaxonApiException
     * @param cause the underlying cause of the exception
     */

    public SaxonApiException(Throwable cause) {
        super(cause);
    }

    /**
     * Create a SaxonApiException
     * @param message the message
     */

    public SaxonApiException(String message) {
        super(new XPathException(message));
    }

    /**
     * Create a SaxonApiException
     * @param message the message
     * @param cause the underlying cause of the exception
     */

    public SaxonApiException(String message, Throwable cause) {
        super(new XPathException(message, cause));
    }

    /**
     * Returns the detail message string of this throwable.
     *
     * @return the detail message string of this <tt>Throwable</tt> instance
     *         (which may be <tt>null</tt>).
     */
    public String getMessage() {
        return getCause().getMessage();
    }

    /**
     * Get the error code associated with the exception, if there is one
     * @return the associated error code, or null if no error code is available
     * @since 9.3
     */

    /*@Nullable*/ public QName getErrorCode() {
        Throwable cause = getCause();
        if (cause instanceof XPathException) {
            StructuredQName code = ((XPathException)cause).getErrorCodeQName();
            return (code==null ? null : new QName(code));
        } else {
            return null;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//