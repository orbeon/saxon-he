package net.sf.saxon.om;

/**
 * The IdentityComparable class provides a way to compare class objects for equality
 *
 *
 */
public interface IdentityComparable {

    public boolean isIdentical(IdentityComparable other);
}
