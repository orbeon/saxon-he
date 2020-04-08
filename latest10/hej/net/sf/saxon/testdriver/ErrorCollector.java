////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.StandardErrorReporter;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XmlProcessingError;
import net.sf.saxon.trans.QuitParsingException;
import net.sf.saxon.trans.XmlProcessingException;

import javax.xml.transform.TransformerException;
import java.util.HashSet;
import java.util.Set;

public class ErrorCollector extends StandardErrorReporter {

    private Set<String> errorCodes = new HashSet<>();
    private boolean foundWarnings = false;
    private boolean madeEarlyExit = false;

    public XmlProcessingError lastError;

    /**
     * Receive notification of a warning.
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     * @see TransformerException
     */
    @Override
    protected void warning(XmlProcessingError exception) {
        foundWarnings = true;
        super.warning(exception);
        if (exception instanceof XmlProcessingException
                && ((XmlProcessingException)exception).getXPathException() instanceof QuitParsingException) {
            madeEarlyExit = true;
        }
    }

    @Override
    protected void error(XmlProcessingError exception) {
        lastError = exception;
        addErrorCode(exception);
        super.error(exception);
    }

    private void addErrorCode(XmlProcessingError exception) {
        String code;
        QName errorCode = exception.getErrorCode();
        if (errorCode != null) {
            String ns = errorCode.getNamespaceURI();
            if (ns == null || NamespaceConstant.ERR.equals(ns)) {
                code = errorCode.getLocalName();
            } else {
                code = errorCode.getEQName();
            }
            errorCodes.add(code);
        } else {
            errorCodes.add("error-with-no-error-code");
        }
    }

    public Set<String> getErrorCodes() {
        return errorCodes;
    }

    public boolean getFoundWarnings() {
        return foundWarnings;
    }

    public boolean isMadeEarlyExit() {
        return madeEarlyExit;
    }

}