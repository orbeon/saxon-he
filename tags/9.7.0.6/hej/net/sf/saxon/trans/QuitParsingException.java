package net.sf.saxon.trans;

import net.sf.saxon.trans.XPathException;

/**
 * An exception used to signal that no more input is required from the parser, and that
 * parsing can therefore be abandoned early (but without signaling any error to the user)
 */
public class QuitParsingException extends XPathException {

    public QuitParsingException() {
        super("No more input required", "SXQP0001");
    }
}

// Copyright (c) 2015 Saxonica Limited.   All rights reserved
