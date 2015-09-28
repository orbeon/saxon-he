package net.sf.saxon.expr;

/**
 * The usage of an operand defines how the containing expression makes use of the value of the operand,
 * as defined in the XSLT 3.0 specification.
 */
public enum OperandUsage {

    ABSORPTION,
    INSPECTION,
    TRANSMISSION,
    NAVIGATION
}

// Copyright (c) 2014 Saxonica Limited. All rights reserved.


