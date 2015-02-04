////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.cpp;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;

/**
 * * This class is to use with Saxon on C++
 */
public class SaxonCException {
    private String errorCode;
    private String errorMessage;
    private int linenumber = -1;
    private boolean isTypeError = false;
    private boolean isStaticError = false;
    private boolean isGlobalError = false;

    public SaxonCException() {
    }

    public SaxonCException(SaxonApiException ex) {
        QName code = ex.getErrorCode();
        errorCode = (code != null ? ex.getErrorCode().toString() : null);
        errorMessage = ex.getMessage();

        if (ex.getCause() instanceof XPathException) {
            XPathException cause = (XPathException) ex.getCause();
            isGlobalError = cause.isGlobalError();
            isStaticError = cause.isStaticError();
            isTypeError = cause.isTypeError();
            javax.xml.transform.SourceLocator locator =  cause.getLocator();
            if(locator != null) {
                linenumber = cause.getLocator().getLineNumber();
            }
        }
    }

    public SaxonCException(XPathException ex) {
        errorCode = ex.getErrorCodeLocalPart();
        errorMessage = ex.getMessage();

        isGlobalError = ex.isGlobalError();
        isStaticError = ex.isStaticError();
        isTypeError = ex.isTypeError();
        javax.xml.transform.SourceLocator locator = ex.getLocator();
        if(locator != null) {
            linenumber = locator.getLineNumber();
        }

    }

    public String getErrorCode(){
        return errorCode;
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public int getLinenumber(){
        return linenumber;
    }

    public boolean isGlobalError(){
        return isGlobalError;
    }

     public boolean isTypeError(){
        return isTypeError;
    }

    public boolean isStaticError(){
        return isStaticError;
    }
}
