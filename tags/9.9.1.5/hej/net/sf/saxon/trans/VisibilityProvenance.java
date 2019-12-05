package net.sf.saxon.trans;

/**
 * Indicates where the visibility property of a component came from
 */

public enum VisibilityProvenance {
    DEFAULTED,
    EXPLICIT,
    EXPOSED,
    ACCEPTED,
    DERIVED
}

